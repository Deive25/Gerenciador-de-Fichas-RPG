package dev.spironello.fichasrpg.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.auth.UsuarioAtual;
import dev.spironello.fichasrpg.campanha.AcessoACampanha;
import dev.spironello.fichasrpg.campanha.Papel;
import dev.spironello.fichasrpg.campanha.Participacao;
import dev.spironello.fichasrpg.dto.FichaRequest;
import dev.spironello.fichasrpg.dto.FichaResponse;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.exception.FichaDesatualizadaException;
import dev.spironello.fichasrpg.exception.SistemaImutavelException;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.repository.FichaRepository;
import jakarta.persistence.EntityManager;

/**
 * Regras de negócio, transações e logs das fichas de qualquer sistema. O controller só cuida de HTTP e o
 * repository só cuida do banco; a validação, a conversão e os recursos de cada sistema ficam atrás de
 * {@link SistemaDeFicha}, e este serviço só delega a ele. Quem pode fazer o quê com uma ficha é decidido pelo
 * {@link AcessoAFicha}. Os logs trazem apenas o id: nome e dados da ficha são dados pessoais e não vão para o log.
 */
@Service
public class FichaService {

	private static final Logger log = LoggerFactory.getLogger(FichaService.class);

	private final FichaRepository repository;
	private final SistemasDeFicha sistemas;
	private final AcessoAFicha fichas;
	private final UsuarioAtual usuario;
	private final AcessoACampanha campanhas;
	private final EntityManager entityManager;

	public FichaService(FichaRepository repository, SistemasDeFicha sistemas, AcessoAFicha fichas, UsuarioAtual usuario,
			AcessoACampanha campanhas, EntityManager entityManager) {
		this.repository = repository;
		this.sistemas = sistemas;
		this.fichas = fichas;
		this.usuario = usuario;
		this.campanhas = campanhas;
		this.entityManager = entityManager;
	}

	@Transactional(readOnly = true)
	public List<FichaResumoResponse> listar() {
		log.debug("Listando fichas");
		return repository.findAllByDonoId(usuario.id()).stream().map(this::resumo).toList();
	}

	@Transactional(readOnly = true)
	public FichaResponse buscar(Long id) {
		log.debug("Buscando ficha id={}", id);
		return resposta(fichas.buscar(id));
	}

	@Transactional
	public FichaResponse criar(FichaRequest request) {
		return criar(request, null);
	}

	/** Cria a ficha dentro da campanha (spec §5); quem cria tem de participar dela, senão 404. */
	@Transactional
	public FichaResponse criarNaCampanha(Long campanhaId, FichaRequest request) {
		campanhas.exigirMembro(campanhaId);
		return criar(request, campanhaId);
	}

	private FichaResponse criar(FichaRequest request, Long campanhaId) {
		Ficha nova = sistemas.de(request.sistema()).criar(request);
		nova.setDonoId(usuario.id());
		nova.setCampanhaId(campanhaId);
		FichaResponse criada = resposta(gravar(nova));
		log.info("Ficha criada id={}", criada.id());
		return criada;
	}

	/**
	 * Substituição completa (ADR 0008): todos os campos do pedido são copiados, inclusive os nulos.
	 * O sistema da ficha não muda: pedido de outro sistema dá 409. A versão do pedido é obrigatória (400) e tem de ser a
	 * do banco, senão alguém gravou antes e o resultado é 409 (ADR 0025).
	 */
	@Transactional
	public FichaResponse atualizar(Long id, FichaRequest request) {
		Ficha ficha = fichas.buscar(id);
		if (ficha.getSistema() != request.sistema()) {
			throw new SistemaImutavelException();
		}
		if (request.versao() == null) {
			throw new DadosInvalidosException(Map.of("versao", "é obrigatória"));
		}
		if (!request.versao().equals(ficha.getVersao())) {
			throw new FichaDesatualizadaException();
		}
		sistemas.de(request.sistema()).copiar(request, ficha);
		FichaResponse atualizada = resposta(gravar(ficha));
		log.info("Ficha atualizada id={}", id);
		return atualizada;
	}

