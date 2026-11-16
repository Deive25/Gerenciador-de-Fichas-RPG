package dev.spironello.fichasrpg.campanha;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.auth.UsuarioAtual;
import dev.spironello.fichasrpg.exception.CampanhaNaoEncontradaException;
import dev.spironello.fichasrpg.exception.SemPermissaoException;

/**
 * A regra de acesso das campanhas (spec §4, ADR 0024), irmã do {@code AcessoAFicha}: para quem não participa a
 * campanha não existe (404); o jogador que tenta o que é do mestre recebe 403. Os serviços de campanha passam por aqui.
 */
@Component
public class AcessoACampanha {

	private final ParticipacaoRepository participacoes;
	private final UsuarioAtual usuario;

	public AcessoACampanha(ParticipacaoRepository participacoes, UsuarioAtual usuario) {
		this.participacoes = participacoes;
		this.usuario = usuario;
	}

	/** A participação de quem está logado; campanha inexistente e não membro dão o mesmo 404. */
	public Participacao exigirMembro(Long campanhaId) {
		return participacoes.findByCampanhaIdAndUsuarioId(campanhaId, usuario.id())
				.orElseThrow(() -> new CampanhaNaoEncontradaException(campanhaId));
	}

	/** Como {@link #exigirMembro}, mas o jogador recebe 403. */
	public Participacao exigirMestre(Long campanhaId) {
		Participacao minha = exigirMembro(campanhaId);
		if (!minha.isMestre()) {
			throw new SemPermissaoException();
		}
		return minha;
	}
}
