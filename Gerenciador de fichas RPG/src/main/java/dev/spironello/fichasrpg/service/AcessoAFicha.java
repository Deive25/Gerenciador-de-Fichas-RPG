package dev.spironello.fichasrpg.service;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.auth.UsuarioAtual;
import dev.spironello.fichasrpg.campanha.Participacao;
import dev.spironello.fichasrpg.campanha.ParticipacaoRepository;
import dev.spironello.fichasrpg.exception.FichaNaoEncontradaException;
import dev.spironello.fichasrpg.exception.SemPermissaoException;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.repository.FichaRepository;

/**
 * Único ponto que decide quem pode fazer o quê numa ficha (spec das campanhas, §4, ADR 0024). Os serviços das fichas e
 * dos recursos dela (itens, magias, retrato...) passam por aqui em vez de chamar o {@code FichaRepository}: um serviço
 * novo que buscasse a ficha direto no repository fugiria da regra.
 *
 * A decisão segue sempre esta ordem:
 * <ol>
 * <li>a ficha não existe: 404;</li>
 * <li>quem está logado é o dono: libera;</li>
 * <li>a ficha está sem campanha, ou quem está logado não participa dela: 404 (para quem está fora a ficha não existe);</li>
 * <li>é o mestre: libera (exceto as ações só do dono, que dão 403);</li>
 * <li>é jogador e o dono da ficha é o mestre: 404 (a ficha do mestre fica oculta para os jogadores);</li>
 * <li>é jogador e a ficha é de outro jogador: 403 (exceto o que o resumo mostra, como baixar o retrato).</li>
 * </ol>
 */
@Component
public class AcessoAFicha {

	/** O que a ação exige: o que muda é só quem passa no passo 4 (mestre) e no 6 (outro jogador). */
	private enum Exigencia {
		/** Ver completa, editar, rolar, listas e retrato (enviar e apagar): o dono e o mestre. */
		DONO_OU_MESTRE,
		/** Excluir e trocar de campanha: só o dono. */
		SO_DONO,
		/** O que o cartão do resumo mostra (baixar o retrato): o dono, o mestre e os outros jogadores. */
		VISIVEL
	}

	private final FichaRepository fichas;
	private final ParticipacaoRepository participacoes;
	private final UsuarioAtual usuario;

	public AcessoAFicha(FichaRepository fichas, ParticipacaoRepository participacoes, UsuarioAtual usuario) {
		this.fichas = fichas;
		this.participacoes = participacoes;
		this.usuario = usuario;
	}

	/** A ficha, para quem pode ler e editar (o dono e o mestre da campanha dela). */
	public Ficha buscar(Long fichaId) {
		return decidir(fichaId, Exigencia.DONO_OU_MESTRE);
	}

	/** Como {@link #buscar}, quando só importa a decisão. */
	public void exigir(Long fichaId) {
		decidir(fichaId, Exigencia.DONO_OU_MESTRE);
	}

	/** Excluir a ficha e levá-la a outra campanha: só o dono (o mestre e os outros jogadores recebem 403). */
	public void exigirDono(Long fichaId) {
		decidir(fichaId, Exigencia.SO_DONO);
	}

	/** Baixar o retrato, que o cartão do resumo mostra: libera também os outros jogadores (a ficha do mestre segue oculta). */
	public void exigirVisivel(Long fichaId) {
		decidir(fichaId, Exigencia.VISIVEL);
	}

	private Ficha decidir(Long fichaId, Exigencia exigencia) {
		Ficha ficha = fichas.findById(fichaId).orElseThrow(() -> new FichaNaoEncontradaException(fichaId));
		Long eu = usuario.id();
		if (eu.equals(ficha.getDonoId())) {
			return ficha;
		}
		if (ficha.getCampanhaId() == null) {
			throw new FichaNaoEncontradaException(fichaId);
		}
		Participacao minha = participacoes.findByCampanhaIdAndUsuarioId(ficha.getCampanhaId(), eu)
				.orElseThrow(() -> new FichaNaoEncontradaException(fichaId));
		if (minha.isMestre()) {
			if (exigencia == Exigencia.SO_DONO) {
				throw new SemPermissaoException();
			}
			return ficha;
		}
		boolean donoEhMestre = participacoes.findByCampanhaIdAndUsuarioId(ficha.getCampanhaId(), ficha.getDonoId())
				.map(Participacao::isMestre).orElse(false);
		if (donoEhMestre) {
			throw new FichaNaoEncontradaException(fichaId);
		}
		if (exigencia == Exigencia.VISIVEL) {
			return ficha;
		}
		throw new SemPermissaoException();
	}
}
