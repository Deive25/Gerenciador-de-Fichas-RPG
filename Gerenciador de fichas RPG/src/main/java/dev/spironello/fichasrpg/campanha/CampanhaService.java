package dev.spironello.fichasrpg.campanha;

import java.time.Clock;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.auth.Usuario;
import dev.spironello.fichasrpg.auth.UsuarioAtual;
import dev.spironello.fichasrpg.auth.UsuarioRepository;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.dto.Referencia;
import dev.spironello.fichasrpg.exception.MembroNaoEncontradoException;
import dev.spironello.fichasrpg.exception.MestreNaoSaiException;
import dev.spironello.fichasrpg.exception.SemPermissaoException;
import dev.spironello.fichasrpg.service.FichaService;

/**
 * Regras das campanhas: criar, ler, editar, excluir, membros e as fichas da mesa. Quem pode o quê é decidido pelo
 * {@link AcessoACampanha}. Sair, remover e excluir apagam a participação (ou a campanha) sem carregar as fichas: quem
 * devolve as fichas a "sem campanha" é a chave estrangeira composta do banco (V11), e carregá-las aqui só convidaria
 * a regravar o {@code campanha_id} antigo. Os logs trazem só ids.
 */
@Service
public class CampanhaService {

	private static final Logger log = LoggerFactory.getLogger(CampanhaService.class);

	private final CampanhaRepository campanhas;
	private final ParticipacaoRepository participacoes;
	private final UsuarioRepository usuarios;
	private final AcessoACampanha acesso;
	private final ConviteService convites;
	private final FichaService fichas;
	private final UsuarioAtual usuario;
	private final Clock relogio;

	public CampanhaService(CampanhaRepository campanhas, ParticipacaoRepository participacoes, UsuarioRepository usuarios,
			AcessoACampanha acesso, ConviteService convites, FichaService fichas, UsuarioAtual usuario, Clock relogio) {
		this.campanhas = campanhas;
		this.participacoes = participacoes;
		this.usuarios = usuarios;
		this.acesso = acesso;
		this.convites = convites;
		this.fichas = fichas;
		this.usuario = usuario;
		this.relogio = relogio;
	}

	/** As campanhas de que quem pede participa, com o mestre e a contagem das fichas que ele vê. */
	@Transactional(readOnly = true)
	public List<CampanhaResumoResponse> listar() {
		List<Participacao> minhas = participacoes.findAllByUsuarioId(usuario.id());
		if (minhas.isEmpty()) {
			return List.of();
		}
		List<Long> ids = minhas.stream().map(Participacao::getCampanhaId).toList();
		Map<Long, Campanha> porId = campanhas.findAllById(ids).stream()
				.collect(Collectors.toMap(Campanha::getId, Function.identity()));
		Map<Long, Referencia> mestres = mestresDe(ids);
		Map<Long, Integer> contagem = fichas.contarVisiveis(minhas);
		return minhas.stream()
				.sorted(Comparator.comparing(Participacao::getCampanhaId))
				.map(p -> new CampanhaResumoResponse(p.getCampanhaId(), porId.get(p.getCampanhaId()).getNome(), p.getPapel(),
						mestres.get(p.getCampanhaId()), contagem.get(p.getCampanhaId())))
				.toList();
	}

	/** Cria a campanha com o convite ativo; quem cria vira o mestre, na mesma transação. */
	@Transactional
	public CampanhaResponse criar(CampanhaRequest pedido) {
		Campanha campanha = campanhas
				.save(new Campanha(pedido.nome(), pedido.descricao(), convites.novoCodigoLivre(), relogio.instant()));
		Participacao minha = participacoes.save(new Participacao(campanha.getId(), usuario.id(), Papel.MESTRE, relogio.instant()));
		log.info("Campanha criada id={}", campanha.getId());
		return resposta(campanha, minha);
	}

	@Transactional(readOnly = true)
	public CampanhaResponse buscar(Long id) {
		Participacao minha = acesso.exigirMembro(id);
		return resposta(campanhas.findById(id).orElseThrow(), minha);
	}

