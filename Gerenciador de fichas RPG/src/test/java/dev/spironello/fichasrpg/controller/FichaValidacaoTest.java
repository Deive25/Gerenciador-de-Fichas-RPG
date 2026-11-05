package dev.spironello.fichasrpg.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.repository.FichaRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * Entrada inválida e regras de entrada da API (ADR 0007 e 0008): JSON estrito, limites de tamanho,
 * tipos errados, tratamento do id no corpo e as regras de Ordem (atributos 0 a 5, NEX, grau x NEX, sistema).
 */
@DisplayName("API /fichas (validação de entrada e JSON estrito)")
// O devtools (usado em spring-boot:run) liga este log por padrão e o Spring então registra, em WARN,
// a exceção de validação com o valor rejeitado. Ligamos aqui para reproduzir esse cenário.
@TestPropertySource(properties = "spring.mvc.log-resolved-exception=true")
@ExtendWith(OutputCaptureExtension.class)
class FichaValidacaoTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository repository;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void limparBanco() {
		repository.deleteAll();
	}

	// ---------- comportamentos que vieram do d100 ----------

	@Test
	@DisplayName("POST com chave desconhecida (\"STR\") devolve 400 citando a chave")
	void postComChaveDesconhecida() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("STR", 50);

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.detail", containsString("STR")));

		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("POST com jogador de 256 caracteres devolve 400 e nada é salvo")
	void postComNomeLongoDemais() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("jogador", "a".repeat(256));

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.errors.jogador").value("deve ter no máximo 255 caracteres"));

		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("POST com jogador de 255 caracteres é aceito (limite da coluna varchar(255))")
	void postComNomeNoLimite() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("jogador", "a".repeat(255));

		enviar("POST", "/fichas", corpo).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("POST com origem de 256 caracteres devolve 400")
	void postComOrigemLongaDemais() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("origem", "a".repeat(256));

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.errors.origem").value("deve ter no máximo 255 caracteres"));
	}

	@Test
	@DisplayName("POST com tipo errado (\"idade\": \"abc\") devolve 400 citando o campo")
	void postComTipoErrado() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("idade", "abc");

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.detail", containsString("idade")));
	}

	@Test
	@DisplayName("POST com JSON malformado devolve 400 ProblemDetail")
	void postComJsonMalformado() throws Exception {
		problema400(enviarTexto("POST", "/fichas", "{jogador: "));
	}

	@Test
	@DisplayName("PUT inválido devolve 400 com os erros por campo e não altera a ficha")
	void putInvalido() throws Exception {
		FichaOrdem ficha = salvar();
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("jogador", "  ");

		problema400(enviar("PUT", "/fichas/" + ficha.getId(), corpo))
				.andExpect(jsonPath("$.errors.jogador").value("é obrigatório"));

		assertThat(repository.findById(ficha.getId()).orElseThrow().getJogador()).isEqualTo("Ana");
	}

	@Test
	@DisplayName("PUT com chave desconhecida devolve 400 citando a chave")
	void putComChaveDesconhecida() throws Exception {
		FichaOrdem ficha = salvar();
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("STR", 50);

		problema400(enviar("PUT", "/fichas/" + ficha.getId(), corpo))
				.andExpect(jsonPath("$.detail", containsString("STR")));
	}

	@Test
	@DisplayName("PUT com id diferente no corpo: vale o id da URL")
	void putComIdDiferenteNoCorpo() throws Exception {
		FichaOrdem alvo = salvar();
		FichaOrdem outra = salvar();
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("id", outra.getId());
		corpo.put("jogador", "Atualizado");

		enviar("PUT", "/fichas/" + alvo.getId(), corpo)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(alvo.getId()));

		assertThat(repository.findById(alvo.getId()).orElseThrow().getJogador()).isEqualTo("Atualizado");
		assertThat(repository.findById(outra.getId()).orElseThrow().getJogador()).isEqualTo("Ana");
		assertThat(repository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("POST com id no corpo: o id é ignorado e o banco gera um novo")
	void postComIdNoCorpo() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("id", 777777);

		enviar("POST", "/fichas", corpo)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber());

		assertThat(repository.existsById(777_777L)).isFalse();
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("POST com id null no corpo (como o Angular envia) funciona")
	void postComIdNulo() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("id", null);

		enviar("POST", "/fichas", corpo).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("Validação que falha não escreve o valor rejeitado (dado pessoal) no log")
	void valorRejeitadoNaoVaiParaOLog(CapturedOutput saida) throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("jogador", "NomeSecretoDoJogador" + "x".repeat(250));

		problema400(enviar("POST", "/fichas", corpo));

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
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("pericias", Map.of("VOAR", "TREINADO"));

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.detail", containsString("pericias")));

		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("PUT com código de perícia inexistente em pericias devolve 400 e não altera a ficha")
	void putComChaveDePericiaInvalida() throws Exception {
		FichaOrdem ficha = salvar();
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("jogador", "Outro");
		corpo.put("pericias", Map.of("ACROBACIA", "TREINADO", "atribute1", "TREINADO"));

		problema400(enviar("PUT", "/fichas/" + ficha.getId(), corpo))
				.andExpect(jsonPath("$.detail", containsString("pericias")));

		assertThat(repository.findById(ficha.getId()).orElseThrow().getJogador()).isEqualTo("Ana");
	}

	@Test
	@DisplayName("POST com grau de perícia inexistente (\"MESTRE\") devolve 400")
	void postComGrauInvalido() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("pericias", Map.of("ACROBACIA", "MESTRE"));

		problema400(enviar("POST", "/fichas", corpo));

		assertThat(repository.count()).isZero();
	}

	// ---------- regras de Ordem ----------

	@Test
	@DisplayName("atributo 6 devolve 400 no campo atributos[AGI]")
	void atributoSeis() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("atributos", Map.of("AGI", 6, "FOR", 1, "INT", 1, "PRE", 1, "VIG", 1));

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.errors['atributos[AGI]']").value("deve estar entre 0 e 5"));

		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("atributo negativo devolve 400 no campo atributos[FOR]")
	void atributoNegativo() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("atributos", Map.of("AGI", 1, "FOR", -1, "INT", 1, "PRE", 1, "VIG", 1));

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.errors['atributos[FOR]']").value("deve estar entre 0 e 5"));
	}

	@Test
	@DisplayName("atributo 0 é aceito")
	void atributoZero() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("atributos", Map.of("AGI", 0, "FOR", 1, "INT", 1, "PRE", 1, "VIG", 5));

		enviar("POST", "/fichas", corpo).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("faltar o atributo VIG devolve 400 no campo atributos.VIG")
	void faltaVigor() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("atributos", Map.of("AGI", 1, "FOR", 1, "INT", 1, "PRE", 1));

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.errors['atributos.VIG']").value("é obrigatório"));

		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("atributo com valor null devolve 400 no campo atributos[AGI]")
	void atributoNulo() throws Exception {
		problema400(enviarTexto("POST", "/fichas", """
				{"sistema": "ORDEM_PARANORMAL", "jogador": "Arthur", "personagem": "Ana", "genero": "F", "nex": 5,
				 "atributos": {"AGI": null, "FOR": 1, "INT": 1, "PRE": 1, "VIG": 1}}"""))
				.andExpect(jsonPath("$.errors['atributos[AGI]']").value("é obrigatório"));
	}

	@Test
	@DisplayName("NEX 4 devolve 400 no campo nex")
	void nexAbaixoDoMinimo() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("nex", 4);

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.errors.nex").value("deve estar entre 5 e 99"));
	}

	@Test
	@DisplayName("NEX 100 devolve 400 e NEX 99 é aceito")
	void nexAcimaDoMaximo() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("nex", 100);
		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.errors.nex").value("deve estar entre 5 e 99"));

		corpo.put("nex", 99);
		enviar("POST", "/fichas", corpo).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("VETERANO com NEX 30 devolve 400 no campo pericias.INVESTIGACAO e nada é salvo")
	void veteranoComNexBaixo() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("nex", 30);
		corpo.put("pericias", Map.of("INVESTIGACAO", "VETERANO"));

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.errors['pericias.INVESTIGACAO']").value("Veterano exige NEX 35% ou mais"));

		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("VETERANO com NEX 35 e EXPERT com NEX 70 são aceitos; EXPERT com NEX 69 não")
	void limitesDeGrauENex() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("nex", 35);
		corpo.put("pericias", Map.of("INVESTIGACAO", "VETERANO"));
		enviar("POST", "/fichas", corpo).andExpect(status().isCreated());

		corpo.put("nex", 70);
		corpo.put("pericias", Map.of("INVESTIGACAO", "EXPERT"));
		enviar("POST", "/fichas", corpo).andExpect(status().isCreated());

		corpo.put("nex", 69);
		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.errors['pericias.INVESTIGACAO']").value("Expert exige NEX 70% ou mais"));
	}

	@Test
	@DisplayName("PUT que baixa o NEX de uma ficha com Veterano devolve 400 em pericias.X e não altera a ficha")
	void putQueBaixaONexComVeterano() throws Exception {
		Map<String, Object> criacao = FichasDeTeste.corpoOrdem();
		criacao.put("nex", 50);
		criacao.put("pericias", Map.of("ATLETISMO", "VETERANO"));
		String resposta = enviar("POST", "/fichas", criacao).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Number id = JsonPath.read(resposta, "$.id");

		Map<String, Object> baixa = FichasDeTeste.corpoOrdem();
		baixa.put("nex", 30);
		baixa.put("pericias", Map.of("ATLETISMO", "VETERANO"));
		problema400(enviar("PUT", "/fichas/" + id, baixa))
				.andExpect(jsonPath("$.errors['pericias.ATLETISMO']").value("Veterano exige NEX 35% ou mais"));

		mockMvc.perform(get("/fichas/{id}", id.longValue()))
				.andExpect(jsonPath("$.nex").value(50))
				.andExpect(jsonPath("$.pericias.ATLETISMO").value("VETERANO"));
	}

	@Test
	@DisplayName("com NEX inválido e Veterano juntos, só o erro do Bean Validation aparece (a regra cruzada só roda com NEX válido)")
	void beanValidationVemPrimeiro() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("nex", 4);
		corpo.put("pericias", Map.of("INVESTIGACAO", "VETERANO"));

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.errors.nex").exists())
				.andExpect(jsonPath("$.errors['pericias.INVESTIGACAO']").doesNotExist());
	}

	@Test
	@DisplayName("sistema DND devolve 400 (subtipo desconhecido) e nada é salvo")
	void sistemaDesconhecido() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("sistema", "DND");

		problema400(enviar("POST", "/fichas", corpo));

		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("corpo sem sistema devolve 400 e nada é salvo")
	void semSistema() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.remove("sistema");

		problema400(enviar("POST", "/fichas", corpo));

		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("PUT sem sistema devolve 400")
	void putSemSistema() throws Exception {
		FichaOrdem ficha = salvar();
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.remove("sistema");

		problema400(enviar("PUT", "/fichas/" + ficha.getId(), corpo));
	}

	// ---------- apoio ----------

	private ResultActions enviar(String metodo, String url, Map<String, Object> corpo) throws Exception {
		return enviarTexto(metodo, url, objectMapper.writeValueAsString(corpo));
	}

	private ResultActions enviarTexto(String metodo, String url, String corpo) throws Exception {
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

	private FichaOrdem salvar() {
		FichaOrdem ficha = FichasDeTeste.ordem("Ana");
		ficha.setJogador("Ana");
		return repository.save(ficha);
	}
}

