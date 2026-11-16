package dev.spironello.fichasrpg.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A API não sobe sem JWT_SEGREDO de pelo menos 32 bytes: a chave é conferida ao criar o bean. Sem Spring e sem Docker. */
@DisplayName("Segredo do JWT (unitário)")
class SegredoJwtTest {

	private static final String MENSAGEM = "Defina JWT_SEGREDO (≥ 32 bytes) no .env; gere com: openssl rand -base64 48";

	@Test
	@DisplayName("segredo vazio, ausente ou com 31 bytes é recusado com a mensagem que ensina a gerar uma chave")
	void segredoCurtoOuAusente() {
		for (String segredo : new String[] { null, "", "x".repeat(31) }) {
			assertThatThrownBy(() -> JwtConfig.chave(segredo))
					.isInstanceOf(IllegalStateException.class)
					.hasMessage(MENSAGEM);
		}
	}

	@Test
	@DisplayName("os bytes contam em UTF-8: 16 'ç' são 32 bytes e passam")
	void contaBytesEmUtf8() {
		assertThat(JwtConfig.chave("ç".repeat(16)).getEncoded()).hasSize(32);
		assertThat(JwtConfig.chave("x".repeat(32)).getAlgorithm()).isEqualTo("HmacSHA256");
	}
}
