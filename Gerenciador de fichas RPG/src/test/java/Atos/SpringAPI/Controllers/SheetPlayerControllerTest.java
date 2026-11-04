package Atos.SpringAPI.Controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import Atos.SpringAPI.IntegrationTestBase;
import Atos.SpringAPI.Models.SheetPlayer;
import Atos.SpringAPI.Repositories.SheetPlayerRepository;

/**
 * Testes de caracterização: registram o que a API FAZ HOJE, inclusive o que é bug.
 * Servem de rede de segurança para as próximas refatorações. Quando um bug for corrigido,
 * o teste correspondente deve ser atualizado de propósito para o novo comportamento.
 */
@DisplayName("API /sheetPlayer (comportamento atual)")
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
	@DisplayName("GET /sheetPlayer/{id} inexistente devolve 200 com corpo \"null\"")
	void buscarFichaInexistente() throws Exception {
		// Comportamento atual (bug): o controller devolve Optional.empty(), que o Jackson
		// serializa como o texto "null". O cliente recebe 200 OK em vez de 404 Not Found.
		// Será corrigido no incremento 1.3.
		mockMvc.perform(get("/sheetPlayer/{id}", ID_INEXISTENTE))
				.andExpect(status().isOk())
				.andExpect(content().string("null"));
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

		mockMvc.perform(post("/sheetPlayer")
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", notNullValue()))
				.andExpect(jsonPath("$.nameJ").value("Arthur"))
				.andExpect(jsonPath("$.nameC").value("Ana"))
				.andExpect(jsonPath("$.gender").value("F"))
				.andExpect(jsonPath("$.age").value(30))
				.andExpect(jsonPath("$.str").value(50))
				.andExpect(jsonPath("$.lifeA", nullValue())); // campos opcionais não enviados ficam null

		assertThat(repository.findAll())
				.singleElement()
				.satisfies(salva -> {
					assertThat(salva.getNameJ()).isEqualTo("Arthur");
					assertThat(salva.getSTR()).isEqualTo(50);
				});
	}

	@Test
	@DisplayName("POST /sheetPlayer sem nameJ/nameC/gender falha no banco e nada é salvo")
	void criarFichaSemCamposObrigatorios() throws Exception {
		String corpo = objectMapper.writeValueAsString(Map.of("age", 30));

		// Comportamento atual (bug): não há validação de entrada. A falta dos campos
		// obrigatórios só é detectada ao salvar (DataIntegrityViolationException) e
		// ninguém trata a exceção: na aplicação real o cliente recebe HTTP 500 em vez de
		// 400 Bad Request. Deve ser tratado quando houver validação/tratamento de erros.
		// (O MockMvc não simula a página de erro, por isso a exceção aparece direto.)
		assertThatThrownBy(() -> mockMvc.perform(post("/sheetPlayer")
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo)))
				.hasCauseInstanceOf(DataIntegrityViolationException.class);

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
	@DisplayName("PUT /sheetPlayer/{id} troca a ficha inteira: campos omitidos viram null")
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

		// Comportamento atual: o PUT copia TODOS os campos do corpo para a ficha; o que
		// não vier no JSON é gravado como null (não é um "patch" parcial).
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
	@DisplayName("PUT /sheetPlayer/{id} inexistente devolve 404 sem corpo")
	void atualizarFichaInexistente() throws Exception {
		String corpo = objectMapper.writeValueAsString(Map.of(
				"nameJ", "Arthur",
				"nameC", "Ana",
				"gender", "F"));

		mockMvc.perform(put("/sheetPlayer/{id}", ID_INEXISTENTE)
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo))
				.andExpect(status().isNotFound())
				.andExpect(content().string(""));

		assertThat(repository.count()).isZero();
	}

	// ---------- DELETE /sheetPlayer/{id} ----------

	@Test
	@DisplayName("DELETE /sheetPlayer/{id} existente devolve 200 sem corpo e remove a ficha")
	void excluirFichaExistente() throws Exception {
		SheetPlayer ficha = salvar("Arthur", "Ana", "F");

		// O status é 200 (e não 204 No Content), porque o método do controller é void.
		mockMvc.perform(delete("/sheetPlayer/{id}", ficha.getId()))
				.andExpect(status().isOk())
				.andExpect(content().string(""));

		assertThat(repository.existsById(ficha.getId())).isFalse();
	}

	@Test
	@DisplayName("DELETE /sheetPlayer/{id} inexistente estoura exceção (HTTP 500 na aplicação real)")
	void excluirFichaInexistente() {
		// Comportamento atual (bug): deleteById lança EmptyResultDataAccessException e o
		// controller não trata. Na aplicação real o cliente recebe HTTP 500 (Internal Server
		// Error) com o JSON de erro padrão do Spring, em vez de 404 Not Found.
		// (O MockMvc não simula a página de erro, por isso a exceção aparece direto.)
		// Será corrigido no incremento 1.3.
		assertThatThrownBy(() -> mockMvc.perform(delete("/sheetPlayer/{id}", ID_INEXISTENTE)))
				.hasCauseInstanceOf(EmptyResultDataAccessException.class);
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
