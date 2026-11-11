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

import java.math.BigDecimal;
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
import dev.spironello.fichasrpg.repository.ItemDndRepository;
import dev.spironello.fichasrpg.repository.ItemOrdemRepository;
import tools.jackson.databind.ObjectMapper;

/** Rotas de /fichas/{id}/itens: criar, editar e excluir, com os campos conferidos pelo sistema da ficha. */
@DisplayName("API /fichas/{id}/itens")
class ItemControllerTest extends IntegrationTestBase {

	private static final Long ID_INEXISTENTE = 999_999L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository fichas;

	@Autowired
	private ItemOrdemRepository itensOrdem;

	@Autowired
	private ItemDndRepository itensDnd;

	@Autowired
	private ObjectMapper objectMapper;

	private FichaOrdem ordem;
	private FichaDnd dnd;
	private Long ordemId;
	private Long dndId;

	@BeforeEach
	void prepararFichas() {
		itensOrdem.deleteAll();
		itensDnd.deleteAll();
		fichas.deleteAll();
		ordem = (FichaOrdem) fichas.save(FichasDeTeste.ordem("Lia"));
		dnd = (FichaDnd) fichas.save(FichasDeTeste.dnd("Bram"));
		ordemId = ordem.getId();
		dndId = dnd.getId();
	}

	private static Map<String, Object> corpoOrdem(String nome, Object categoria, Object espacos, Object quantidade) {
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("nome", nome);
		corpo.put("categoria", categoria);
		corpo.put("espacos", espacos);
		corpo.put("quantidade", quantidade);
		return corpo;
	}

	private static Map<String, Object> corpoOrdem(String nome) {
		return corpoOrdem(nome, 0, 1, 1);
	}

	private static Map<String, Object> corpoDnd(String nome, Object peso) {
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("nome", nome);
		corpo.put("quantidade", 1);
		corpo.put("peso", peso);
		return corpo;
	}

	private static Map<String, Object> corpoDnd(String nome) {
		return corpoDnd(nome, 1);
	}

