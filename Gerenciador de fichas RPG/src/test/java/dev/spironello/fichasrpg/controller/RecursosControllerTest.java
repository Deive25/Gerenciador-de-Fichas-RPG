package dev.spironello.fichasrpg.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.repository.FichaRepository;

/** PATCH /fichas/{id}/recursos de ponta a ponta: só os atuais de PV, SAN e PE, e só os enviados mudam. */
@DisplayName("API PATCH /fichas/{id}/recursos")
class RecursosControllerTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository repository;

	@BeforeEach
	void limparBanco() {
		repository.deleteAll();
	}

	private Long criarFicha() {
		FichaOrdem ficha = FichasDeTeste.ordem("Ana");
		ficha.setPvAtual(8);
		ficha.setPvMax(10);
		ficha.setSanAtual(5);
		ficha.setSanMax(6);
		return repository.save(ficha).getId();
	}

	private ResultActions patchar(Object id, String corpo) throws Exception {
		return mockMvc.perform(patch("/fichas/" + id + "/recursos")
				.contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	@Test
	@DisplayName("200 muda só o PV enviado; SAN e os máximos ficam como estavam")
	void mudaSoOEnviado() throws Exception {
		Long id = criarFicha();

		patchar(id, "{\"PV\": 10}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.recursos.PV.atual").value(10))
				.andExpect(jsonPath("$.recursos.PV.maximo").value(10))
				.andExpect(jsonPath("$.recursos.SAN.atual").value(5));

		FichaOrdem salva = (FichaOrdem) repository.findById(id).orElseThrow();
		assertThat(salva.getPvAtual()).isEqualTo(10);
		assertThat(salva.getSanAtual()).isEqualTo(5);
	}

	@Test
	@DisplayName("200 aceita PV, SAN e PE juntos, inclusive PE que estava vazio")
	void mudaOsTres() throws Exception {
		Long id = criarFicha();

		patchar(id, "{\"PV\": 1, \"SAN\": 2, \"PE\": 3}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.recursos.PV.atual").value(1))
				.andExpect(jsonPath("$.recursos.SAN.atual").value(2))
				.andExpect(jsonPath("$.recursos.PE.atual").value(3));
	}

	@Test
	@DisplayName("400 para código desconhecido, com a chave do próprio código em errors")
	void codigoDesconhecido() throws Exception {
		Long id = criarFicha();

		patchar(id, "{\"DES\": 1}")
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Dados inválidos"))
				.andExpect(jsonPath("$.errors.DES").value("não existe em Ordem Paranormal"));
	}

	@Test
	@DisplayName("400 para valor negativo, com a chave do código em errors, e nada é gravado")
	void valorNegativo() throws Exception {
		Long id = criarFicha();

		patchar(id, "{\"SAN\": 1, \"PV\": -1}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.PV", containsString("0")));

		FichaOrdem salva = (FichaOrdem) repository.findById(id).orElseThrow();
		assertThat(salva.getSanAtual()).isEqualTo(5);
	}

	@Test
	@DisplayName("400 para valor nulo")
	void valorNulo() throws Exception {
		Long id = criarFicha();

		patchar(id, "{\"PE\": null}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.PE", containsString("0")));
	}

	@Test
	@DisplayName("404 para ficha inexistente")
	void fichaInexistente() throws Exception {
		patchar(999_999L, "{\"PV\": 1}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}
}
