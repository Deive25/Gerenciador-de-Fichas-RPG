package dev.spironello.fichasrpg.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

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
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.repository.MagiaDndRepository;
import dev.spironello.fichasrpg.repository.RitualOrdemRepository;
import tools.jackson.databind.ObjectMapper;

/** Rotas de /fichas/{id}/magias: criar, editar e excluir; só existem em ficha de D&D. */
@DisplayName("API /fichas/{id}/magias")
class MagiaControllerTest extends IntegrationTestBase {

	private static final Long ID_INEXISTENTE = 999_999L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository fichas;

	@Autowired
	private MagiaDndRepository magias;

	@Autowired
	private RitualOrdemRepository rituais;

	@Autowired
	private ObjectMapper objectMapper;

	private FichaDnd dnd;
	private Long ordemId;
	private Long dndId;

	@BeforeEach
	void prepararFichas() {
		magias.deleteAll();
		rituais.deleteAll();
		fichas.deleteAll();
		dnd = (FichaDnd) fichas.save(FichasDeTeste.dnd("Bram"));
		FichaOrdem ordem = (FichaOrdem) fichas.save(FichasDeTeste.ordem("Lia"));
		dndId = dnd.getId();
		ordemId = ordem.getId();
	}

	private static Map<String, Object> corpo(String nome, Object nivel) {
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("nome", nome);
		corpo.put("nivel", nivel);
		return corpo;
	}

	private static Map<String, Object> corpo(String nome) {
		return corpo(nome, 1);
	}