	private ResultActions criar(Long fichaId, Map<String, Object> corpo) throws Exception {
		return mockMvc.perform(post("/fichas/{id}/itens", fichaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)));
	}

	private ResultActions editar(Long fichaId, Long itemId, Map<String, Object> corpo) throws Exception {
		return mockMvc.perform(put("/fichas/{id}/itens/{i}", fichaId, itemId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)));
	}

	private Long itemOrdemSalvo(String nome) {
		return itensOrdem.save(FichasDeTeste.itemOrdem(ordem, nome)).getId();
	}

	private Long itemDndSalvo(String nome) {
		return itensDnd.save(FichasDeTeste.itemDnd(dnd, nome)).getId();
	}

	// ---- criar ----

	@Test
	@DisplayName("POST em ficha de Ordem dá 201 com Location exato, e o GET da ficha lista o item")
	void criaEmOrdem() throws Exception {
		Map<String, Object> corpo = corpoOrdem("Lanterna", 0, 1, 2);

		var resultado = criar(ordemId, corpo)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nome").value("Lanterna"))
				.andExpect(jsonPath("$.categoria").value(0))
				.andExpect(jsonPath("$.espacos").value(1))
				.andExpect(jsonPath("$.quantidade").value(2))
				.andReturn();
		long id = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
		assertEquals("/fichas/" + ordemId + "/itens/" + id, resultado.getResponse().getHeader("Location"));

		mockMvc.perform(get("/fichas/{id}", ordemId))
				.andExpect(jsonPath("$.itens.length()").value(1))
				.andExpect(jsonPath("$.itens[0].id").value(id))
				.andExpect(jsonPath("$.itens[0].nome").value("Lanterna"));
	}

	@Test
	@DisplayName("POST em ficha de D&D dá 201 com Location, peso 4.5 e equipado verdadeiro")
	void criaEmDnd() throws Exception {
		Map<String, Object> corpo = corpoDnd("Corda", 4.5);
		corpo.put("equipado", true);

		var resultado = criar(dndId, corpo)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nome").value("Corda"))
				.andExpect(jsonPath("$.quantidade").value(1))
				.andExpect(jsonPath("$.peso").value(4.5))
				.andExpect(jsonPath("$.equipado").value(true))
				.andReturn();
		long id = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
		assertEquals("/fichas/" + dndId + "/itens/" + id, resultado.getResponse().getHeader("Location"));

		mockMvc.perform(get("/fichas/{id}", dndId))
				.andExpect(jsonPath("$.itens.length()").value(1))
				.andExpect(jsonPath("$.itens[0].id").value(id))
				.andExpect(jsonPath("$.itens[0].equipado").value(true));
	}

	@Test
	@DisplayName("D&D sem equipado grava equipado falso")
	void equipadoAusenteEFalso() throws Exception {
		criar(dndId, corpoDnd("Adaga"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.equipado").value(false));
	}

	@Test
	@DisplayName("zero é válido: categoria 0 e espaços 0 em Ordem, peso 0 em D&D")
	void zeroEValido() throws Exception {
		criar(ordemId, corpoOrdem("Pena", 0, 0, 1))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.categoria").value(0))
				.andExpect(jsonPath("$.espacos").value(0));
		criar(dndId, corpoDnd("Pena", 0))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.peso").value(0));
	}

	@Test
	@DisplayName("três itens criados voltam na ordem de criação")
	void ordemDeCriacao() throws Exception {
		criar(ordemId, corpoOrdem("Zeta")).andExpect(status().isCreated());
		criar(ordemId, corpoOrdem("Alfa")).andExpect(status().isCreated());
		criar(ordemId, corpoOrdem("Mu")).andExpect(status().isCreated());

		mockMvc.perform(get("/fichas/{id}", ordemId))
				.andExpect(jsonPath("$.itens.length()").value(3))
				.andExpect(jsonPath("$.itens[0].nome").value("Zeta"))
				.andExpect(jsonPath("$.itens[1].nome").value("Alfa"))
				.andExpect(jsonPath("$.itens[2].nome").value("Mu"));
	}

	// ---- erros comuns ----

	@Test
	@DisplayName("nome vazio dá 400 em errors.nome, 256 caracteres também e 255 passa")
	void nomeInvalido() throws Exception {
		criar(ordemId, corpoOrdem(""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nome").exists());
		criar(ordemId, corpoOrdem("a".repeat(256)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nome").exists());
		criar(ordemId, corpoOrdem("a".repeat(255))).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("quantidade 0 ou ausente dá 400 em errors.quantidade")
	void quantidadeInvalida() throws Exception {
		criar(ordemId, corpoOrdem("X", 0, 1, 0))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.quantidade").exists());
		Map<String, Object> semQuantidade = corpoOrdem("X");
		semQuantidade.remove("quantidade");
		criar(ordemId, semQuantidade)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.quantidade").exists());
	}

	@Test
	@DisplayName("descrição de 4001 caracteres dá 400 em errors.descricao e descrição vazia vira null")
	void descricao() throws Exception {
		Map<String, Object> grande = corpoOrdem("X");
		grande.put("descricao", "a".repeat(4001));
		criar(ordemId, grande)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.descricao").exists());

		Map<String, Object> vazia = corpoOrdem("X");
		vazia.put("descricao", "  ");
		criar(ordemId, vazia)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.descricao").isEmpty());
		Map<String, Object> vaziaDnd = corpoDnd("X");
		vaziaDnd.put("descricao", "");
		criar(dndId, vaziaDnd)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.descricao").isEmpty());
	}

	@Test
	@DisplayName("chave desconhecida dá 400 e id no corpo é ignorado")
	void chaveDesconhecidaEId() throws Exception {
		Map<String, Object> comExtra = corpoOrdem("X");
		comExtra.put("extra", "y");
		criar(ordemId, comExtra)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("extra")));

		Map<String, Object> comId = corpoOrdem("Com id");
		comId.put("id", 12345);
		String resposta = criar(ordemId, comId)
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		assertNotEquals(12345L, objectMapper.readTree(resposta).get("id").asLong());
	}

	@Test
	@DisplayName("POST em ficha inexistente dá 404 Ficha não encontrada")
	void criarEmFichaInexistente() throws Exception {
		criar(ID_INEXISTENTE, corpoOrdem("X"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}

	// ---- erros de Ordem ----

	@Test
	@DisplayName("Ordem: categoria 5 dá 400; categoria ausente dá 400 é obrigatório")
	void categoriaInvalida() throws Exception {
		criar(ordemId, corpoOrdem("X", 5, 1, 1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.categoria").exists());
		Map<String, Object> semCategoria = corpoOrdem("X");
		semCategoria.remove("categoria");
		criar(ordemId, semCategoria)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.categoria").value("é obrigatório"));
	}

	@Test
	@DisplayName("Ordem: espaços -1 dá 400; espaços ausente dá 400 é obrigatório")
	void espacosInvalidos() throws Exception {
		criar(ordemId, corpoOrdem("X", 0, -1, 1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.espacos").exists());
		Map<String, Object> semEspacos = corpoOrdem("X");
		semEspacos.remove("espacos");
		criar(ordemId, semEspacos)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.espacos").value("é obrigatório"));
	}

	@Test
	@DisplayName("Ordem: peso e equipado no corpo dão 400 não existe em Ordem Paranormal")
	void camposDeDndEmOrdem() throws Exception {
		Map<String, Object> comPeso = corpoOrdem("X");
		comPeso.put("peso", 1.5);
		criar(ordemId, comPeso)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.peso").value("não existe em Ordem Paranormal"));

		Map<String, Object> comEquipado = corpoOrdem("X");
		comEquipado.put("equipado", false);
		criar(ordemId, comEquipado)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.equipado").value("não existe em Ordem Paranormal"));
	}

	@Test
	@DisplayName("Ordem sem categoria e sem espaços dá 400 com as duas chaves de uma vez")
	void variosErrosDeUmaVez() throws Exception {
		Map<String, Object> corpo = corpoOrdem("X");
		corpo.remove("categoria");
		corpo.remove("espacos");
		criar(ordemId, corpo)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.categoria").value("é obrigatório"))
				.andExpect(jsonPath("$.errors.espacos").value("é obrigatório"));
	}

	// ---- erros de D&D ----

	@Test
	@DisplayName("D&D: peso ausente dá 400 é obrigatório; -1, 1.555 e 100000 dão 400; 99999.99 passa")
	void pesoInvalido() throws Exception {
		Map<String, Object> semPeso = corpoDnd("X");
		semPeso.remove("peso");
		criar(dndId, semPeso)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.peso").value("é obrigatório"));
		criar(dndId, corpoDnd("X", -1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.peso").exists());
		criar(dndId, corpoDnd("X", new BigDecimal("1.555")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.peso").exists());
		criar(dndId, corpoDnd("X", 100000))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.peso").exists());
		criar(dndId, corpoDnd("X", new BigDecimal("99999.99")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.peso").value(99999.99));
	}

	@Test
	@DisplayName("D&D: categoria e espaços no corpo dão 400 não existe em D&D")
	void camposDeOrdemEmDnd() throws Exception {
		Map<String, Object> comCategoria = corpoDnd("X");
		comCategoria.put("categoria", 1);
		criar(dndId, comCategoria)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.categoria").value("não existe em D&D"));

		Map<String, Object> comEspacos = corpoDnd("X");
		comEspacos.put("espacos", 1);
		criar(dndId, comEspacos)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.espacos").value("não existe em D&D"));
	}

	// ---- editar ----

	@Test
	@DisplayName("PUT muda os campos do item de Ordem")
	void editaEmOrdem() throws Exception {
		Long id = itemOrdemSalvo("Antes");

		editar(ordemId, id, corpoOrdem("Depois", 3, 2, 4))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.nome").value("Depois"))
				.andExpect(jsonPath("$.categoria").value(3))
				.andExpect(jsonPath("$.espacos").value(2))
				.andExpect(jsonPath("$.quantidade").value(4));

		mockMvc.perform(get("/fichas/{id}", ordemId))
				.andExpect(jsonPath("$.itens[0].nome").value("Depois"))
				.andExpect(jsonPath("$.itens[0].categoria").value(3));
	}

	@Test
	@DisplayName("PUT muda os campos do item de D&D")
	void editaEmDnd() throws Exception {
		Long id = itemDndSalvo("Antes");
		Map<String, Object> corpo = corpoDnd("Depois", 2.25);
		corpo.put("quantidade", 3);
		corpo.put("equipado", true);

		editar(dndId, id, corpo)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.nome").value("Depois"))
				.andExpect(jsonPath("$.quantidade").value(3))
				.andExpect(jsonPath("$.peso").value(2.25))
				.andExpect(jsonPath("$.equipado").value(true));

		mockMvc.perform(get("/fichas/{id}", dndId))
				.andExpect(jsonPath("$.itens[0].nome").value("Depois"))
				.andExpect(jsonPath("$.itens[0].equipado").value(true));
	}

	@Test
	@DisplayName("PUT com dado inválido dá 400 e o item não muda")
	void editaComDadoInvalido() throws Exception {
		Long idOrdem = itemOrdemSalvo("Antes");
		editar(ordemId, idOrdem, corpoOrdem("Depois", 5, 1, 1)).andExpect(status().isBadRequest());
		Map<String, Object> comPeso = corpoOrdem("Depois");
		comPeso.put("peso", 1.5);
		editar(ordemId, idOrdem, comPeso)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.peso").value("não existe em Ordem Paranormal"));
		assertEquals("Antes", itensOrdem.findById(idOrdem).orElseThrow().getNome());

		Long idDnd = itemDndSalvo("Antes");
		editar(dndId, idDnd, corpoDnd("Depois", -1)).andExpect(status().isBadRequest());
		Map<String, Object> comCategoria = corpoDnd("Depois");
		comCategoria.put("categoria", 1);
		editar(dndId, idDnd, comCategoria)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.categoria").value("não existe em D&D"));
		assertEquals("Antes", itensDnd.findById(idDnd).orElseThrow().getNome());
	}

	@Test
	@DisplayName("PUT de item de outra ficha dá 404 Item não encontrado e ele fica intacto")
	void editarDeOutraFicha() throws Exception {
		Long id = itemOrdemSalvo("Da outra");
		FichaOrdem segunda = (FichaOrdem) fichas.save(FichasDeTeste.ordem("Outra"));

		editar(segunda.getId(), id, corpoOrdem("Roubado"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"))
				.andExpect(jsonPath("$.detail").value("Não existe item com id " + id + " nesta ficha."));

		assertEquals("Da outra", itensOrdem.findById(id).orElseThrow().getNome());
	}

	@Test
	@DisplayName("PUT numa ficha de D&D com o id de um item de Ordem dá 404")
	void editarComIdDoOutroSistema() throws Exception {
		Long idDeOrdem = itemOrdemSalvo("De Ordem");

		editar(dndId, idDeOrdem, corpoDnd("Roubado"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"));

		assertEquals("De Ordem", itensOrdem.findById(idDeOrdem).orElseThrow().getNome());
	}

	@Test
	@DisplayName("PUT de item inexistente dá 404 Item; ficha inexistente dá 404 Ficha")
	void editarInexistente() throws Exception {
		editar(ordemId, ID_INEXISTENTE, corpoOrdem("X"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"));
		editar(ID_INEXISTENTE, 1L, corpoOrdem("X"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}

	// ---- excluir ----

	@Test
	@DisplayName("DELETE dá 204 e o item some do GET da ficha, nos dois sistemas")
	void exclui() throws Exception {
		Long idOrdem = itemOrdemSalvo("Some");
		Long idDnd = itemDndSalvo("Some");

		mockMvc.perform(delete("/fichas/{id}/itens/{i}", ordemId, idOrdem)).andExpect(status().isNoContent());
		mockMvc.perform(delete("/fichas/{id}/itens/{i}", dndId, idDnd)).andExpect(status().isNoContent());

		mockMvc.perform(get("/fichas/{id}", ordemId)).andExpect(jsonPath("$.itens.length()").value(0));
		mockMvc.perform(get("/fichas/{id}", dndId)).andExpect(jsonPath("$.itens.length()").value(0));
	}

	@Test
	@DisplayName("DELETE de item de outra ficha dá 404 Item não encontrado e ele continua")
	void excluirDeOutraFicha() throws Exception {
		Long id = itemDndSalvo("Da outra");

		mockMvc.perform(delete("/fichas/{id}/itens/{i}", ordemId, id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"));

		assertTrue(itensDnd.findById(id).isPresent());
	}

	@Test
	@DisplayName("DELETE em ficha inexistente dá 404 Ficha não encontrada")
	void excluirEmFichaInexistente() throws Exception {
		mockMvc.perform(delete("/fichas/{id}/itens/{i}", ID_INEXISTENTE, 1L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}
}
