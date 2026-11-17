package dev.spironello.fichasrpg.rolagem;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.auth.UsuarioAtual;
import dev.spironello.fichasrpg.auth.UsuarioRepository;
import dev.spironello.fichasrpg.campanha.Participacao;
import dev.spironello.fichasrpg.campanha.ParticipacaoRepository;
import dev.spironello.fichasrpg.dto.Referencia;
import dev.spironello.fichasrpg.model.Ficha;
import tools.jackson.databind.json.JsonMapper;

/**
 * O único ponto que grava uma rolagem na mesa: toda rota que rola dados chama este registro (ADR 0026). Uma rolagem
 * nova no futuro precisa chamá-lo também, senão a mesa não a vê.
 *
 * Só grava se a ficha está numa campanha NO MOMENTO da rolagem (a ficha vem do banco na própria rota, então uma ficha
 * tirada da campanha depois de a tela abrir não grava). Quando a ficha é do mestre da campanha, o nome do personagem
 * não é gravado (fica nulo): assim ele não vaza por nenhum caminho, nem por uma consulta futura. Depois de gravar,
 * publica um evento; quem o leva ao tópico é o {@link PublicadorDeRolagens}, só depois do commit.
 */
@Component
public class RegistroDeRolagens {

	private final RolagemDaMesaRepository rolagens;
	private final ParticipacaoRepository participacoes;
	private final UsuarioRepository usuarios;
	private final UsuarioAtual usuarioAtual;
	private final JsonMapper json;
	private final ApplicationEventPublisher eventos;
	private final Clock relogio;

	public RegistroDeRolagens(RolagemDaMesaRepository rolagens, ParticipacaoRepository participacoes,
			UsuarioRepository usuarios, UsuarioAtual usuarioAtual, JsonMapper json, ApplicationEventPublisher eventos,
			Clock relogio) {
		this.rolagens = rolagens;
		this.participacoes = participacoes;
		this.usuarios = usuarios;
		this.usuarioAtual = usuarioAtual;
		this.json = json;
		this.eventos = eventos;
		this.relogio = relogio;
	}

	/** O {@code detalhe} é a resposta da rota, serializada como foi devolvida a quem rolou. Ficha sem campanha: não faz nada. */
	@Transactional
	public void registrar(Ficha ficha, TipoRolagem tipo, String rotulo, int total, Object detalhe) {
		Long campanhaId = ficha.getCampanhaId();
		if (campanhaId == null) {
			return;
		}
		boolean fichaDoMestre = participacoes.findByCampanhaIdAndUsuarioId(campanhaId, ficha.getDonoId())
				.map(Participacao::isMestre).orElse(false);
		Long autorId = usuarioAtual.id();
		String nomeDoAutor = usuarios.findById(autorId)
				.orElseThrow(() -> new IllegalStateException("Usuário logado inexistente: " + autorId)).getNome();
		// O banco guarda microssegundos: truncar aqui faz o evento e o GET do histórico trazerem o mesmo instante.
		Instant agora = relogio.instant().truncatedTo(ChronoUnit.MICROS);
		RolagemDaMesa gravada = rolagens.save(new RolagemDaMesa(campanhaId, ficha.getId(), autorId,
				fichaDoMestre ? null : ficha.getPersonagem(), tipo, rotulo, total, json.writeValueAsString(detalhe), agora));
		eventos.publishEvent(new RolagemRegistrada(RolagemDaMesaResponse.de(gravada, new Referencia(autorId, nomeDoAutor))));
	}
}
