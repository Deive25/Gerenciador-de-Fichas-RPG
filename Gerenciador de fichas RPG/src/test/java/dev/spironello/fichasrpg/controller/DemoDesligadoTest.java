package dev.spironello.fichasrpg.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import dev.spironello.fichasrpg.IntegrationTestBase;

/** Sem o perfil demo (o padrão, e o dos testes), o POST /auth/demo não existe: 404, com ou sem corpo. */
@DisplayName("POST /auth/demo sem o perfil demo")
class DemoDesligadoTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("responde 404 (e não 405), com ou sem corpo")
	void naoExiste() throws Exception {
		mockMvc.perform(post("/auth/demo").with(anonymous())).andExpect(status().isNotFound());
		mockMvc.perform(post("/auth/demo").with(anonymous()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"codigoConvite\":\"AAAAAAAAAAAA\"}")).andExpect(status().isNotFound());
	}
}
