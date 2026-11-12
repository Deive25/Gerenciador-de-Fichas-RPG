package dev.spironello.fichasrpg.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import dev.spironello.fichasrpg.repository.AtaqueDndRepository;
import dev.spironello.fichasrpg.repository.AtaqueOrdemRepository;
import dev.spironello.fichasrpg.repository.FichaRepository;
import tools.jackson.databind.ObjectMapper;

/** Rotas de /fichas/{id}/ataques: criar, editar e excluir nos dois sistemas, e a convivência com o PUT da ficha. */
@DisplayName("API /fichas/{id}/ataques")
class AtaqueControllerTest extends IntegrationTestBase {

	private static final Long ID_INEXISTENTE = 999_999L;

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private FichaRepository fichas;
	@Autowired
	private AtaqueOrdemRepository ataquesOrdem;
	@Autowired
	private AtaqueDndRepository ataquesDnd;
	@Autowired
	private ObjectMapper objectMapper;

	private FichaOrdem ordem;
	private FichaDnd dnd;
	private Long ordemId;
	private Long dndId;

	@BeforeEach
	void prepararFichas() {
		fichas.deleteAll();
		ordem = (FichaOrdem) fichas.save(FichasDeTeste.ordem("Lia"));
		dnd = (FichaDnd) fichas.save(FichasDeTeste.dnd("Bram"));
		ordemId = ordem.getId();
		dndId = dnd.getId();
	}

	private static Map<String, Object> corpoOrdem(String nome) {
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("nome", nome);
		corpo.put("pericia", "LUTA");
		corpo.put("dano", "1d8+3");
		return corpo;
	}

	private static Map<String, Object> corpoDnd(String nome) {
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("nome", nome);
		corpo.put("atributo", "FOR");
		corpo.put("dano", "1d8+3");
		return corpo;
	}

