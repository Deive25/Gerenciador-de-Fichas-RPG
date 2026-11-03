package dev.spironello.fichasrpg.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import tools.jackson.databind.ObjectMapper;

import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.model.SheetPlayer;
import dev.spironello.fichasrpg.repository.SheetPlayerRepository;

/**
 * Especificam o comportamento esperado da API /sheetPlayer (códigos HTTP e corpos de erro).
 * Nasceram como testes de caracterização, que registravam o comportamento de então, com
 * bugs incluídos; no incremento 1.3 os bugs foram corrigidos e estes testes foram
 * atualizados de propósito para exigir o comportamento correto.
 */
@DisplayName("API /sheetPlayer (comportamento esperado)")
class SheetPlayerControllerTest extends IntegrationTestBase {

	private static final Long ID_INEXISTENTE = 999_999L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SheetPlayerRepository repository;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void limparBanco() {
		repository.deleteAll();
	}

	// ---------- GET /sheetPlayer ----------

	@Test
	@DisplayName("GET /sheetPlayer sem fichas devolve 200 e lista vazia")
	void listarSemFichas() throws Exception {
		mockMvc.perform(get("/sheetPlayer"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	@DisplayName("GET /sheetPlayer com fichas devolve 200 e todas as fichas")
	void listarComFichas() throws Exception {
		salvar("Arthur", "Ana", "F");
		salvar("Bruno", "Beto", "M");

		mockMvc.perform(get("/sheetPlayer"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[*].nameJ", containsInAnyOrder("Arthur", "Bruno")))
				.andExpect(jsonPath("$[*].nameC", containsInAnyOrder("Ana", "Beto")));
	}

	// ---------- GET /sheetPlayer/{id} ----------

	@Test
	@DisplayName("GET /sheetPlayer/{id} existente devolve 200 e a ficha")
	void buscarFichaExistente() throws Exception {
		SheetPlayer ficha = salvar("Arthur", "Ana", "F");

		mockMvc.perform(get("/sheetPlayer/{id}", ficha.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ficha.getId()))
				.andExpect(jsonPath("$.nameJ").value("Arthur"))
				.andExpect(jsonPath("$.nameC").value("Ana"))
				.andExpect(jsonPath("$.gender").value("F"));
	}

	@Test
	@DisplayName("GET /sheetPlayer/{id} inexistente devolve 404 com ProblemDetail")
	void buscarFichaInexistente() throws Exception {
		mockMvc.perform(get("/sheetPlayer/{id}", ID_INEXISTENTE))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"))
				.andExpect(jsonPath("$.detail", containsString(String.valueOf(ID_INEXISTENTE))));
	}

	// ---------- POST /sheetPlayer ----------

	@Test
	@DisplayName("POST /sheetPlayer devolve 201 e a ficha criada, com id gerado")
	void criarFicha() throws Exception {
		String corpo = objectMapper.writeValueAsString(Map.of(
				"nameJ", "Arthur",
				"nameC", "Ana",
				"gender", "F",
				"age", 30,
				"str", 50)); // no JSON os atributos vão em minúsculas (getSTR -> "str")

		MvcResult resultado = mockMvc.perform(post("/sheetPlayer")
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", notNullValue()))
				.andExpect(jsonPath("$.nameJ").value("Arthur"))
				.andExpect(jsonPath("$.nameC").value("Ana"))
				.andExpect(jsonPath("$.gender").value("F"))
				.andExpect(jsonPath("$.age").value(30))
				.andExpect(jsonPath("$.str").value(50))
				.andExpect(jsonPath("$.lifeA", nullValue())) // campos opcionais não enviados ficam null
				.andReturn();

		Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
		assertThat(resultado.getResponse().getHeader("Location")).isEqualTo("/sheetPlayer/" + id);

		assertThat(repository.findAll())
				.singleElement()
				.satisfies(salva -> {
					assertThat(salva.getNameJ()).isEqualTo("Arthur");
					assertThat(salva.getSTR()).isEqualTo(50);
				});
	}

	@Test
	@DisplayName("POST /sheetPlayer sem nameJ/nameC/gender devolve 400 com os erros por campo e nada é salvo")
	void criarFichaSemCamposObrigatorios() throws Exception {
		String corpo = objectMapper.writeValueAsString(Map.of("age", 30));

		mockMvc.perform(post("/sheetPlayer")
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errors.nameJ").value("é obrigatório"))
				.andExpect(jsonPath("$.errors.nameC").value("é obrigatório"))
				.andExpect(jsonPath("$.errors.gender").value("é obrigatório"));

		assertThat(repository.count()).isZero();
	}

	// ---------- PUT /sheetPlayer/{id} ----------

	@Test
	@DisplayName("PUT /sheetPlayer/{id} existente devolve 200 e atualiza a ficha")
	void atualizarFichaExistente() throws Exception {
		SheetPlayer ficha = salvar("Arthur", "Ana", "F");
		String corpo = objectMapper.writeValueAsString(Map.of(
				"nameJ", "Arthur Atualizado",
				"nameC", "Ana Atualizada",
				"gender", "M",
				"age", 31,
				"str", 60));

		mockMvc.perform(put("/sheetPlayer/{id}", ficha.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ficha.getId()))
				.andExpect(jsonPath("$.nameJ").value("Arthur Atualizado"))
				.andExpect(jsonPath("$.nameC").value("Ana Atualizada"))
				.andExpect(jsonPath("$.gender").value("M"))
				.andExpect(jsonPath("$.age").value(31))
				.andExpect(jsonPath("$.str").value(60));

		SheetPlayer noBanco = repository.findById(ficha.getId()).orElseThrow();
		assertThat(noBanco.getNameJ()).isEqualTo("Arthur Atualizado");
		assertThat(noBanco.getAge()).isEqualTo(31);
		assertThat(noBanco.getSTR()).isEqualTo(60);
	}

	@Test
	@DisplayName("PUT /sheetPlayer/{id} troca a ficha inteira, de propósito: campos omitidos viram null")
	void atualizarFichaApagaCamposOmitidos() throws Exception {
		SheetPlayer ficha = new SheetPlayer();
		ficha.setNameJ("Arthur");
		ficha.setNameC("Ana");
		ficha.setGender("F");
		ficha.setAge(30);
		ficha.setSTR(50);
		ficha = repository.save(ficha);
		String corpo = objectMapper.writeValueAsString(Map.of(
				"nameJ", "Arthur",
				"nameC", "Ana",
				"gender", "F"));

		// Intencional (ADR 0008): o PUT é substituição completa. Copia TODOS os campos do
		// corpo para a ficha; o que não vier no JSON é gravado como null. Não há PATCH
		// porque o Angular sempre envia a ficha inteira.
		mockMvc.perform(put("/sheetPlayer/{id}", ficha.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.age", nullValue()))
				.andExpect(jsonPath("$.str", nullValue()));

		SheetPlayer noBanco = repository.findById(ficha.getId()).orElseThrow();
		assertThat(noBanco.getAge()).isNull();
		assertThat(noBanco.getSTR()).isNull();
	}

	@Test
	@DisplayName("PUT /sheetPlayer/{id} inexistente devolve 404 com ProblemDetail e não cria nada")
	void atualizarFichaInexistente() throws Exception {
		String corpo = objectMapper.writeValueAsString(Map.of(
				"nameJ", "Arthur",
				"nameC", "Ana",
				"gender", "F"));

		mockMvc.perform(put("/sheetPlayer/{id}", ID_INEXISTENTE)
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail", containsString(String.valueOf(ID_INEXISTENTE))));

		assertThat(repository.count()).isZero();
	}

	// ---------- DELETE /sheetPlayer/{id} ----------

	@Test
	@DisplayName("DELETE /sheetPlayer/{id} existente devolve 204 sem corpo e remove a ficha")
	void excluirFichaExistente() throws Exception {
		SheetPlayer ficha = salvar("Arthur", "Ana", "F");

		mockMvc.perform(delete("/sheetPlayer/{id}", ficha.getId()))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		assertThat(repository.existsById(ficha.getId())).isFalse();
	}

	@Test
	@DisplayName("DELETE /sheetPlayer/{id} inexistente devolve 404 com ProblemDetail e não remove nada")
	void excluirFichaInexistente() throws Exception {
		salvar("Arthur", "Ana", "F");

		mockMvc.perform(delete("/sheetPlayer/{id}", ID_INEXISTENTE))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404));

		assertThat(repository.count()).isEqualTo(1);
	}

	// ---------- apoio ----------

	private SheetPlayer salvar(String nameJ, String nameC, String gender) {
		SheetPlayer ficha = new SheetPlayer();
		ficha.setNameJ(nameJ);
		ficha.setNameC(nameC);
		ficha.setGender(gender);
		return repository.save(ficha);
	}
}
