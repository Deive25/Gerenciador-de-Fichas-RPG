package dev.spironello.fichasrpg.rolagem;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.campanha.ParticipacaoId;
import dev.spironello.fichasrpg.campanha.ParticipacaoRepository;

/**
 * Segurança do STOMP, no canal de entrada. O handshake do WebSocket é público (o navegador não manda cabeçalho nele),
 * então a autenticação acontece no CONNECT, com o mesmo {@link JwtDecoder} da API. Depois, só entra no tópico de uma
 * campanha quem participa dela (o {@code UsuarioAtual} lê o contexto de segurança do HTTP, que não existe aqui, por
 * isso a consulta é direta na participação). O cliente nunca publica: o SEND é sempre recusado.
 *
 * Limite conhecido (ADR 0026): o token vale só no CONNECT. Quem é removido da campanha continua recebendo até a
 * conexão cair; o histórico, por outro lado, exige ser membro a cada GET.
 */
@Component
public class AutenticacaoStomp implements ChannelInterceptor {

	private static final String PREFIXO_BEARER = "Bearer ";
	private static final Pattern TOPICO_DE_ROLAGENS = Pattern.compile("^/topic/campanhas/(\\d+)/rolagens$");

	private final JwtDecoder decoder;
	private final ParticipacaoRepository participacoes;

	public AutenticacaoStomp(JwtDecoder decoder, ParticipacaoRepository participacoes) {
		this.decoder = decoder;
		this.participacoes = participacoes;
	}

	@Override
	public Message<?> preSend(Message<?> mensagem, MessageChannel canal) {
		StompHeaderAccessor cabecalhos = MessageHeaderAccessor.getAccessor(mensagem, StompHeaderAccessor.class);
		StompCommand comando = cabecalhos == null ? null : cabecalhos.getCommand();
		if (comando == StompCommand.CONNECT) {
			autenticar(cabecalhos);
		} else if (comando == StompCommand.SUBSCRIBE) {
			autorizarInscricao(cabecalhos);
		} else if (comando == StompCommand.SEND) {
			throw new MessagingException("Envio não permitido: a mesa só recebe");
		}
		return mensagem;
	}

	private void autenticar(StompHeaderAccessor cabecalhos) {
		String autorizacao = cabecalhos.getFirstNativeHeader("Authorization");
		if (autorizacao == null || !autorizacao.startsWith(PREFIXO_BEARER)) {
			throw new MessagingException("Não autenticado");
		}
		try {
			Jwt jwt = decoder.decode(autorizacao.substring(PREFIXO_BEARER.length()).trim());
			cabecalhos.setUser(new JwtAuthenticationToken(jwt));
		} catch (JwtException e) {
			// Sem a causa na mensagem: o texto do token não vai para o log nem para o cliente.
			throw new MessagingException("Não autenticado");
		}
	}

	private void autorizarInscricao(StompHeaderAccessor cabecalhos) {
		Principal usuario = cabecalhos.getUser();
		String destino = cabecalhos.getDestination();
		Matcher casou = destino == null ? null : TOPICO_DE_ROLAGENS.matcher(destino);
		if (!(usuario instanceof JwtAuthenticationToken jwt) || casou == null || !casou.matches()) {
			throw new MessagingException("Inscrição recusada");
		}
		Long usuarioId = Long.valueOf(jwt.getToken().getSubject());
		Long campanhaId;
		try {
			campanhaId = Long.valueOf(casou.group(1));
		} catch (NumberFormatException e) {
			throw new MessagingException("Inscrição recusada");
		}
		if (!participacoes.existsById(new ParticipacaoId(campanhaId, usuarioId))) {
			throw new MessagingException("Inscrição recusada");
		}
	}
}
