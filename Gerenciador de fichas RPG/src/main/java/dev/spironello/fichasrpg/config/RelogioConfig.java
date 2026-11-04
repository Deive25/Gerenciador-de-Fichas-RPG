package dev.spironello.fichasrpg.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio da aplicação. Injetado (em vez de Instant.now()) pelo mesmo motivo do Dado (ADR 0011):
 * o teste escolhe o instante e a versão do retrato fica previsível.
 */
@Configuration
public class RelogioConfig {

	@Bean
	Clock relogio() {
		return Clock.systemUTC();
	}
}
