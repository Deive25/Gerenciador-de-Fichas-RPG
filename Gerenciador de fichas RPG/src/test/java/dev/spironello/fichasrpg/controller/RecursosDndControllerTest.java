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
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.repository.FichaRepository;

/** PATCH /fichas/{id}/recursos em D&D: só PV e PV_TEMP, e só os enviados mudam. */
@DisplayName("API PATCH /fichas/{id}/recursos (D&D)")
class RecursosDndControllerTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository repository;

	@BeforeEach
	void limparBanco() {
		repository.deleteAll();
	}

	private Long criarFicha() {
		FichaDnd ficha = FichasDeTeste.dnd("Ana");
		ficha.setPvAtual(8);
		ficha.setPvMax(12);
		ficha.setPvTemporario(1);
		return repository.save(ficha).getId();
	}

	private ResultActions patchar(Object id, String corpo) throws Exception {
		return mockMvc.perform(patch("/fichas/" + id + "/recursos")
				.contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	@Test
	@DisplayName("200 muda PV e PV_TEMP; o máximo do PV não muda")
	void mudaPvEPvTemp() throws Exception {
		Long id = criarFicha();

		patchar(id, "{\"PV\": 5, \"PV_TEMP\": 3}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.recursos.PV.atual").value(5))
				.andExpect(jsonPath("$.recursos.PV.maximo").value(12))
				.andExpect(jsonPath("$.recursos.PV_TEMP.atual").value(3));

		FichaDnd salva = (FichaDnd) repository.findById(id).orElseThrow();
		assertThat(salva.getPvAtual()).isEqualTo(5);
		assertThat(salva.getPvMax()).isEqualTo(12);
		assertThat(salva.getPvTemporario()).isEqualTo(3);
	}

	@Test
	@DisplayName("200 muda só o PV_TEMP e o PV fica como estava")
	void mudaSoPvTemp() throws Exception {
		Long id = criarFicha();

		patchar(id, "{\"PV_TEMP\": 7}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.recursos.PV.atual").value(8))
				.andExpect(jsonPath("$.recursos.PV_TEMP.atual").value(7));
	}

	@Test
	@DisplayName("400 para código que não existe em D&D, com a chave do código em errors")
	void codigoDesconhecido() throws Exception {
		Long id = criarFicha();

		patchar(id, "{\"SAN\": 1}")
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors.SAN").value("não existe em D&D"));
	}

	@Test
	@DisplayName("400 para valor negativo ou nulo, e nada é gravado")
	void valorInvalido() throws Exception {
		Long id = criarFicha();

		patchar(id, "{\"PV_TEMP\": 4, \"PV\": -1}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.PV", containsString("0")));
		patchar(id, "{\"PV\": null}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.PV", containsString("0")));

		FichaDnd salva = (FichaDnd) repository.findById(id).orElseThrow();
		assertThat(salva.getPvTemporario()).isEqualTo(1);
		assertThat(salva.getPvAtual()).isEqualTo(8);
	}

	@Test
	@DisplayName("404 para ficha inexistente")
	void fichaInexistente() throws Exception {
		patchar(999_999L, "{\"PV\": 1}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}
}
