package dev.spironello.fichasrpg.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroups;
import org.springframework.test.web.servlet.MockMvc;

import dev.spironello.fichasrpg.IntegrationTestBase;

/**
 * A sonda de vida que o Render e o front chamam (2.6, spec §5.3): pública, sem token e sem consultar o banco (assim
 * não acorda o Neon à toa). O resto do actuator não é exposto.
 */
@DisplayName("Saúde da API (actuator)")
class SaudeDaApiTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private HealthEndpointGroups grupos;

	@Test
	@DisplayName("/actuator/health/liveness responde 200 UP sem token")
	void livenessPublico() throws Exception {
		mockMvc.perform(get("/actuator/health/liveness").with(anonymous()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	@DisplayName("o grupo liveness só olha o estado da aplicação: o banco (db) não entra")
	void livenessNaoConsultaOBanco() {
		assertThat(grupos.get("liveness")).isNotNull();
		assertThat(grupos.get("liveness").isMember("livenessState")).isTrue();
		assertThat(grupos.get("liveness").isMember("db")).isFalse();
	}

	@Test
	@DisplayName("a saúde completa (que consulta o banco) e os outros endpoints do actuator não são públicos")
	void soALivenessEPublica() throws Exception {
		mockMvc.perform(get("/actuator/health").with(anonymous())).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/actuator/env").with(anonymous())).andExpect(status().isUnauthorized());
		// mesmo logado, só o health está exposto: o resto nem existe
		mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
		mockMvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
	}
}