	/** Substituição completa de nome e descrição (como a ficha, ADR 0008). Só o mestre. */
	@Transactional
	public CampanhaResponse atualizar(Long id, CampanhaRequest pedido) {
		Participacao minha = acesso.exigirMestre(id);
		Campanha campanha = campanhas.findById(id).orElseThrow();
		campanha.setNome(pedido.nome());
		campanha.setDescricao(pedido.descricao());
		log.info("Campanha atualizada id={}", id);
		return resposta(campanha, minha);
	}

	/** Exclui a campanha: as participações vão junto (cascade) e as fichas voltam aos donos, sem campanha (chave estrangeira). */
	@Transactional
	public void excluir(Long id) {
		acesso.exigirMestre(id);
		campanhas.deleteById(id);
		campanhas.flush();
		log.info("Campanha excluida id={}", id);
	}

	/** Os membros, o mestre primeiro e depois por ordem de entrada. Sem e-mail. */
	@Transactional(readOnly = true)
	public List<MembroResponse> membros(Long id) {
		acesso.exigirMembro(id);
		List<Participacao> membros = participacoes.findAllByCampanhaId(id).stream()
				.sorted(Comparator.comparing((Participacao p) -> !p.isMestre()).thenComparing(Participacao::getEntrouEm))
				.toList();
		Map<Long, String> nomes = nomes(membros.stream().map(Participacao::getUsuarioId).toList());
		return membros.stream()
				.map(p -> new MembroResponse(p.getUsuarioId(), nomes.get(p.getUsuarioId()), p.getPapel(), p.getEntrouEm()))
				.toList();
	}

	/**
	 * Remove um membro: o mestre remove um jogador, e o jogador sai (remove a si mesmo). O mestre não sai (409) e o
	 * jogador não remove os outros (403, inclusive se o alvo for o mestre). Alvo que não participa: 404.
	 */
	@Transactional
	public void removerMembro(Long id, Long usuarioId) {
		Participacao minha = acesso.exigirMembro(id);
		boolean sair = minha.getUsuarioId().equals(usuarioId);
		if (sair && minha.isMestre()) {
			throw new MestreNaoSaiException();
		}
		if (!sair && !minha.isMestre()) {
			throw new SemPermissaoException();
		}
		Participacao alvo = participacoes.findByCampanhaIdAndUsuarioId(id, usuarioId)
				.orElseThrow(() -> new MembroNaoEncontradoException(usuarioId));
		participacoes.delete(alvo);
		participacoes.flush();
		log.info("Membro saiu da campanha id={} usuarioId={}", id, usuarioId);
	}

	/** As fichas que quem pede vê na mesa: o mestre vê todas, o jogador não vê as do mestre. */
	@Transactional(readOnly = true)
	public List<FichaResumoResponse> fichas(Long id) {
		return fichas.listarDaCampanha(acesso.exigirMembro(id));
	}

	private CampanhaResponse resposta(Campanha campanha, Participacao minha) {
		Referencia mestre = mestresDe(List.of(campanha.getId())).get(campanha.getId());
		ConviteResponse convite = minha.isMestre()
				? new ConviteResponse(campanha.getCodigoConvite(), campanha.isConviteAtivo())
				: null;
		return new CampanhaResponse(campanha.getId(), campanha.getNome(), campanha.getDescricao(), minha.getPapel(), mestre,
				convite);
	}

	/** O mestre de cada campanha (id e nome), por id da campanha. */
	private Map<Long, Referencia> mestresDe(Collection<Long> campanhaIds) {
		List<Participacao> mestres = participacoes.findAllByCampanhaIdInAndPapel(campanhaIds, Papel.MESTRE);
		Map<Long, String> nomes = nomes(mestres.stream().map(Participacao::getUsuarioId).toList());
		return mestres.stream().collect(Collectors.toMap(Participacao::getCampanhaId,
				p -> new Referencia(p.getUsuarioId(), nomes.get(p.getUsuarioId()))));
	}

	private Map<Long, String> nomes(Collection<Long> usuarioIds) {
		return usuarios.findAllById(usuarioIds).stream().collect(Collectors.toMap(Usuario::getId, Usuario::getNome));
	}
}
