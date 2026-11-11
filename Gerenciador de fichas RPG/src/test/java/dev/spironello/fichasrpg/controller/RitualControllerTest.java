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

/** Rotas de /fichas/{id}/rituais: criar, editar e excluir; só existem em ficha de Ordem Paranormal. */
@DisplayName("API /fichas/{id}/rituais")
class RitualControllerTest extends IntegrationTestBase {

	private static final Long ID_INEXISTENTE = 999_999L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository fichas;

	@Autowired
	private RitualOrdemRepository rituais;

	@Autowired
	private MagiaDndRepository magias;

	@Autowired
	private ObjectMapper objectMapper;

	private FichaOrdem ordem;
	private Long ordemId;
	private Long dndId;

	@BeforeEach
	void prepararFichas() {
		magias.deleteAll();
		rituais.deleteAll();
		fichas.deleteAll();
		ordem = (FichaOrdem) fichas.save(FichasDeTeste.ordem("Lia"));
		FichaDnd dnd = (FichaDnd) fichas.save(FichasDeTeste.dnd("Bram"));
		ordemId = ordem.getId();
		dndId = dnd.getId();
	}

	private static Map<String, Object> corpo(String nome, Object circulo, Object elemento) {
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("nome", nome);
		corpo.put("circulo", circulo);
		corpo.put("elemento", elemento);
		return corpo;
	}

	private static Map<String, Object> corpo(String nome) {
		return corpo(nome, 1, "SANGUE");
	}

