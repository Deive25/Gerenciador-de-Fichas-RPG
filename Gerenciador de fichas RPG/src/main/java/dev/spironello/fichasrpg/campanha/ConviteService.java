package dev.spironello.fichasrpg.campanha;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.auth.UsuarioAtual;
import dev.spironello.fichasrpg.auth.UsuarioRepository;
import dev.spironello.fichasrpg.exception.ConviteInvalidoException;

/**
 * Convites por link: o código tem 12 caracteres, não expira, e o mestre renova (o antigo morre na hora) ou desativa.
 * Código inexistente, trocado ou desativado dá o mesmo 404 em qualquer rota, para não revelar o que existe. Os logs
 * nunca trazem o código (ele é a "senha" de entrada).
 */
@Service
public class ConviteService {

	private static final Logger log = LoggerFactory.getLogger(ConviteService.class);
	private static final int TENTATIVAS = 3;

	private final CampanhaRepository campanhas;
	private final ParticipacaoRepository participacoes;
	private final UsuarioRepository usuarios;
	private final AcessoACampanha acesso;
	private final UsuarioAtual usuario;
	private final Clock relogio;

	public ConviteService(CampanhaRepository campanhas, ParticipacaoRepository participacoes, UsuarioRepository usuarios,
			AcessoACampanha acesso, UsuarioAtual usuario, Clock relogio) {
		this.campanhas = campanhas;
		this.participacoes = participacoes;
		this.usuarios = usuarios;
		this.acesso = acesso;
		this.usuario = usuario;
		this.relogio = relogio;
	}

	/**
	 * Um código que ainda não existe, com até 3 tentativas. A conferência vem antes de gravar de propósito: uma colisão
	 * do {@code unique} só apareceria no flush e deixaria a transação inteira inutilizável. Com 71 bits, a segunda
	 * tentativa nunca acontece na prática; o {@code unique} fica como rede de segurança.
	 */
	public String novoCodigoLivre() {
		for (int tentativa = 1; tentativa <= TENTATIVAS; tentativa++) {
			String codigo = CodigoDeConvite.gerar();
			if (!campanhas.existsByCodigoConvite(codigo)) {
				return codigo;
			}
		}
		throw new IllegalStateException("Não foi possível gerar um código de convite novo");
	}

	/** Gera um código novo e ativa o convite; o antigo deixa de valer na hora. Só o mestre. */
	@Transactional
	public ConviteResponse renovar(Long campanhaId) {
		acesso.exigirMestre(campanhaId);
		Campanha campanha = campanhas.findById(campanhaId).orElseThrow();
		campanha.setCodigoConvite(novoCodigoLivre());
		campanha.setConviteAtivo(true);
		campanhas.flush();
		log.info("Convite renovado campanhaId={}", campanhaId);
		return new ConviteResponse(campanha.getCodigoConvite(), true);
	}

	/** Desativa o convite (o código fica guardado, e renovar o reativa com um código novo). Só o mestre. */
	@Transactional
	public void desativar(Long campanhaId) {
		acesso.exigirMestre(campanhaId);
		campanhas.findById(campanhaId).orElseThrow().setConviteAtivo(false);
		log.info("Convite desativado campanhaId={}", campanhaId);
	}

	/** A prévia pública: só os nomes da campanha e do mestre. */
	@Transactional(readOnly = true)
	public PreviaConviteResponse previa(String codigo) {
		Campanha campanha = ativaPeloCodigo(codigo);
		String mestre = participacoes.findByCampanhaIdAndPapel(campanha.getId(), Papel.MESTRE)
				.flatMap(p -> usuarios.findById(p.getUsuarioId())).map(u -> u.getNome()).orElse("");
		return new PreviaConviteResponse(campanha.getId(), campanha.getNome(), mestre, participacoes.mestreEhDemo(campanha.getId(), Papel.MESTRE));
	}

	/**
	 * Entra na campanha como jogador. O código vale antes de tudo (o antigo dá 404 até para quem já é membro); depois,
	 * quem já participa (inclusive o mestre) não muda de papel e o resultado diz que não é novo.
	 */
	@Transactional
	public ResultadoDoAceite aceitar(String codigo) {
		Campanha campanha = ativaPeloCodigo(codigo);
		// Conta demo só entra em campanha de demonstração: para ela, o convite de uma campanha real não existe (404).
		if (contaAtualEhDemo() && !participacoes.mestreEhDemo(campanha.getId(), Papel.MESTRE)) {
			throw new ConviteInvalidoException();
		}
		boolean novo = participacoes.entrarComoJogador(campanha.getId(), usuario.id(), relogio.instant()) > 0;
		log.info("Convite aceito campanhaId={} novo={}", campanha.getId(), novo);
		return new ResultadoDoAceite(campanha.getId(), novo);
	}

	private boolean contaAtualEhDemo() {
		return usuarios.findById(usuario.id()).map(u -> u.isDemo()).orElse(false);
	}

	private Campanha ativaPeloCodigo(String codigo) {
		return campanhas.findByCodigoConvite(codigo).filter(Campanha::isConviteAtivo).orElseThrow(ConviteInvalidoException::new);
	}
}