	private ResultActions criar(Long fichaId, Map<String, Object> corpo) throws Exception {
		return mockMvc.perform(post("/fichas/{id}/ataques", fichaId)
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(corpo)));
	}

	private ResultActions editar(Long fichaId, Long ataqueId, Map<String, Object> corpo) throws Exception {
		return mockMvc.perform(put("/fichas/{id}/ataques/{a}", fichaId, ataqueId)
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(corpo)));
	}

	private Long ataqueOrdemSalvo(String nome) {
		return ataquesOrdem.save(FichasDeTeste.ataqueOrdem(ordem, nome)).getId();
	}

	private Long ataqueDndSalvo(String nome) {
		return ataquesDnd.save(FichasDeTeste.ataqueDnd(dnd, nome)).getId();
	}

	// ---- criar ----

	@Test
	@DisplayName("Ordem: POST dá 201 com Location exato e todos os campos, e o GET da ficha lista o ataque")
	void criaEmOrdem() throws Exception {
		Map<String, Object> corpo = corpoOrdem("Katana");
		corpo.put("pericia", "PONTARIA");
		corpo.put("bonus", 5);
		corpo.put("dano", "1d10+2");
		corpo.put("tipoDano", "Corte");
		corpo.put("alcance", "Corpo a corpo");
		corpo.put("margem", 19);
		corpo.put("multiplicador", 3);

		var resultado = criar(ordemId, corpo)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nome").value("Katana"))
				.andExpect(jsonPath("$.pericia").value("PONTARIA"))
				.andExpect(jsonPath("$.bonus").value(5))
				.andExpect(jsonPath("$.dano").value("1d10+2"))
				.andExpect(jsonPath("$.tipoDano").value("Corte"))
				.andExpect(jsonPath("$.alcance").value("Corpo a corpo"))
				.andExpect(jsonPath("$.margem").value(19))
				.andExpect(jsonPath("$.multiplicador").value(3))
				.andReturn();
		long id = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
		assertEquals("/fichas/" + ordemId + "/ataques/" + id, resultado.getResponse().getHeader("Location"));

		mockMvc.perform(get("/fichas/{id}", ordemId))
				.andExpect(jsonPath("$.ataques.length()").value(1))
				.andExpect(jsonPath("$.ataques[0].id").value(id))
				.andExpect(jsonPath("$.ataques[0].nome").value("Katana"));
	}

	@Test
	@DisplayName("Ordem: só nome, perícia e dano usa os padrões (bônus 0, margem 20, multiplicador 2) e a notação sai canônica")
	void padroesEmOrdem() throws Exception {
		Map<String, Object> corpo = corpoOrdem("Faca");
		corpo.put("dano", " 1D4 + 2 ");
		corpo.put("tipoDano", "  ");
		criar(ordemId, corpo)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.bonus").value(0))
				.andExpect(jsonPath("$.margem").value(20))
				.andExpect(jsonPath("$.multiplicador").value(2))
				.andExpect(jsonPath("$.dano").value("1d4+2"))
				.andExpect(jsonPath("$.tipoDano").doesNotExist())
				.andExpect(jsonPath("$.alcance").doesNotExist());
	}

	@Test
	@DisplayName("D&D: POST dá 201 com Location, e o GET da ficha lista o ataque")
	void criaEmDnd() throws Exception {
		Map<String, Object> corpo = corpoDnd("Espada longa");
		corpo.put("proficiente", true);
		corpo.put("bonus", 1);
		corpo.put("tipoDano", "Cortante");

		var resultado = criar(dndId, corpo)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.atributo").value("FOR"))
				.andExpect(jsonPath("$.proficiente").value(true))
				.andExpect(jsonPath("$.bonus").value(1))
				.andExpect(jsonPath("$.dano").value("1d8+3"))
				.andExpect(jsonPath("$.tipoDano").value("Cortante"))
				.andReturn();
		long id = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
		assertEquals("/fichas/" + dndId + "/ataques/" + id, resultado.getResponse().getHeader("Location"));

		mockMvc.perform(get("/fichas/{id}", dndId))
				.andExpect(jsonPath("$.ataques.length()").value(1))
				.andExpect(jsonPath("$.ataques[0].nome").value("Espada longa"));
	}

	@Test
	@DisplayName("D&D: sem proficiente e sem bônus valem false e 0")
	void padroesEmDnd() throws Exception {
		criar(dndId, corpoDnd("Adaga"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.proficiente").value(false))
				.andExpect(jsonPath("$.bonus").value(0));
	}

	@Test
	@DisplayName("os ataques voltam na ordem de criação")
	void ordemDeCriacao() throws Exception {
		criar(ordemId, corpoOrdem("C")).andExpect(status().isCreated());
		criar(ordemId, corpoOrdem("A")).andExpect(status().isCreated());
		criar(ordemId, corpoOrdem("B")).andExpect(status().isCreated());

		mockMvc.perform(get("/fichas/{id}", ordemId))
				.andExpect(jsonPath("$.ataques[0].nome").value("C"))
				.andExpect(jsonPath("$.ataques[1].nome").value("A"))
				.andExpect(jsonPath("$.ataques[2].nome").value("B"));
	}

	// ---- 400 por campo ----

	@Test
	@DisplayName("nome vazio ou de 256 caracteres dá 400 em errors.nome; 255 passa")
	void nomeInvalido() throws Exception {
		criar(ordemId, corpoOrdem("")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.nome").exists());
		criar(ordemId, corpoOrdem("x".repeat(256))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nome").exists());
		criar(ordemId, corpoOrdem("x".repeat(255))).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("bônus: 100 e -100 dão 400 em errors.bonus; 99 e -99 passam")
	void bonusInvalido() throws Exception {
		for (int invalido : new int[] { 100, -100 }) {
			Map<String, Object> corpo = corpoOrdem("X");
			corpo.put("bonus", invalido);
			criar(ordemId, corpo).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.bonus").exists());
		}
		for (int valido : new int[] { 99, -99 }) {
			Map<String, Object> corpo = corpoDnd("X");
			corpo.put("bonus", valido);
			criar(dndId, corpo).andExpect(status().isCreated()).andExpect(jsonPath("$.bonus").value(valido));
		}
	}

	@Test
	@DisplayName("dano ausente dá 400 é obrigatório; dano com notação inválida dá 400 em errors.dano com a mensagem do caso")
	void danoInvalido() throws Exception {
		Map<String, Object> semDano = corpoOrdem("X");
		semDano.remove("dano");
		criar(ordemId, semDano).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.dano").value("é obrigatório"));

		String[][] casos = { { "d6", "notação inválida (ex.: 2d6+3)" }, { "1d7", "use d4, d6, d8, d10, d12, d20 ou d100" },
				{ "21d6", "no máximo 20 dados" }, { "1d8-1d4", "dados não podem ser subtraídos" },
				{ "100", "a constante deve ir de 0 a 99" } };
		for (String[] caso : casos) {
			Map<String, Object> corpo = corpoOrdem("X");
			corpo.put("dano", caso[0]);
			criar(ordemId, corpo).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.dano").value(caso[1]));
			Map<String, Object> corpoDnd = corpoDnd("X");
			corpoDnd.put("dano", caso[0]);
			criar(dndId, corpoDnd).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.dano").value(caso[1]));
		}
		Map<String, Object> longo = corpoOrdem("X");
		longo.put("dano", "1".repeat(61));
		criar(ordemId, longo).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.dano").exists());
	}

	@Test
	@DisplayName("tipoDano e alcance de 256 caracteres dão 400 nos próprios campos")
	void textosLongos() throws Exception {
		Map<String, Object> corpo = corpoOrdem("X");
		corpo.put("tipoDano", "x".repeat(256));
		corpo.put("alcance", "x".repeat(256));
		criar(ordemId, corpo).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.tipoDano").exists())
				.andExpect(jsonPath("$.errors.alcance").exists());
	}

	@Test
	@DisplayName("Ordem: perícia ausente dá 400 é obrigatório; ATLETISMO dá 400 use LUTA ou PONTARIA; perícia desconhecida dá 400")
	void periciaInvalida() throws Exception {
		Map<String, Object> sem = corpoOrdem("X");
		sem.remove("pericia");
		criar(ordemId, sem).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.pericia").value("é obrigatório"));
		Map<String, Object> outra = corpoOrdem("X");
		outra.put("pericia", "ATLETISMO");
		criar(ordemId, outra).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.pericia").value("use LUTA ou PONTARIA"));
		Map<String, Object> desconhecida = corpoOrdem("X");
		desconhecida.put("pericia", "XYZ");
		criar(ordemId, desconhecida).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Ordem: margem 1 e 21 e multiplicador 1 e 6 dão 400 nos próprios campos; 2 passa em todos")
	void margemEMultiplicador() throws Exception {
		for (int margem : new int[] { 1, 21 }) {
			Map<String, Object> corpo = corpoOrdem("X");
			corpo.put("margem", margem);
			criar(ordemId, corpo).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.margem").exists());
		}
		for (int multiplicador : new int[] { 1, 6 }) {
			Map<String, Object> corpo = corpoOrdem("X");
			corpo.put("multiplicador", multiplicador);
			criar(ordemId, corpo).andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errors.multiplicador").exists());
		}
		Map<String, Object> limites = corpoOrdem("X");
		limites.put("margem", 2);
		limites.put("multiplicador", 5);
		criar(ordemId, limites).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("Ordem: atributo e proficiente no corpo dão 400 não existe em Ordem Paranormal, nos dois campos de uma vez")
	void camposDeDndEmOrdem() throws Exception {
		Map<String, Object> corpo = corpoOrdem("X");
		corpo.put("atributo", "FOR");
		corpo.put("proficiente", false);
		criar(ordemId, corpo).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.atributo").value("não existe em Ordem Paranormal"))
				.andExpect(jsonPath("$.errors.proficiente").value("não existe em Ordem Paranormal"));
	}

	@Test
	@DisplayName("D&D: atributo ausente dá 400 é obrigatório; atributo desconhecido dá 400")
	void atributoInvalido() throws Exception {
		Map<String, Object> sem = corpoDnd("X");
		sem.remove("atributo");
		criar(dndId, sem).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.atributo").value("é obrigatório"));
		Map<String, Object> desconhecido = corpoDnd("X");
		desconhecido.put("atributo", "STR");
		criar(dndId, desconhecido).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("D&D: pericia, margem e multiplicador no corpo dão 400 não existe em D&D, nos três campos de uma vez")
	void camposDeOrdemEmDnd() throws Exception {
		Map<String, Object> corpo = corpoDnd("X");
		corpo.put("pericia", "LUTA");
		corpo.put("margem", 20);
		corpo.put("multiplicador", 2);
		criar(dndId, corpo).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.pericia").value("não existe em D&D"))
				.andExpect(jsonPath("$.errors.margem").value("não existe em D&D"))
				.andExpect(jsonPath("$.errors.multiplicador").value("não existe em D&D"));
	}

	@Test
	@DisplayName("chave desconhecida dá 400, id no corpo é ignorado e ficha inexistente dá 404 Ficha")
	void chaveDesconhecidaEFichaInexistente() throws Exception {
		Map<String, Object> estranha = corpoOrdem("X");
		estranha.put("forca", 3);
		criar(ordemId, estranha).andExpect(status().isBadRequest());
		Map<String, Object> comId = corpoOrdem("X");
		comId.put("id", 123456);
		criar(ordemId, comId).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(123456)));
		criar(ID_INEXISTENTE, corpoOrdem("X")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}

	// ---- editar ----

	@Test
	@DisplayName("PUT muda os campos do ataque de Ordem e de D&D")
	void edita() throws Exception {
		Long idOrdem = ataqueOrdemSalvo("Antes");
		Map<String, Object> corpo = corpoOrdem("Depois");
		corpo.put("margem", 18);
		corpo.put("dano", "2d6");
		editar(ordemId, idOrdem, corpo).andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Depois"))
				.andExpect(jsonPath("$.margem").value(18))
				.andExpect(jsonPath("$.dano").value("2d6"));
		assertEquals("Depois", ataquesOrdem.findById(idOrdem).orElseThrow().getNome());

		Long idDnd = ataqueDndSalvo("Antes");
		Map<String, Object> corpoDnd = corpoDnd("Depois");
		corpoDnd.put("proficiente", true);
		editar(dndId, idDnd, corpoDnd).andExpect(status().isOk())
				.andExpect(jsonPath("$.proficiente").value(true));
		assertTrue(ataquesDnd.findById(idDnd).orElseThrow().isProficiente());
	}

	@Test
	@DisplayName("PUT com dado inválido dá 400 e o ataque não muda")
	void editaComDadoInvalido() throws Exception {
		Long id = ataqueOrdemSalvo("Intacto");
		Map<String, Object> corpo = corpoOrdem("Mudou");
		corpo.put("dano", "abc");
		editar(ordemId, id, corpo).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.dano").exists());
		assertEquals("Intacto", ataquesOrdem.findById(id).orElseThrow().getNome());
	}

	@Test
	@DisplayName("PUT de ataque de outra ficha dá 404 Item não encontrado e ele fica intacto")
	void editarDeOutraFicha() throws Exception {
		Long id = ataqueOrdemSalvo("Da outra");
		FichaOrdem segunda = (FichaOrdem) fichas.save(FichasDeTeste.ordem("Outra"));

		editar(segunda.getId(), id, corpoOrdem("Roubado")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"))
				.andExpect(jsonPath("$.detail").value("Não existe ataque com id " + id + " nesta ficha."));
		assertEquals("Da outra", ataquesOrdem.findById(id).orElseThrow().getNome());
	}

	@Test
	@DisplayName("PUT numa ficha de D&D com o id de um ataque de Ordem dá 404; ataque ou ficha inexistente também")
	void editarInexistente() throws Exception {
		Long idDeOrdem = ataqueOrdemSalvo("De Ordem");
		editar(dndId, idDeOrdem, corpoDnd("X")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"));
		editar(ordemId, ID_INEXISTENTE, corpoOrdem("X")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"));
		editar(ID_INEXISTENTE, 1L, corpoOrdem("X")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}

	// ---- excluir ----

	@Test
	@DisplayName("DELETE dá 204 e o ataque some do GET da ficha, nos dois sistemas")
	void exclui() throws Exception {
		Long idOrdem = ataqueOrdemSalvo("Some");
		Long idDnd = ataqueDndSalvo("Some");
		mockMvc.perform(delete("/fichas/{id}/ataques/{a}", ordemId, idOrdem)).andExpect(status().isNoContent());
		mockMvc.perform(delete("/fichas/{id}/ataques/{a}", dndId, idDnd)).andExpect(status().isNoContent());
		mockMvc.perform(get("/fichas/{id}", ordemId)).andExpect(jsonPath("$.ataques.length()").value(0));
		mockMvc.perform(get("/fichas/{id}", dndId)).andExpect(jsonPath("$.ataques.length()").value(0));
	}

	@Test
	@DisplayName("DELETE de ataque de outra ficha dá 404 e ele continua; ficha inexistente dá 404 Ficha")
	void excluirDeOutraFicha() throws Exception {
		Long id = ataqueOrdemSalvo("Fica");
		FichaOrdem segunda = (FichaOrdem) fichas.save(FichasDeTeste.ordem("Outra"));
		mockMvc.perform(delete("/fichas/{id}/ataques/{a}", segunda.getId(), id)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"));
		assertTrue(ataquesOrdem.existsById(id));
		mockMvc.perform(delete("/fichas/{id}/ataques/{a}", ID_INEXISTENTE, id)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}

	// ---- ficha ----

	@Test
	@DisplayName("excluir a ficha leva os ataques junto (cascade do banco), nos dois sistemas")
	void cascata() throws Exception {
		ataqueOrdemSalvo("A");
		ataqueDndSalvo("B");
		mockMvc.perform(delete("/fichas/{id}", ordemId)).andExpect(status().isNoContent());
		mockMvc.perform(delete("/fichas/{id}", dndId)).andExpect(status().isNoContent());
		assertEquals(0, ataquesOrdem.count());
		assertEquals(0, ataquesDnd.count());
	}

	@Test
	@DisplayName("PUT /fichas/{id} preserva os ataques")
	void putDaFichaPreservaAtaques() throws Exception {
		ataqueOrdemSalvo("Fica");
		ataqueDndSalvo("Fica");
		Map<String, Object> corpoO = FichasDeTeste.corpoOrdem();
		corpoO.put("personagem", "Renomeada");
		mockMvc.perform(put("/fichas/{id}", ordemId).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpoO))).andExpect(status().isOk());
		mockMvc.perform(put("/fichas/{id}", dndId).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(FichasDeTeste.corpoDnd()))).andExpect(status().isOk());

		mockMvc.perform(get("/fichas/{id}", ordemId)).andExpect(jsonPath("$.ataques.length()").value(1))
				.andExpect(jsonPath("$.ataques[0].nome").value("Fica"));
		mockMvc.perform(get("/fichas/{id}", dndId)).andExpect(jsonPath("$.ataques.length()").value(1));
	}

	@Test
	@DisplayName("PUT /fichas/{id} com ataques no corpo dá 400 (JSON estrito), nos dois sistemas")
	void putDaFichaComAtaquesDa400() throws Exception {
		Map<String, Object> corpoO = FichasDeTeste.corpoOrdem();
		corpoO.put("ataques", java.util.List.of());
		mockMvc.perform(put("/fichas/{id}", ordemId).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpoO))).andExpect(status().isBadRequest());
		Map<String, Object> corpoD = FichasDeTeste.corpoDnd();
		corpoD.put("ataques", java.util.List.of());
		mockMvc.perform(put("/fichas/{id}", dndId).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpoD))).andExpect(status().isBadRequest());
	}
}
