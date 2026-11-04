package dev.spironello.fichasrpg;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Relógio controlado nos testes de integração (importado pela IntegrationTestBase). Ele substitui
 * o Clock do sistema, como o Dado fixo substitui a sorte no teste de perícia (ADR 0011): o teste
 * escolhe o instante e o resultado é sempre o mesmo.
 */
@TestConfiguration
public class RelogioDeTesteConfig {

	@Bean
	@Primary
	RelogioAjustavel relogioAjustavel() {
		return new RelogioAjustavel(Instant.parse("2026-01-01T00:00:00Z"));
	}

	/** Clock parado num instante que o teste muda com definir/avancar. */
	public static class RelogioAjustavel extends Clock {

		private Instant agora;

		RelogioAjustavel(Instant inicio) {
			this.agora = inicio;
		}

		public synchronized void definir(Instant instante) {
			this.agora = instante;
		}

		public synchronized void avancar(Duration duracao) {
			this.agora = agora.plus(duracao);
		}

		@Override
		public synchronized Instant instant() {
			return agora;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zona) {
			return this;
		}
	}
}
