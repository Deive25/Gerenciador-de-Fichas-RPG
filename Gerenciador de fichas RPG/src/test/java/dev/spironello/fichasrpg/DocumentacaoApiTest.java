package dev.spironello.fichasrpg;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("Documentação OpenAPI (springdoc)")
class DocumentacaoApiTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("GET /v3/api-docs devolve 200 e descreve os caminhos da ficha")
	void apiDocsListaOsCaminhos() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths", hasKey("/fichas")))
				.andExpect(jsonPath("$.paths", hasKey("/fichas/{id}")));
	}

	@Test
	@DisplayName("GET /swagger-ui/index.html devolve 200")
	void swaggerUiResponde() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}
}
