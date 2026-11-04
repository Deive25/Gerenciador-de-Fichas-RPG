package dev.spironello.fichasrpg.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.model.SheetPlayer;
import dev.spironello.fichasrpg.repository.SheetPlayerRepository;

/**
 * Entrada inválida e regras de entrada da API (incremento 1.3, ADR 0007 e 0008): JSON
 * estrito, limites de tamanho, tipos errados e o tratamento do id no corpo.
 */
@DisplayName("API /fichas (validação de entrada e JSON estrito)")
// O devtools (usado em spring-boot:run) liga este log por padrão e o Spring então registra, em WARN,
// a exceção de validação com o valor rejeitado. Ligamos aqui para reproduzir esse cenário.
@TestPropertySource(properties = "spring.mvc.log-resolved-exception=true")
@ExtendWith(OutputCaptureExtension.class)
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
				{"jogador": "Arthur", "personagem": "Ana", "genero": "F", "STR": 50}""";

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.detail", containsString("STR")));

		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("POST com jogador de 256 caracteres devolve 400 e nada é salvo")
	void postComNomeLongoDemais() throws Exception {
		String corpo = """
				{"jogador": "%s", "personagem": "Ana", "genero": "F"}""".formatted("a".repeat(256));

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.errors.jogador").value("deve ter no máximo 255 caracteres"));

		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("POST com jogador de 255 caracteres é aceito (limite da coluna varchar(255))")
	void postComNomeNoLimite() throws Exception {
		String corpo = """
				{"jogador": "%s", "personagem": "Ana", "genero": "F"}""".formatted("a".repeat(255));

		enviar("POST", "/fichas", corpo).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("POST com tipo errado (\"idade\": \"abc\") devolve 400 citando o campo")
	void postComTipoErrado() throws Exception {
		String corpo = """
				{"jogador": "Arthur", "personagem": "Ana", "genero": "F", "idade": "abc"}""";

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.detail", containsString("idade")));
	}

	@Test
	@DisplayName("POST com JSON malformado devolve 400 ProblemDetail")
	void postComJsonMalformado() throws Exception {
		problema400(enviar("POST", "/fichas", "{jogador: "));
	}

	@Test
	@DisplayName("PUT inválido devolve 400 com os erros por campo e não altera a ficha")
	void putInvalido() throws Exception {
		SheetPlayer ficha = salvar();

		problema400(enviar("PUT", "/fichas/" + ficha.getId(), """
				{"jogador": "  ", "personagem": "Ana", "genero": "F"}"""))
				.andExpect(jsonPath("$.errors.jogador").value("é obrigatório"));

		assertThat(repository.findById(ficha.getId()).orElseThrow().getNameJ()).isEqualTo("Arthur");
	}

	@Test
	@DisplayName("PUT com chave desconhecida devolve 400 citando a chave")
	void putComChaveDesconhecida() throws Exception {
		SheetPlayer ficha = salvar();

		problema400(enviar("PUT", "/fichas/" + ficha.getId(), """
				{"jogador": "Arthur", "personagem": "Ana", "genero": "F", "STR": 50}"""))
				.andExpect(jsonPath("$.detail", containsString("STR")));
	}

	@Test
	@DisplayName("PUT com id diferente no corpo: vale o id da URL")
	void putComIdDiferenteNoCorpo() throws Exception {
		SheetPlayer alvo = salvar();
		SheetPlayer outra = salvar();

		enviar("PUT", "/fichas/" + alvo.getId(), """
				{"id": %d, "jogador": "Atualizado", "personagem": "Ana", "genero": "F"}""".formatted(outra.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(alvo.getId()));

		assertThat(repository.findById(alvo.getId()).orElseThrow().getNameJ()).isEqualTo("Atualizado");
		assertThat(repository.findById(outra.getId()).orElseThrow().getNameJ()).isEqualTo("Arthur");
		assertThat(repository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("POST com id no corpo: o id é ignorado e o banco gera um novo")
	void postComIdNoCorpo() throws Exception {
		enviar("POST", "/fichas", """
				{"id": 777777, "jogador": "Arthur", "personagem": "Ana", "genero": "F"}""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber());

		assertThat(repository.existsById(777_777L)).isFalse();
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("POST com id null no corpo (como o Angular envia) funciona")
	void postComIdNulo() throws Exception {
		enviar("POST", "/fichas", """
				{"id": null, "jogador": "Arthur", "personagem": "Ana", "genero": "F"}""")
				.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("Validação que falha não escreve o valor rejeitado (dado pessoal) no log")
	void valorRejeitadoNaoVaiParaOLog(CapturedOutput saida) throws Exception {
		String nomeLongo = "NomeSecretoDoJogador" + "x".repeat(250);

		problema400(enviar("POST", "/fichas", """
				{"jogador": "%s", "personagem": "Ana", "genero": "F"}""".formatted(nomeLongo)));

		assertThat(saida.getAll()).doesNotContain("NomeSecretoDoJogador");
	}

	@Test
	@DisplayName("GET com id não numérico devolve 400 em português")
	void idNaoNumerico() throws Exception {
		problema400(mockMvc.perform(get("/fichas/abc")))
				.andExpect(jsonPath("$.title").value("Parâmetro inválido"))
				.andExpect(jsonPath("$.detail").value("O parâmetro 'id' deve ser um número inteiro."));
	}

	@Test
	@DisplayName("POST com código de perícia inexistente em pericias (\"VOAR\") devolve 400 e nada é salvo")
	void postComChaveDePericiaInvalida() throws Exception {
		String corpo = """
				{"jogador": "Arthur", "personagem": "Ana", "genero": "F", "pericias": {"VOAR": 10}}""";

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.detail", containsString("pericias")));

		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("PUT com código de perícia inexistente em pericias devolve 400 e não altera a ficha")
	void putComChaveDePericiaInvalida() throws Exception {
		SheetPlayer ficha = salvar();

		problema400(enviar("PUT", "/fichas/" + ficha.getId(), """
				{"jogador": "Outro", "personagem": "Ana", "genero": "F", "pericias": {"ACROBACIA": 10, "atribute1": 10}}"""))
				.andExpect(jsonPath("$.detail", containsString("pericias")));

		assertThat(repository.findById(ficha.getId()).orElseThrow().getNameJ()).isEqualTo("Arthur");
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
