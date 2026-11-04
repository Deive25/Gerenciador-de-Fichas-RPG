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
import dev.spironello.fichasrpg.model.Pericia;
import dev.spironello.fichasrpg.model.SheetPlayer;
import dev.spironello.fichasrpg.repository.SheetPlayerRepository;

/**
 * Especificam o comportamento esperado da API /fichas (códigos HTTP e corpos de erro).
 * Nasceram como testes de caracterização, que registravam o comportamento de então, com
 * bugs incluídos; no incremento 1.3 os bugs foram corrigidos e estes testes foram
 * atualizados de propósito para exigir o comportamento correto.
 */
@DisplayName("API /fichas (comportamento esperado)")
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

	// ---------- GET /fichas ----------

	@Test
	@DisplayName("GET /fichas sem fichas devolve 200 e lista vazia")
	void listarSemFichas() throws Exception {
		mockMvc.perform(get("/fichas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	@DisplayName("GET /fichas com fichas devolve 200 e todas as fichas")
	void listarComFichas() throws Exception {
		salvar("Arthur", "Ana", "F");
		salvar("Bruno", "Beto", "M");

		mockMvc.perform(get("/fichas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[*].jogador", containsInAnyOrder("Arthur", "Bruno")))
				.andExpect(jsonPath("$[*].personagem", containsInAnyOrder("Ana", "Beto")));
	}

	// ---------- GET /fichas/{id} ----------

	@Test
	@DisplayName("GET /fichas/{id} existente devolve 200 e a ficha")
	void buscarFichaExistente() throws Exception {
		SheetPlayer ficha = salvar("Arthur", "Ana", "F");

		mockMvc.perform(get("/fichas/{id}", ficha.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ficha.getId()))
				.andExpect(jsonPath("$.jogador").value("Arthur"))
				.andExpect(jsonPath("$.personagem").value("Ana"))
				.andExpect(jsonPath("$.genero").value("F"));
	}

	@Test
	@DisplayName("GET /fichas/{id} inexistente devolve 404 com ProblemDetail")
	void buscarFichaInexistente() throws Exception {
		mockMvc.perform(get("/fichas/{id}", ID_INEXISTENTE))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"))
				.andExpect(jsonPath("$.detail", containsString(String.valueOf(ID_INEXISTENTE))));
	}

	// ---------- POST /fichas ----------

	@Test
	@DisplayName("POST /fichas devolve 201 e a ficha criada, com id gerado")
	void criarFicha() throws Exception {
		String corpo = objectMapper.writeValueAsString(Map.of(
				"jogador", "Arthur",
				"personagem", "Ana",
				"genero", "F",
				"idade", 30,
				"forca", 50)); // no JSON, o atributo STR da entidade chama-se "forca"

		MvcResult resultado = mockMvc.perform(post("/fichas")
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", notNullValue()))
				.andExpect(jsonPath("$.jogador").value("Arthur"))
				.andExpect(jsonPath("$.personagem").value("Ana"))
				.andExpect(jsonPath("$.genero").value("F"))
				.andExpect(jsonPath("$.idade").value(30))
				.andExpect(jsonPath("$.forca").value(50))
				.andExpect(jsonPath("$.vidaAtual", nullValue())) // campos opcionais não enviados ficam null
				.andReturn();

		Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
		assertThat(resultado.getResponse().getHeader("Location")).isEqualTo("/fichas/" + id);

		assertThat(repository.findAll())
				.singleElement()
				.satisfies(salva -> {
					assertThat(salva.getNameJ()).isEqualTo("Arthur");
					assertThat(salva.getSTR()).isEqualTo(50);
				});
	}

	@Test
	@DisplayName("POST /fichas sem jogador/personagem/genero devolve 400 com os erros por campo e nada é salvo")
	void criarFichaSemCamposObrigatorios() throws Exception {
		String corpo = objectMapper.writeValueAsString(Map.of("idade", 30));

		mockMvc.perform(post("/fichas")
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("Há campos inválidos na requisição. Veja a propriedade errors."))
				.andExpect(jsonPath("$.errors.jogador").value("é obrigatório"))
				.andExpect(jsonPath("$.errors.personagem").value("é obrigatório"))
				.andExpect(jsonPath("$.errors.genero").value("é obrigatório"));

		assertThat(repository.count()).isZero();
	}

	// ---------- PUT /fichas/{id} ----------

	@Test
	@DisplayName("PUT /fichas/{id} existente devolve 200 e atualiza a ficha")
	void atualizarFichaExistente() throws Exception {
		SheetPlayer ficha = salvar("Arthur", "Ana", "F");
		String corpo = objectMapper.writeValueAsString(Map.of(
				"jogador", "Arthur Atualizado",
				"personagem", "Ana Atualizada",
				"genero", "M",
				"idade", 31,
				"forca", 60));

		mockMvc.perform(put("/fichas/{id}", ficha.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ficha.getId()))
				.andExpect(jsonPath("$.jogador").value("Arthur Atualizado"))
				.andExpect(jsonPath("$.personagem").value("Ana Atualizada"))
				.andExpect(jsonPath("$.genero").value("M"))
				.andExpect(jsonPath("$.idade").value(31))
				.andExpect(jsonPath("$.forca").value(60));

		SheetPlayer noBanco = repository.findById(ficha.getId()).orElseThrow();
		assertThat(noBanco.getNameJ()).isEqualTo("Arthur Atualizado");
		assertThat(noBanco.getAge()).isEqualTo(31);
		assertThat(noBanco.getSTR()).isEqualTo(60);
	}

	@Test
	@DisplayName("PUT /fichas/{id} troca a ficha inteira, de propósito: campos omitidos viram null")
	void atualizarFichaApagaCamposOmitidos() throws Exception {
		SheetPlayer ficha = new SheetPlayer();
		ficha.setNameJ("Arthur");
		ficha.setNameC("Ana");
		ficha.setGender("F");
		ficha.setAge(30);
		ficha.setSTR(50);
		ficha = repository.save(ficha);
		String corpo = objectMapper.writeValueAsString(Map.of(
				"jogador", "Arthur",
				"personagem", "Ana",
				"genero", "F"));

		// Intencional (ADR 0008): o PUT é substituição completa. Copia TODOS os campos do
		// corpo para a ficha; o que não vier no JSON é gravado como null. Não há PATCH
		// porque o Angular sempre envia a ficha inteira.
		mockMvc.perform(put("/fichas/{id}", ficha.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.idade", nullValue()))
				.andExpect(jsonPath("$.forca", nullValue()));

		SheetPlayer noBanco = repository.findById(ficha.getId()).orElseThrow();
		assertThat(noBanco.getAge()).isNull();
		assertThat(noBanco.getSTR()).isNull();
	}

	@Test
	@DisplayName("PUT /fichas/{id} inexistente devolve 404 com ProblemDetail e não cria nada")
	void atualizarFichaInexistente() throws Exception {
		String corpo = objectMapper.writeValueAsString(Map.of(
				"jogador", "Arthur",
				"personagem", "Ana",
				"genero", "F"));

		mockMvc.perform(put("/fichas/{id}", ID_INEXISTENTE)
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail", containsString(String.valueOf(ID_INEXISTENTE))));

		assertThat(repository.count()).isZero();
	}

	// ---------- pericias no contrato novo (incremento 1.6) ----------

	@Test
	@DisplayName("POST com pericias devolve só as preenchidas, por código do enum, e grava na tabela de perícias")
	void criarFichaComPericias() throws Exception {
		String corpo = """
				{"jogador": "Arthur", "personagem": "Ana", "genero": "F",
				 "pericias": {"ACROBACIA": 40, "PERCEPCAO": 55}}""";

		mockMvc.perform(post("/fichas").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.pericias.ACROBACIA").value(40))
				.andExpect(jsonPath("$.pericias.PERCEPCAO").value(55))
				.andExpect(jsonPath("$.pericias.length()").value(2));

		assertThat(repository.findAll()).singleElement()
				.satisfies(salva -> assertThat(salva.getPericias())
						.containsOnly(Map.entry(Pericia.ACROBACIA, 40), Map.entry(Pericia.PERCEPCAO, 55)));
	}

	@Test
	@DisplayName("POST com pericias de valor null (como o Angular envia) ignora as nulas: não viram linha nem chave")
	void periciasNulasSaoIgnoradas() throws Exception {
		StringBuilder pericias = new StringBuilder();
		for (Pericia p : Pericia.values()) {
			pericias.append('"').append(p.name()).append("\": ")
					.append(p == Pericia.CURA ? "30" : "null").append(", ");
		}
		pericias.setLength(pericias.length() - 2);
		String corpo = """
				{"id": null, "jogador": "Arthur", "personagem": "Ana", "genero": "F", "pericias": {%s}}"""
				.formatted(pericias);

		mockMvc.perform(post("/fichas").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.pericias.CURA").value(30))
				.andExpect(jsonPath("$.pericias.length()").value(1));

		assertThat(repository.findAll()).singleElement()
				.satisfies(salva -> assertThat(salva.getPericias()).containsOnly(Map.entry(Pericia.CURA, 30)));
	}

	@Test
	@DisplayName("PUT omitindo uma perícia (ou o objeto pericias inteiro) remove a que não veio")
	void putOmitindoPericiaRemove() throws Exception {
		SheetPlayer ficha = salvar("Arthur", "Ana", "F");
		ficha.getPericias().put(Pericia.ACROBACIA, 40);
		ficha.getPericias().put(Pericia.PERCEPCAO, 55);
		ficha = repository.save(ficha);

		mockMvc.perform(put("/fichas/{id}", ficha.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"jogador": "Arthur", "personagem": "Ana", "genero": "F", "pericias": {"PERCEPCAO": 60}}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pericias.PERCEPCAO").value(60))
				.andExpect(jsonPath("$.pericias.ACROBACIA").doesNotExist())
				.andExpect(jsonPath("$.pericias.length()").value(1));
		assertThat(repository.findById(ficha.getId()).orElseThrow().getPericias())
				.containsOnly(Map.entry(Pericia.PERCEPCAO, 60));

		mockMvc.perform(put("/fichas/{id}", ficha.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"jogador": "Arthur", "personagem": "Ana", "genero": "F"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pericias.length()").value(0));
		assertThat(repository.findById(ficha.getId()).orElseThrow().getPericias()).isEmpty();
	}

	// ---------- DELETE /fichas/{id} ----------

	@Test
	@DisplayName("DELETE /fichas/{id} existente devolve 204 sem corpo e remove a ficha")
	void excluirFichaExistente() throws Exception {
		SheetPlayer ficha = salvar("Arthur", "Ana", "F");

		mockMvc.perform(delete("/fichas/{id}", ficha.getId()))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		assertThat(repository.existsById(ficha.getId())).isFalse();
	}

	@Test
	@DisplayName("DELETE /fichas/{id} inexistente devolve 404 com ProblemDetail e não remove nada")
	void excluirFichaInexistente() throws Exception {
		salvar("Arthur", "Ana", "F");

		mockMvc.perform(delete("/fichas/{id}", ID_INEXISTENTE))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404));

		assertThat(repository.count()).isEqualTo(1);
	}

	// ---------- apoio ----------

	private SheetPlayer salvar(String jogador, String personagem, String genero) {
		SheetPlayer ficha = new SheetPlayer();
		ficha.setNameJ(jogador);
		ficha.setNameC(personagem);
		ficha.setGender(genero);
		return repository.save(ficha);
	}
}
