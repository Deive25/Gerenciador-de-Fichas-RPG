package dev.spironello.fichasrpg.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.dto.FichaOrdemRequest;
import dev.spironello.fichasrpg.dto.FichaRequest;
import dev.spironello.fichasrpg.dto.FichaResponse;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.exception.FichaNaoEncontradaException;
import dev.spironello.fichasrpg.exception.SistemaImutavelException;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.RecursoOrdem;
import dev.spironello.fichasrpg.repository.FichaRepository;

/**
 * Regras de negócio, transações e logs das fichas de qualquer sistema. O controller só cuida de HTTP e o
 * repository só cuida do banco; a conversão e a validação de cada sistema ficam no mapper e na classe de
 * validação dele. Os logs trazem apenas o id: nome e dados da ficha são dados pessoais e não vão para o log.
 */
@Service
public class FichaService {

	private static final Logger log = LoggerFactory.getLogger(FichaService.class);

	private final FichaRepository repository;
	private final FichaOrdemMapper ordemMapper;

	public FichaService(FichaRepository repository, FichaOrdemMapper ordemMapper) {
		this.repository = repository;
		this.ordemMapper = ordemMapper;
	}

	@Transactional(readOnly = true)
	public List<FichaResumoResponse> listar() {
		log.debug("Listando fichas");
		return repository.findAll().stream().map(this::resumo).toList();
	}

	@Transactional(readOnly = true)
	public FichaResponse buscar(Long id) {
		log.debug("Buscando ficha id={}", id);
		return repository.findById(id)
				.map(this::resposta)
				.orElseThrow(() -> new FichaNaoEncontradaException(id));
	}

	@Transactional
	public FichaResponse criar(FichaRequest request) {
		Ficha nova = switch (request) {
			case FichaOrdemRequest ordem -> {
				ValidacaoFichaOrdem.validar(ordem);
				yield ordemMapper.criar(ordem);
			}
		};
		FichaResponse criada = resposta(repository.save(nova));
		log.info("Ficha criada id={}", criada.id());
		return criada;
	}

	/**
	 * Substituição completa (ADR 0008): todos os campos do pedido são copiados, inclusive os nulos.
	 * O sistema da ficha não muda: pedido de outro sistema dá 409.
	 */
	@Transactional
	public FichaResponse atualizar(Long id, FichaRequest request) {
		Ficha ficha = repository.findById(id).orElseThrow(() -> new FichaNaoEncontradaException(id));
		switch (request) {
			case FichaOrdemRequest ordem -> {
				if (!(ficha instanceof FichaOrdem fichaOrdem)) {
					throw new SistemaImutavelException();
				}
				ValidacaoFichaOrdem.validar(ordem);
				ordemMapper.copiar(ordem, fichaOrdem);
			}
		}
		FichaResponse atualizada = resposta(repository.save(ficha));
		log.info("Ficha atualizada id={}", id);
		return atualizada;
	}

	/**
	 * Edição rápida (PATCH): muda só os valores atuais dos recursos enviados (PV, SAN, PE em Ordem); o resto da
	 * ficha e os máximos não mudam. Valida tudo antes de gravar: se um código ou valor for inválido, nada muda.
	 */
	@Transactional
	public FichaResponse atualizarRecursos(Long id, Map<String, Integer> atuais) {
		Ficha ficha = repository.findById(id).orElseThrow(() -> new FichaNaoEncontradaException(id));
		if (!(ficha instanceof FichaOrdem ordem)) {
			throw new IllegalStateException("Sistema sem recursos: " + ficha.getSistema());
		}
		Map<String, String> erros = new LinkedHashMap<>();
		Map<RecursoOrdem, Integer> validos = new LinkedHashMap<>();
		atuais.forEach((codigo, valor) -> {
			RecursoOrdem recurso = recursoOuNulo(codigo);
			if (recurso == null) {
				erros.put(codigo, "não existe em Ordem Paranormal");
			} else if (valor == null || valor < 0) {
				erros.put(codigo, "deve ser um número maior ou igual a 0");
			} else {
				validos.put(recurso, valor);
			}
		});
		if (!erros.isEmpty()) {
			throw new DadosInvalidosException(erros);
		}
		validos.forEach((recurso, valor) -> {
			switch (recurso) {
				case PV -> ordem.setPvAtual(valor);
				case SAN -> ordem.setSanAtual(valor);
				case PE -> ordem.setPeAtual(valor);
			}
		});
		FichaResponse atualizada = resposta(repository.save(ordem));
		log.info("Recursos atualizados id={}", id);
		return atualizada;
	}

	private static RecursoOrdem recursoOuNulo(String codigo) {
		try {
			return RecursoOrdem.valueOf(codigo);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	@Transactional
	public void excluir(Long id) {
		if (!repository.existsById(id)) {
			throw new FichaNaoEncontradaException(id);
		}
		repository.deleteById(id);
		log.info("Ficha excluida id={}", id);
	}

	private FichaResponse resposta(Ficha ficha) {
		if (ficha instanceof FichaOrdem ordem) {
			return ordemMapper.resposta(ordem);
		}
		throw new IllegalStateException("Sistema sem conversão: " + ficha.getSistema());
	}

	private FichaResumoResponse resumo(Ficha ficha) {
		if (ficha instanceof FichaOrdem ordem) {
			return ordemMapper.resumo(ordem);
		}
		throw new IllegalStateException("Sistema sem conversão: " + ficha.getSistema());
	}
}