	/**
	 * Edição rápida (PATCH): muda só os valores atuais dos recursos enviados; o resto da ficha e os máximos
	 * não mudam. Cada sistema valida tudo antes de gravar: se um código ou valor for inválido, nada muda.
	 */
	@Transactional
	public FichaResponse atualizarRecursos(Long id, Map<String, Integer> atuais) {
		Ficha ficha = fichas.buscar(id);
		sistemas.de(ficha.getSistema()).atualizarRecursos(ficha, atuais);
		FichaResponse atualizada = resposta(gravar(ficha));
		log.info("Recursos atualizados id={}", id);
		return atualizada;
	}

	/** Leva a ficha a uma campanha de que o dono participa (ou a tira de uma, com null). Só o dono (spec §5). */
	@Transactional
	public FichaResponse trocarCampanha(Long id, Long campanhaId) {
		fichas.exigirDono(id);
		if (campanhaId != null) {
			campanhas.exigirMembro(campanhaId);
		}
		Ficha ficha = fichas.buscar(id);
		ficha.setCampanhaId(campanhaId);
		FichaResponse trocada = resposta(gravar(ficha));
		log.info("Campanha da ficha trocada id={}", id);
		return trocada;
	}

	/**
	 * As fichas que quem pede vê na mesa (spec §4): o mestre vê todas, inclusive as dele; o jogador, as dos jogadores. Recebe
	 * a participação já conferida (o {@code CampanhaService} chama o {@code AcessoACampanha}) e faz uma consulta só.
	 */
	@Transactional(readOnly = true)
	public List<FichaResumoResponse> listarDaCampanha(Participacao minha) {
		List<Ficha> visiveis = minha.isMestre()
				? repository.findAllByCampanhaIdOrderById(minha.getCampanhaId())
				: repository.findDosJogadoresDaCampanha(minha.getCampanhaId(), Papel.MESTRE);
		return visiveis.stream().map(this::resumo).toList();
	}

	/** Quantas fichas cada participação enxerga na campanha dela: a contagem de GET /campanhas. */
	@Transactional(readOnly = true)
	public Map<Long, Integer> contarVisiveis(Collection<Participacao> minhas) {
		Map<Long, Integer> contagem = new HashMap<>();
		minhas.forEach(p -> contagem.put(p.getCampanhaId(), 0));
		if (minhas.isEmpty()) {
			return contagem;
		}
		Map<Long, Participacao> porCampanha = minhas.stream().collect(Collectors.toMap(Participacao::getCampanhaId, p -> p));
		for (Object[] linha : repository.contarPorCampanha(porCampanha.keySet(), Papel.MESTRE)) {
			Long campanhaId = (Long) linha[0];
			long total = (Long) linha[1];
			long doMestre = (Long) linha[2];
			contagem.put(campanhaId, (int) (porCampanha.get(campanhaId).isMestre() ? total : total - doMestre));
		}
		return contagem;
	}

	/** Só o dono exclui (spec §4): o mestre vê e edita as fichas da mesa, mas não as exclui. */
	@Transactional
	public void excluir(Long id) {
		fichas.exigirDono(id);
		repository.deleteById(id);
		log.info("Ficha excluida id={}", id);
	}

	/**
	 * Grava já (flush) e relê a ficha: o flush sobe a versão (@Version) antes da resposta, e o refresh traz de volta os
	 * campos de leitura (o nome do dono e da campanha, que são subconsultas e não mudam sozinhos na entidade).
	 */
	private Ficha gravar(Ficha ficha) {
		Ficha salva = repository.save(ficha);
		entityManager.flush();
		entityManager.refresh(salva);
		return salva;
	}

	private FichaResponse resposta(Ficha ficha) {
		return sistemas.de(ficha.getSistema()).resposta(ficha);
	}

	private FichaResumoResponse resumo(Ficha ficha) {
		return sistemas.de(ficha.getSistema()).resumo(ficha);
	}
}
