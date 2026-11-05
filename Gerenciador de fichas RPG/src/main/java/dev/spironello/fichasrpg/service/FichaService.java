package dev.spironello.fichasrpg.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.dto.FichaRequest;
import dev.spironello.fichasrpg.dto.FichaResponse;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.exception.FichaNaoEncontradaException;
import dev.spironello.fichasrpg.exception.SistemaImutavelException;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.repository.FichaRepository;

/**
 * Regras de negócio, transações e logs das fichas de qualquer sistema. O controller só cuida de HTTP e o
 * repository só cuida do banco; a validação, a conversão e os recursos de cada sistema ficam atrás de
 * {@link SistemaDeFicha}, e este serviço só delega a ele. Os logs trazem apenas o id: nome e dados da ficha
 * são dados pessoais e não vão para o log.
 */
@Service
public class FichaService {

	private static final Logger log = LoggerFactory.getLogger(FichaService.class);

	private final FichaRepository repository;
	private final SistemasDeFicha sistemas;

	public FichaService(FichaRepository repository, SistemasDeFicha sistemas) {
		this.repository = repository;
		this.sistemas = sistemas;
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
		Ficha nova = sistemas.de(request.sistema()).criar(request);
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
		if (ficha.getSistema() != request.sistema()) {
			throw new SistemaImutavelException();
		}
		sistemas.de(request.sistema()).copiar(request, ficha);
		FichaResponse atualizada = resposta(repository.save(ficha));
		log.info("Ficha atualizada id={}", id);
		return atualizada;
	}

	/**
	 * Edição rápida (PATCH): muda só os valores atuais dos recursos enviados; o resto da ficha e os máximos
	 * não mudam. Cada sistema valida tudo antes de gravar: se um código ou valor for inválido, nada muda.
	 */
	@Transactional
	public FichaResponse atualizarRecursos(Long id, Map<String, Integer> atuais) {
		Ficha ficha = repository.findById(id).orElseThrow(() -> new FichaNaoEncontradaException(id));
		sistemas.de(ficha.getSistema()).atualizarRecursos(ficha, atuais);
		FichaResponse atualizada = resposta(repository.save(ficha));
		log.info("Recursos atualizados id={}", id);
		return atualizada;
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
		return sistemas.de(ficha.getSistema()).resposta(ficha);
	}

	private FichaResumoResponse resumo(Ficha ficha) {
		return sistemas.de(ficha.getSistema()).resumo(ficha);
	}
}