	private ResultActions criar(Long fichaId, Map<String, Object> corpo) throws Exception {
		return mockMvc.perform(post("/fichas/{id}/magias", fichaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)));
	}

	private ResultActions editar(Long fichaId, Long magiaId, Map<String, Object> corpo) throws Exception {
		return mockMvc.perform(put("/fichas/{id}/magias/{m}", fichaId, magiaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)));
	}

	private Long magiaSalva(String nome, int nivel) {
		return magias.save(FichasDeTeste.magiaDnd(dnd, nome, nivel)).getId();
	}

	// ---- criar ----

	@Test
	@DisplayName("POST dá 201 com Location exato e todos os campos, e o GET da ficha lista a magia")
	void cria() throws Exception {
		Map<String, Object> corpo = corpo("Bola de Fogo", 3);
		corpo.put("escola", "Evocação");
		corpo.put("tempo", "1 ação");
		corpo.put("alcance", "45 m");
		corpo.put("componentes", "V, S, M");
		corpo.put("duracao", "Instantânea");
		corpo.put("concentracao", true);
		corpo.put("ritual", true);
		corpo.put("preparada", true);
		corpo.put("descricao", "Explosão de chamas.");

		var resultado = criar(dndId, corpo)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nome").value("Bola de Fogo"))
				.andExpect(jsonPath("$.nivel").value(3))
				.andExpect(jsonPath("$.escola").value("Evocação"))
				.andExpect(jsonPath("$.tempo").value("1 ação"))
				.andExpect(jsonPath("$.alcance").value("45 m"))
				.andExpect(jsonPath("$.componentes").value("V, S, M"))
				.andExpect(jsonPath("$.duracao").value("Instantânea"))
				.andExpect(jsonPath("$.concentracao").value(true))
				.andExpect(jsonPath("$.ritual").value(true))
				.andExpect(jsonPath("$.preparada").value(true))
				.andExpect(jsonPath("$.descricao").value("Explosão de chamas."))
				.andReturn();
		long id = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
		assertEquals("/fichas/" + dndId + "/magias/" + id, resultado.getResponse().getHeader("Location"));

		mockMvc.perform(get("/fichas/{id}", dndId))
				.andExpect(jsonPath("$.magias.length()").value(1))
				.andExpect(jsonPath("$.magias[0].id").value(id))
				.andExpect(jsonPath("$.magias[0].nome").value("Bola de Fogo"));
	}

	@Test
	@DisplayName("só nome e nível: marcadores nulos ou ausentes viram false, textos opcionais saem nulos")
	void soObrigatorios() throws Exception {
		criar(dndId, corpo("Luz", 0))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nivel").value(0))
				.andExpect(jsonPath("$.concentracao").value(false))
				.andExpect(jsonPath("$.ritual").value(false))
				.andExpect(jsonPath("$.preparada").value(false))
				.andExpect(jsonPath("$.escola").isEmpty())
				.andExpect(jsonPath("$.descricao").isEmpty());

		Map<String, Object> comNulos = corpo("Luz", 0);
		comNulos.put("concentracao", null);
		comNulos.put("ritual", null);
		comNulos.put("preparada", null);
		criar(dndId, comNulos)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.concentracao").value(false))
				.andExpect(jsonPath("$.ritual").value(false))
				.andExpect(jsonPath("$.preparada").value(false));
	}

	@Test
	@DisplayName("textos vazios ou só com espaços viram nulo, nos campos curtos e na descrição")
	void textosVaziosViramNulo() throws Exception {
		Map<String, Object> corpo = corpo("Luz", 0);
		for (String campo : new String[] { "escola", "tempo", "alcance", "componentes", "duracao" }) {
			corpo.put(campo, "  ");
		}
		corpo.put("descricao", "");

		criar(dndId, corpo)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.escola").isEmpty())
				.andExpect(jsonPath("$.tempo").isEmpty())
				.andExpect(jsonPath("$.alcance").isEmpty())
				.andExpect(jsonPath("$.componentes").isEmpty())
				.andExpect(jsonPath("$.duracao").isEmpty())
				.andExpect(jsonPath("$.descricao").isEmpty());
	}

	@Test
	@DisplayName("a lista volta por nível (truque primeiro) e, no mesmo nível, pela ordem de criação")
	void ordemDaLista() throws Exception {
		criar(dndId, corpo("Terceira A", 3)).andExpect(status().isCreated());
		criar(dndId, corpo("Truque", 0)).andExpect(status().isCreated());
		criar(dndId, corpo("Primeira", 1)).andExpect(status().isCreated());
		criar(dndId, corpo("Terceira B", 3)).andExpect(status().isCreated());

		mockMvc.perform(get("/fichas/{id}", dndId))
				.andExpect(jsonPath("$.magias.length()").value(4))
				.andExpect(jsonPath("$.magias[0].nome").value("Truque"))
				.andExpect(jsonPath("$.magias[1].nome").value("Primeira"))
				.andExpect(jsonPath("$.magias[2].nome").value("Terceira A"))
				.andExpect(jsonPath("$.magias[3].nome").value("Terceira B"));
	}

	// ---- erros ----

	@Test
	@DisplayName("nome vazio ou ausente dá 400 em errors.nome; 256 caracteres também e 255 passa")
	void nomeInvalido() throws Exception {
		criar(dndId, corpo(""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nome").exists());
		Map<String, Object> semNome = corpo("X");
		semNome.remove("nome");
		criar(dndId, semNome)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nome").value("é obrigatório"));
		criar(dndId, corpo("a".repeat(256)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nome").exists());
		criar(dndId, corpo("a".repeat(255))).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("nível 10, -1 ou ausente dá 400 em errors.nivel; 0 e 9 passam")
	void nivelInvalido() throws Exception {
		criar(dndId, corpo("X", 10))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nivel").exists());
		criar(dndId, corpo("X", -1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nivel").exists());
		Map<String, Object> semNivel = corpo("X");
		semNivel.remove("nivel");
		criar(dndId, semNivel)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nivel").value("é obrigatório"));
		criar(dndId, corpo("X", 0)).andExpect(status().isCreated());
		criar(dndId, corpo("X", 9)).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("campo curto com 256 caracteres dá 400 no próprio campo; 255 passa")
	void camposCurtosGrandes() throws Exception {
		for (String campo : new String[] { "escola", "tempo", "alcance", "componentes", "duracao" }) {
			Map<String, Object> grande = corpo("X");
			grande.put(campo, "a".repeat(256));
			criar(dndId, grande)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errors." + campo).exists());
			Map<String, Object> limite = corpo("X");
			limite.put(campo, "a".repeat(255));
			criar(dndId, limite).andExpect(status().isCreated());
		}
	}

	@Test
	@DisplayName("descrição de 4001 caracteres dá 400 em errors.descricao; 4000 passa")
	void descricaoGrande() throws Exception {
		Map<String, Object> grande = corpo("X");
		grande.put("descricao", "a".repeat(4001));
		criar(dndId, grande)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.descricao").exists());
		Map<String, Object> limite = corpo("X");
		limite.put("descricao", "a".repeat(4000));
		criar(dndId, limite).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("vários campos errados dão 400 com todas as chaves de uma vez")
	void variosErrosDeUmaVez() throws Exception {
		criar(dndId, corpo("", 10))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nome").exists())
				.andExpect(jsonPath("$.errors.nivel").exists());
	}

	@Test
	@DisplayName("chave desconhecida dá 400 e id no corpo é ignorado")
	void chaveDesconhecidaEId() throws Exception {
		Map<String, Object> comExtra = corpo("X");
		comExtra.put("extra", "y");
		criar(dndId, comExtra)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("extra")));

		Map<String, Object> comId = corpo("Com id");
		comId.put("id", 12345);
		String resposta = criar(dndId, comId)
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		assertNotEquals(12345L, objectMapper.readTree(resposta).get("id").asLong());
	}

	@Test
	@DisplayName("POST em ficha inexistente dá 404 Ficha não encontrada")
	void criarEmFichaInexistente() throws Exception {
		criar(ID_INEXISTENTE, corpo("X"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}

	@Test
	@DisplayName("ficha de Ordem dá 400 em errors.lista nas três rotas, e nada é gravado")
	void fichaDeOrdem() throws Exception {
		criar(ordemId, corpo("X"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Dados inválidos"))
				.andExpect(jsonPath("$.errors.lista").value("não existe em Ordem Paranormal"));
		editar(ordemId, 1L, corpo("X"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.lista").value("não existe em Ordem Paranormal"));
		mockMvc.perform(delete("/fichas/{id}/magias/{m}", ordemId, 1L))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.lista").value("não existe em Ordem Paranormal"));
		assertEquals(0, magias.count());
	}

	@Test
	@DisplayName("corpo inválido numa ficha de Ordem dá 400 nos campos, e não em lista (o Bean Validation roda antes)")
	void corpoInvalidoEmOrdem() throws Exception {
		criar(ordemId, corpo("", 10))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nome").exists())
				.andExpect(jsonPath("$.errors.lista").doesNotExist());
	}

	// ---- editar ----

	@Test
	@DisplayName("PUT muda os campos da magia e o GET da ficha mostra a mudança")
	void edita() throws Exception {
		Long id = magiaSalva("Antes", 1);
		Map<String, Object> corpo = corpo("Depois", 5);
		corpo.put("escola", "Abjuração");
		corpo.put("preparada", true);

		editar(dndId, id, corpo)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.nome").value("Depois"))
				.andExpect(jsonPath("$.nivel").value(5))
				.andExpect(jsonPath("$.escola").value("Abjuração"))
				.andExpect(jsonPath("$.preparada").value(true));

		mockMvc.perform(get("/fichas/{id}", dndId))
				.andExpect(jsonPath("$.magias[0].nome").value("Depois"))
				.andExpect(jsonPath("$.magias[0].nivel").value(5));
	}

	@Test
	@DisplayName("PUT substitui a magia inteira: campo que não veio volta a nulo ou false")
	void editaSubstituiTudo() throws Exception {
		Long id = magiaSalva("Antes", 1);
		Map<String, Object> cheia = corpo("Antes", 1);
		cheia.put("escola", "Evocação");
		cheia.put("concentracao", true);
		editar(dndId, id, cheia).andExpect(status().isOk());

		editar(dndId, id, corpo("Antes", 1))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.escola").isEmpty())
				.andExpect(jsonPath("$.concentracao").value(false));
	}

	@Test
	@DisplayName("PUT com dado inválido dá 400 e a magia não muda")
	void editaComDadoInvalido() throws Exception {
		Long id = magiaSalva("Antes", 1);

		editar(dndId, id, corpo("Depois", 10))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nivel").exists());

		assertEquals("Antes", magias.findById(id).orElseThrow().getNome());
	}

	@Test
	@DisplayName("PUT de magia de outra ficha dá 404 Item não encontrado e ela fica intacta")
	void editarDeOutraFicha() throws Exception {
		Long id = magiaSalva("Da outra", 1);
		FichaDnd segunda = (FichaDnd) fichas.save(FichasDeTeste.dnd("Outra"));

		editar(segunda.getId(), id, corpo("Roubada"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"))
				.andExpect(jsonPath("$.detail").value("Não existe magia com id " + id + " nesta ficha."));

		assertEquals("Da outra", magias.findById(id).orElseThrow().getNome());
	}

	@Test
	@DisplayName("PUT de magia inexistente dá 404 Item; ficha inexistente dá 404 Ficha")
	void editarInexistente() throws Exception {
		editar(dndId, ID_INEXISTENTE, corpo("X"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"));
		editar(ID_INEXISTENTE, 1L, corpo("X"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}

	// ---- excluir ----

	@Test
	@DisplayName("DELETE dá 204 e a magia some do GET da ficha")
	void exclui() throws Exception {
		Long id = magiaSalva("Some", 2);

		mockMvc.perform(delete("/fichas/{id}/magias/{m}", dndId, id)).andExpect(status().isNoContent());

		mockMvc.perform(get("/fichas/{id}", dndId)).andExpect(jsonPath("$.magias.length()").value(0));
	}

	@Test
	@DisplayName("DELETE de magia de outra ficha dá 404 Item não encontrado e ela continua")
	void excluirDeOutraFicha() throws Exception {
		Long id = magiaSalva("Da outra", 2);
		FichaDnd segunda = (FichaDnd) fichas.save(FichasDeTeste.dnd("Outra"));

		mockMvc.perform(delete("/fichas/{id}/magias/{m}", segunda.getId(), id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"));

		assertTrue(magias.findById(id).isPresent());
	}

	@Test
	@DisplayName("DELETE em ficha inexistente dá 404 Ficha não encontrada")
	void excluirEmFichaInexistente() throws Exception {
		mockMvc.perform(delete("/fichas/{id}/magias/{m}", ID_INEXISTENTE, 1L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}

	// ---- cascata ----

	@Test
	@DisplayName("DELETE da ficha com magias dá 204 e as magias somem do banco")
	void excluirFichaApagaAsMagias() throws Exception {
		magiaSalva("Luz", 0);
		magiaSalva("Bola de Fogo", 3);

		mockMvc.perform(delete("/fichas/{id}", dndId)).andExpect(status().isNoContent());

		assertEquals(0, magias.count());
	}
}