	private ResultActions criar(Long fichaId, Map<String, Object> corpo) throws Exception {
		return mockMvc.perform(post("/fichas/{id}/rituais", fichaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)));
	}

	private ResultActions editar(Long fichaId, Long ritualId, Map<String, Object> corpo) throws Exception {
		return mockMvc.perform(put("/fichas/{id}/rituais/{r}", fichaId, ritualId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)));
	}

	private Long ritualSalvo(String nome, int circulo) {
		return rituais.save(FichasDeTeste.ritualOrdem(ordem, nome, circulo)).getId();
	}

	// ---- criar ----

	@Test
	@DisplayName("POST dá 201 com Location exato e todos os campos, e o GET da ficha lista o ritual")
	void cria() throws Exception {
		Map<String, Object> corpo = corpo("Terceiro Olho", 2, "CONHECIMENTO");
		corpo.put("execucao", "Padrão");
		corpo.put("alcance", "Pessoal");
		corpo.put("alvo", "Você");
		corpo.put("duracao", "Cena");
		corpo.put("resistencia", "Vontade anula");
		corpo.put("descricao", "Revela o invisível.");

		var resultado = criar(ordemId, corpo)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nome").value("Terceiro Olho"))
				.andExpect(jsonPath("$.circulo").value(2))
				.andExpect(jsonPath("$.elemento").value("CONHECIMENTO"))
				.andExpect(jsonPath("$.execucao").value("Padrão"))
				.andExpect(jsonPath("$.alcance").value("Pessoal"))
				.andExpect(jsonPath("$.alvo").value("Você"))
				.andExpect(jsonPath("$.duracao").value("Cena"))
				.andExpect(jsonPath("$.resistencia").value("Vontade anula"))
				.andExpect(jsonPath("$.descricao").value("Revela o invisível."))
				.andReturn();
		long id = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
		assertEquals("/fichas/" + ordemId + "/rituais/" + id, resultado.getResponse().getHeader("Location"));

		mockMvc.perform(get("/fichas/{id}", ordemId))
				.andExpect(jsonPath("$.rituais.length()").value(1))
				.andExpect(jsonPath("$.rituais[0].id").value(id))
				.andExpect(jsonPath("$.rituais[0].nome").value("Terceiro Olho"));
	}

	@Test
	@DisplayName("só nome, círculo e elemento: os textos opcionais saem nulos")
	void soObrigatorios() throws Exception {
		criar(ordemId, corpo("Cicatrização", 1, "SANGUE"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.execucao").isEmpty())
				.andExpect(jsonPath("$.alcance").isEmpty())
				.andExpect(jsonPath("$.alvo").isEmpty())
				.andExpect(jsonPath("$.duracao").isEmpty())
				.andExpect(jsonPath("$.resistencia").isEmpty())
				.andExpect(jsonPath("$.descricao").isEmpty());
	}

	@Test
	@DisplayName("textos vazios ou só com espaços viram nulo, nos campos curtos e na descrição")
	void textosVaziosViramNulo() throws Exception {
		Map<String, Object> corpo = corpo("X");
		for (String campo : new String[] { "execucao", "alcance", "alvo", "duracao", "resistencia" }) {
			corpo.put(campo, "  ");
		}
		corpo.put("descricao", "");

		criar(ordemId, corpo)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.execucao").isEmpty())
				.andExpect(jsonPath("$.alcance").isEmpty())
				.andExpect(jsonPath("$.alvo").isEmpty())
				.andExpect(jsonPath("$.duracao").isEmpty())
				.andExpect(jsonPath("$.resistencia").isEmpty())
				.andExpect(jsonPath("$.descricao").isEmpty());
	}

	@Test
	@DisplayName("os cinco elementos e os quatro círculos são aceitos")
	void elementosECirculos() throws Exception {
		String[] elementos = { "SANGUE", "MORTE", "CONHECIMENTO", "ENERGIA", "MEDO" };
		for (int circulo = 1; circulo <= 4; circulo++) {
			criar(ordemId, corpo("X", circulo, elementos[circulo - 1])).andExpect(status().isCreated());
		}
		criar(ordemId, corpo("X", 1, elementos[4])).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("a lista volta por círculo e, no mesmo círculo, pela ordem de criação")
	void ordemDaLista() throws Exception {
		criar(ordemId, corpo("Terceiro A", 3, "MORTE")).andExpect(status().isCreated());
		criar(ordemId, corpo("Primeiro", 1, "MORTE")).andExpect(status().isCreated());
		criar(ordemId, corpo("Quarto", 4, "MORTE")).andExpect(status().isCreated());
		criar(ordemId, corpo("Terceiro B", 3, "MORTE")).andExpect(status().isCreated());

		mockMvc.perform(get("/fichas/{id}", ordemId))
				.andExpect(jsonPath("$.rituais.length()").value(4))
				.andExpect(jsonPath("$.rituais[0].nome").value("Primeiro"))
				.andExpect(jsonPath("$.rituais[1].nome").value("Terceiro A"))
				.andExpect(jsonPath("$.rituais[2].nome").value("Terceiro B"))
				.andExpect(jsonPath("$.rituais[3].nome").value("Quarto"));
	}

	// ---- erros ----

	@Test
	@DisplayName("nome vazio ou ausente dá 400 em errors.nome; 256 caracteres também e 255 passa")
	void nomeInvalido() throws Exception {
		criar(ordemId, corpo(""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nome").exists());
		Map<String, Object> semNome = corpo("X");
		semNome.remove("nome");
		criar(ordemId, semNome)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nome").value("é obrigatório"));
		criar(ordemId, corpo("a".repeat(256)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nome").exists());
		criar(ordemId, corpo("a".repeat(255))).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("círculo 0, 5 ou ausente dá 400 em errors.circulo")
	void circuloInvalido() throws Exception {
		criar(ordemId, corpo("X", 0, "SANGUE"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.circulo").exists());
		criar(ordemId, corpo("X", 5, "SANGUE"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.circulo").exists());
		Map<String, Object> semCirculo = corpo("X");
		semCirculo.remove("circulo");
		criar(ordemId, semCirculo)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.circulo").value("é obrigatório"));
	}

	@Test
	@DisplayName("elemento ausente dá 400 em errors.elemento; elemento que não existe dá 400")
	void elementoInvalido() throws Exception {
		Map<String, Object> semElemento = corpo("X");
		semElemento.remove("elemento");
		criar(ordemId, semElemento)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.elemento").value("é obrigatório"));
		criar(ordemId, corpo("X", 1, "FOGO")).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("campo curto com 256 caracteres dá 400 no próprio campo; 255 passa")
	void camposCurtosGrandes() throws Exception {
		for (String campo : new String[] { "execucao", "alcance", "alvo", "duracao", "resistencia" }) {
			Map<String, Object> grande = corpo("X");
			grande.put(campo, "a".repeat(256));
			criar(ordemId, grande)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errors." + campo).exists());
			Map<String, Object> limite = corpo("X");
			limite.put(campo, "a".repeat(255));
			criar(ordemId, limite).andExpect(status().isCreated());
		}
	}

	@Test
	@DisplayName("descrição de 4001 caracteres dá 400 em errors.descricao; 4000 passa")
	void descricaoGrande() throws Exception {
		Map<String, Object> grande = corpo("X");
		grande.put("descricao", "a".repeat(4001));
		criar(ordemId, grande)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.descricao").exists());
		Map<String, Object> limite = corpo("X");
		limite.put("descricao", "a".repeat(4000));
		criar(ordemId, limite).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("vários campos errados dão 400 com todas as chaves de uma vez")
	void variosErrosDeUmaVez() throws Exception {
		criar(ordemId, corpo("", 9, null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nome").exists())
				.andExpect(jsonPath("$.errors.circulo").exists())
				.andExpect(jsonPath("$.errors.elemento").exists());
	}

	@Test
	@DisplayName("chave desconhecida dá 400 e id no corpo é ignorado")
	void chaveDesconhecidaEId() throws Exception {
		Map<String, Object> comExtra = corpo("X");
		comExtra.put("extra", "y");
		criar(ordemId, comExtra)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("extra")));

		Map<String, Object> comId = corpo("Com id");
		comId.put("id", 12345);
		String resposta = criar(ordemId, comId)
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
	@DisplayName("ficha de D&D dá 400 em errors.lista nas três rotas, e nada é gravado")
	void fichaDeDnd() throws Exception {
		criar(dndId, corpo("X"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Dados inválidos"))
				.andExpect(jsonPath("$.errors.lista").value("não existe em D&D"));
		editar(dndId, 1L, corpo("X"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.lista").value("não existe em D&D"));
		mockMvc.perform(delete("/fichas/{id}/rituais/{r}", dndId, 1L))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.lista").value("não existe em D&D"));
		assertEquals(0, rituais.count());
	}

	@Test
	@DisplayName("corpo inválido numa ficha de D&D dá 400 nos campos, e não em lista (o Bean Validation roda antes)")
	void corpoInvalidoEmDnd() throws Exception {
		criar(dndId, corpo("", 9, "SANGUE"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nome").exists())
				.andExpect(jsonPath("$.errors.lista").doesNotExist());
	}

	// ---- editar ----

	@Test
	@DisplayName("PUT muda os campos do ritual e o GET da ficha mostra a mudança")
	void edita() throws Exception {
		Long id = ritualSalvo("Antes", 1);
		Map<String, Object> corpo = corpo("Depois", 3, "ENERGIA");
		corpo.put("resistencia", "Fortitude parcial");

		editar(ordemId, id, corpo)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.nome").value("Depois"))
				.andExpect(jsonPath("$.circulo").value(3))
				.andExpect(jsonPath("$.elemento").value("ENERGIA"))
				.andExpect(jsonPath("$.resistencia").value("Fortitude parcial"));

		mockMvc.perform(get("/fichas/{id}", ordemId))
				.andExpect(jsonPath("$.rituais[0].nome").value("Depois"))
				.andExpect(jsonPath("$.rituais[0].elemento").value("ENERGIA"));
	}

	@Test
	@DisplayName("PUT com dado inválido dá 400 e o ritual não muda")
	void editaComDadoInvalido() throws Exception {
		Long id = ritualSalvo("Antes", 1);

		editar(ordemId, id, corpo("Depois", 5, "SANGUE"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.circulo").exists());

		assertEquals("Antes", rituais.findById(id).orElseThrow().getNome());
	}

	@Test
	@DisplayName("PUT de ritual de outra ficha dá 404 Item não encontrado e ele fica intacto")
	void editarDeOutraFicha() throws Exception {
		Long id = ritualSalvo("Do outro", 1);
		FichaOrdem segunda = (FichaOrdem) fichas.save(FichasDeTeste.ordem("Outra"));

		editar(segunda.getId(), id, corpo("Roubado"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"))
				.andExpect(jsonPath("$.detail").value("Não existe ritual com id " + id + " nesta ficha."));

		assertEquals("Do outro", rituais.findById(id).orElseThrow().getNome());
	}

	@Test
	@DisplayName("PUT de ritual inexistente dá 404 Item; ficha inexistente dá 404 Ficha")
	void editarInexistente() throws Exception {
		editar(ordemId, ID_INEXISTENTE, corpo("X"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"));
		editar(ID_INEXISTENTE, 1L, corpo("X"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}

	// ---- excluir ----

	@Test
	@DisplayName("DELETE dá 204 e o ritual some do GET da ficha")
	void exclui() throws Exception {
		Long id = ritualSalvo("Some", 2);

		mockMvc.perform(delete("/fichas/{id}/rituais/{r}", ordemId, id)).andExpect(status().isNoContent());

		mockMvc.perform(get("/fichas/{id}", ordemId)).andExpect(jsonPath("$.rituais.length()").value(0));
	}

	@Test
	@DisplayName("DELETE de ritual de outra ficha dá 404 Item não encontrado e ele continua")
	void excluirDeOutraFicha() throws Exception {
		Long id = ritualSalvo("Do outro", 2);
		FichaOrdem segunda = (FichaOrdem) fichas.save(FichasDeTeste.ordem("Outra"));

		mockMvc.perform(delete("/fichas/{id}/rituais/{r}", segunda.getId(), id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"));

		assertTrue(rituais.findById(id).isPresent());
	}

	@Test
	@DisplayName("DELETE em ficha inexistente dá 404 Ficha não encontrada")
	void excluirEmFichaInexistente() throws Exception {
		mockMvc.perform(delete("/fichas/{id}/rituais/{r}", ID_INEXISTENTE, 1L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}

	// ---- cascata ----

	@Test
	@DisplayName("DELETE da ficha com rituais dá 204 e os rituais somem do banco")
	void excluirFichaApagaOsRituais() throws Exception {
		ritualSalvo("Um", 1);
		ritualSalvo("Dois", 2);

		mockMvc.perform(delete("/fichas/{id}", ordemId)).andExpect(status().isNoContent());

		assertEquals(0, rituais.count());
	}
}
