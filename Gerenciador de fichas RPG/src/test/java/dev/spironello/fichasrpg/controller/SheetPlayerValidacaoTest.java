package dev.spironello.fichasrpg.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.model.SheetPlayer;
import dev.spironello.fichasrpg.repository.SheetPlayerRepository;

/**
 * Entrada inválida e regras de entrada da API (incremento 1.3, ADR 0007 e 0008): JSON
 * estrito, limites de tamanho, tipos errados e o tratamento do id no corpo.
 */
@DisplayName("API /sheetPlayer (validação de entrada e JSON estrito)")
class SheetPlayerValidacaoTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SheetPlayerRepository repository;

	@BeforeEach
	void limparBanco() {
		repository.deleteAll();
	}

	@Test
	@DisplayName("POST com chave desconhecida (\"STR\") devolve 400 citando a chave")
	void postComChaveDesconhecida() throws Exception {
		String corpo = """
				{"nameJ": "Arthur", "nameC": "Ana", "gender": "F", "STR": 50}""";

		problema400(enviar("POST", "/sheetPlayer", corpo))
				.andExpect(jsonPath("$.detail", containsString("STR")));

		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("POST com nameJ de 256 caracteres devolve 400 e nada é salvo")
	void postComNomeLongoDemais() throws Exception {
		String corpo = """
				{"nameJ": "%s", "nameC": "Ana", "gender": "F"}""".formatted("a".repeat(256));

		problema400(enviar("POST", "/sheetPlayer", corpo))
				.andExpect(jsonPath("$.errors.nameJ").value("deve ter no máximo 255 caracteres"));

		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("POST com nameJ de 255 caracteres é aceito (limite da coluna varchar(255))")
	void postComNomeNoLimite() throws Exception {
		String corpo = """
				{"nameJ": "%s", "nameC": "Ana", "gender": "F"}""".formatted("a".repeat(255));

		enviar("POST", "/sheetPlayer", corpo).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("POST com tipo errado (\"age\": \"abc\") devolve 400 citando o campo")
	void postComTipoErrado() throws Exception {
		String corpo = """
				{"nameJ": "Arthur", "nameC": "Ana", "gender": "F", "age": "abc"}""";

		problema400(enviar("POST", "/sheetPlayer", corpo))
				.andExpect(jsonPath("$.detail", containsString("age")));
	}

	@Test
	@DisplayName("POST com JSON malformado devolve 400 ProblemDetail")
	void postComJsonMalformado() throws Exception {
		problema400(enviar("POST", "/sheetPlayer", "{nameJ: "));
	}

	@Test
	@DisplayName("PUT inválido devolve 400 com os erros por campo e não altera a ficha")
	void putInvalido() throws Exception {
		SheetPlayer ficha = salvar();

		problema400(enviar("PUT", "/sheetPlayer/" + ficha.getId(), """
				{"nameJ": "  ", "nameC": "Ana", "gender": "F"}"""))
				.andExpect(jsonPath("$.errors.nameJ").value("é obrigatório"));

		assertThat(repository.findById(ficha.getId()).orElseThrow().getNameJ()).isEqualTo("Arthur");
	}

	@Test
	@DisplayName("PUT com chave desconhecida devolve 400 citando a chave")
	void putComChaveDesconhecida() throws Exception {
		SheetPlayer ficha = salvar();

		problema400(enviar("PUT", "/sheetPlayer/" + ficha.getId(), """
				{"nameJ": "Arthur", "nameC": "Ana", "gender": "F", "STR": 50}"""))
				.andExpect(jsonPath("$.detail", containsString("STR")));
	}

	@Test
	@DisplayName("PUT com id diferente no corpo: vale o id da URL")
	void putComIdDiferenteNoCorpo() throws Exception {
		SheetPlayer alvo = salvar();
		SheetPlayer outra = salvar();

		enviar("PUT", "/sheetPlayer/" + alvo.getId(), """
				{"id": %d, "nameJ": "Atualizado", "nameC": "Ana", "gender": "F"}""".formatted(outra.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(alvo.getId()));

		assertThat(repository.findById(alvo.getId()).orElseThrow().getNameJ()).isEqualTo("Atualizado");
		assertThat(repository.findById(outra.getId()).orElseThrow().getNameJ()).isEqualTo("Arthur");
		assertThat(repository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("POST com id no corpo: o id é ignorado e o banco gera um novo")
	void postComIdNoCorpo() throws Exception {
		enviar("POST", "/sheetPlayer", """
				{"id": 777777, "nameJ": "Arthur", "nameC": "Ana", "gender": "F"}""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber());

		assertThat(repository.existsById(777_777L)).isFalse();
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("POST com id null no corpo (como o Angular envia) funciona")
	void postComIdNulo() throws Exception {
		enviar("POST", "/sheetPlayer", """
				{"id": null, "nameJ": "Arthur", "nameC": "Ana", "gender": "F"}""")
				.andExpect(status().isCreated());
	}

	// ---------- apoio ----------

	private ResultActions enviar(String metodo, String url, String corpo) throws Exception {
		var requisicao = "PUT".equals(metodo) ? put(url) : post(url);
		return mockMvc.perform(requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	private ResultActions problema400(ResultActions resultado) throws Exception {
		return resultado
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").exists());
	}

	private SheetPlayer salvar() {
		SheetPlayer ficha = new SheetPlayer();
		ficha.setNameJ("Arthur");
		ficha.setNameC("Ana");
		ficha.setGender("F");
		return repository.save(ficha);
	}
}
