package dev.spironello.fichasrpg.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import dev.spironello.fichasrpg.IntegrationTestBase;

/**
 * As origens do CORS vêm de {@code app.cors.origens}, uma lista separada por vírgula: em produção o Render a define pela
 * variável APP_CORS_ORIGENS (o relaxed binding troca ponto por sublinhado e põe em maiúsculas; a conferência com a variável
 * de ambiente de verdade é no docker run). Aqui: duas origens na lista valem, e a que não está nela (a de dev) é recusada.
 */
@TestPropertySource(properties = "app.cors.origens=https://fichas-rpg.pages.dev,https://outra.example.com")
@DisplayName("CORS: origens vindas da propriedade (lista com vírgula)")
class CorsOrigensTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("as duas origens da lista passam no preflight")
	void origensDaLista() throws Exception {
		for (String origem : new String[] { "https://fichas-rpg.pages.dev", "https://outra.example.com" }) {
			mockMvc.perform(preflight(origem)).andExpect(status().isOk())
					.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origem));
		}
	}

	@Test
	@DisplayName("a origem que não está na lista (nem a de desenvolvimento) é recusada")
	void origemForaDaLista() throws Exception {
		mockMvc.perform(preflight("http://localhost:4200")).andExpect(status().isForbidden());
		mockMvc.perform(preflight("https://invasor.example.com")).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("o actuator passa pela mesma cadeia do .cors(): GET /actuator/health/liveness de origem permitida leva o Access-Control-Allow-Origin; de origem não permitida, não")
	void livenessComCors() throws Exception {
		mockMvc.perform(get("/actuator/health/liveness").with(anonymous()).header(HttpHeaders.ORIGIN, "https://fichas-rpg.pages.dev"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://fichas-rpg.pages.dev"));
		mockMvc.perform(get("/actuator/health/liveness").with(anonymous()).header(HttpHeaders.ORIGIN, "https://invasor.example.com"))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		mockMvc.perform(options("/actuator/health/liveness").with(anonymous()).header(HttpHeaders.ORIGIN, "https://fichas-rpg.pages.dev")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://fichas-rpg.pages.dev"));
	}

	private MockHttpServletRequestBuilder preflight(String origem) {
		return options("/me").with(anonymous()).header(HttpHeaders.ORIGIN, origem)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET").header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization");
	}
}
