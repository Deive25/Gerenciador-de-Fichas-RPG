package dev.spironello.fichasrpg;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * Deixa o MockMvc da base de testes já autenticado como o usuário de teste (jwt() do spring-security-test), para os
 * testes das fichas continuarem como eram, sem token em cada chamada. Um with(jwt()...) no próprio pedido prevalece
 * sobre este padrão (o DonoDaFichaTest usa isso para agir como o outro usuário).
 */
@TestConfiguration
public class AutenticacaoDeTesteConfig {

	/** Id fixo do usuário de teste, inserido pela IntegrationTestBase antes de cada teste. */
	public static final long USUARIO_DE_TESTE_ID = 1_000_000L;

	@Bean
	MockMvcBuilderCustomizer autenticadoComoUsuarioDeTeste() {
		return builder -> builder.defaultRequest(
				get("/").with(jwt().jwt(j -> j.subject(String.valueOf(USUARIO_DE_TESTE_ID)))));
	}
}
