package dev.spironello.fichasrpg.rolagem;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP sobre WebSocket puro (sem SockJS) em /ws. O broker é o simples, em memória, só em /topic e sem destinos de
 * aplicação: a mesa só recebe. Serve a uma instância da API; com mais de uma, troca-se por RabbitMQ ou Redis (ADR 0026).
 * As origens permitidas vêm da mesma propriedade do CORS.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private static final long HEARTBEAT_MS = 10_000;

	private final String[] origens;
	private final AutenticacaoStomp autenticacao;
	private final ThreadPoolTaskScheduler agendadorDoBroker;

	public WebSocketConfig(@Value("${app.cors.origens:http://localhost:4200}") String[] origens,
			AutenticacaoStomp autenticacao, @Qualifier("agendadorDoBroker") ThreadPoolTaskScheduler agendadorDoBroker) {
		this.origens = origens;
		this.autenticacao = autenticacao;
		this.agendadorDoBroker = agendadorDoBroker;
	}

	/** Só o heartbeat do broker usa este agendador (um fio basta). */
	@Bean
	static ThreadPoolTaskScheduler agendadorDoBroker() {
		ThreadPoolTaskScheduler agendador = new ThreadPoolTaskScheduler();
		agendador.setPoolSize(1);
		agendador.setThreadNamePrefix("broker-heartbeat-");
		agendador.setDaemon(true);
		return agendador;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registro) {
		registro.addEndpoint("/ws").setAllowedOrigins(origens);
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registro) {
		registro.enableSimpleBroker("/topic").setHeartbeatValue(new long[] { HEARTBEAT_MS, HEARTBEAT_MS })
				.setTaskScheduler(agendadorDoBroker);
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registro) {
		registro.interceptors(autenticacao);
	}
}
