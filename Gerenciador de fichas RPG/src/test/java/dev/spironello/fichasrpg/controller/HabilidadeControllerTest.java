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
import dev.spironello.fichasrpg.model.TipoHabilidade;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.repository.HabilidadeFichaRepository;
import tools.jackson.databind.ObjectMapper;

/** Rotas de /fichas/{id}/habilidades: criar, editar e excluir, com o tipo conferido pelo sistema da ficha. */
@DisplayName("API /fichas/{id}/habilidades")
class HabilidadeControllerTest extends IntegrationTestBase {

	private static final Long ID_INEXISTENTE = 999_999L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository fichas;

	@Autowired
	private HabilidadeFichaRepository habilidades;

	@Autowired
	private ObjectMapper objectMapper;

	private Long ordemId;
	private Long dndId;

	@BeforeEach
	void prepararFichas() {
		habilidades.deleteAll();
		fichas.deleteAll();
		ordemId = fichas.save(FichasDeTeste.ordem("Lia")).getId();
		dndId = fichas.save(FichasDeTeste.dnd("Bram")).getId();
	}

	private static Map<String, Object> corpo(String nome, String tipo) {
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("nome", nome);
		corpo.put("tipo", tipo);
		return corpo;
	}

	private ResultActions criar(Long fichaId, Map<String, Object> corpo) throws Exception {
		return mockMvc.perform(post("/fichas/{id}/habilidades", fichaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)));
	}

	private ResultActions editar(Long fichaId, Long habilidadeId, Map<String, Object> corpo) throws Exception {
		return mockMvc.perform(put("/fichas/{id}/habilidades/{h}", fichaId, habilidadeId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)));
	}

	private Long salvarHabilidade(Long fichaId, String nome, TipoHabilidade tipo) {
		return habilidades.save(FichasDeTeste.habilidade(fichas.findById(fichaId).orElseThrow(), nome, tipo, "desc"))
				.getId();
	}

	@Test
	@DisplayName("POST em ficha de Ordem dá 201 com Location, e o GET da ficha lista a habilidade")
	void criaEListaNaFicha() throws Exception {
		Map<String, Object> corpo = corpo("Ataque especial", "CLASSE");
		corpo.put("descricao", "Gasta 2 PE\npara causar mais dano");

		String resposta = criar(ordemId, corpo)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nome").value("Ataque especial"))
				.andExpect(jsonPath("$.tipo").value("CLASSE"))
				.andExpect(jsonPath("$.descricao").value("Gasta 2 PE\npara causar mais dano"))
				.andReturn().getResponse().getContentAsString();
		long id = objectMapper.readTree(resposta).get("id").asLong();

		mockMvc.perform(get("/fichas/{id}", ordemId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.habilidades.length()").value(1))
				.andExpect(jsonPath("$.habilidades[0].id").value(id))
				.andExpect(jsonPath("$.habilidades[0].nome").value("Ataque especial"));
	}

	@Test
	@DisplayName("o Location devolvido é exatamente /fichas/{id}/habilidades/{id da habilidade}")
	void locationExato() throws Exception {
		var resultado = criar(ordemId, corpo("Uma", "CLASSE")).andExpect(status().isCreated()).andReturn();
		long id = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();

		assertEquals("/fichas/" + ordemId + "/habilidades/" + id, resultado.getResponse().getHeader("Location"));
	}

	@Test
	@DisplayName("três habilidades criadas voltam na ordem de criação")
	void ordemDeCriacao() throws Exception {
		criar(ordemId, corpo("Zeta", "CLASSE")).andExpect(status().isCreated());
		criar(ordemId, corpo("Alfa", "TRILHA")).andExpect(status().isCreated());
		criar(ordemId, corpo("Mu", "OUTRA")).andExpect(status().isCreated());

		mockMvc.perform(get("/fichas/{id}", ordemId))
				.andExpect(jsonPath("$.habilidades.length()").value(3))
				.andExpect(jsonPath("$.habilidades[0].nome").value("Zeta"))
				.andExpect(jsonPath("$.habilidades[1].nome").value("Alfa"))
				.andExpect(jsonPath("$.habilidades[2].nome").value("Mu"));
	}

	@Test
	@DisplayName("descrição vazia vira null")
	void descricaoVaziaViraNula() throws Exception {
		Map<String, Object> corpo = corpo("Sem texto", "OUTRA");
		corpo.put("descricao", "  ");
		criar(ordemId, corpo)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.descricao").isEmpty());
	}

	@Test
	@DisplayName("TRILHA numa ficha de D&D dá 400 em errors.tipo")
	void tipoDeOrdemEmDnd() throws Exception {
		criar(dndId, corpo("Trilha", "TRILHA"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Dados inválidos"))
				.andExpect(jsonPath("$.errors.tipo").value("não existe em D&D"));
	}

	@Test
	@DisplayName("ESPECIE numa ficha de Ordem dá 400 em errors.tipo")
	void tipoDeDndEmOrdem() throws Exception {
		criar(ordemId, corpo("Elfo", "ESPECIE"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.tipo").value("não existe em Ordem Paranormal"));
	}

	@Test
	@DisplayName("nome vazio dá 400 em errors.nome, 256 caracteres também e 255 passa")
	void nomeInvalido() throws Exception {
		criar(ordemId, corpo("", "CLASSE"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nome").exists());
		criar(ordemId, corpo("a".repeat(256), "CLASSE"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nome").exists());
		criar(ordemId, corpo("a".repeat(255), "CLASSE")).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("tipo ausente dá 400 em errors.tipo; tipo desconhecido e chave desconhecida dão 400")
	void corpoInvalido() throws Exception {
		Map<String, Object> semTipo = new LinkedHashMap<>();
		semTipo.put("nome", "X");
		criar(ordemId, semTipo)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.tipo").exists());

		criar(ordemId, corpo("X", "MAGIA")).andExpect(status().isBadRequest());

		Map<String, Object> comExtra = corpo("X", "CLASSE");
		comExtra.put("extra", "y");
		criar(ordemId, comExtra)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("extra")));
	}

	@Test
	@DisplayName("descrição de 4001 caracteres dá 400 em errors.descricao")
	void descricaoGrande() throws Exception {
		Map<String, Object> corpo = corpo("X", "CLASSE");
		corpo.put("descricao", "a".repeat(4001));
		criar(ordemId, corpo)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.descricao").exists());
	}

	@Test
	@DisplayName("id no corpo é ignorado")
	void idNoCorpoEIgnorado() throws Exception {
		Map<String, Object> corpo = corpo("Com id", "CLASSE");
		corpo.put("id", 12345);
		String resposta = criar(ordemId, corpo)
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		assertNotEquals(12345L, objectMapper.readTree(resposta).get("id").asLong());
	}

	@Test
	@DisplayName("POST em ficha inexistente dá 404 Ficha não encontrada")
	void criarEmFichaInexistente() throws Exception {
		criar(ID_INEXISTENTE, corpo("X", "CLASSE"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}

	@Test
	@DisplayName("PUT muda o nome e o tipo")
	void edita() throws Exception {
		Long id = salvarHabilidade(ordemId, "Antes", TipoHabilidade.CLASSE);

		editar(ordemId, id, corpo("Depois", "TRILHA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.nome").value("Depois"))
				.andExpect(jsonPath("$.tipo").value("TRILHA"))
				.andExpect(jsonPath("$.descricao").isEmpty());

		mockMvc.perform(get("/fichas/{id}", ordemId))
				.andExpect(jsonPath("$.habilidades[0].nome").value("Depois"))
				.andExpect(jsonPath("$.habilidades[0].tipo").value("TRILHA"));
	}

	@Test
	@DisplayName("PUT com tipo de outro sistema dá 400 e a habilidade não muda")
	void editaComTipoInvalido() throws Exception {
		Long id = salvarHabilidade(dndId, "Antes", TipoHabilidade.CLASSE);

		editar(dndId, id, corpo("Depois", "ORIGEM"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.tipo").value("não existe em D&D"));

		assertEquals("Antes", habilidades.findById(id).orElseThrow().getNome());
	}

	@Test
	@DisplayName("PUT de habilidade de outra ficha dá 404 Item não encontrado e ela fica intacta")
	void editarDeOutraFicha() throws Exception {
		Long id = salvarHabilidade(dndId, "Da outra", TipoHabilidade.CLASSE);

		editar(ordemId, id, corpo("Roubada", "CLASSE"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"))
				.andExpect(jsonPath("$.detail").value("Não existe habilidade com id " + id + " nesta ficha."));

		assertEquals("Da outra", habilidades.findById(id).orElseThrow().getNome());
	}

	@Test
	@DisplayName("PUT de habilidade inexistente dá 404 Item não encontrado; ficha inexistente dá 404 Ficha")
	void editarInexistente() throws Exception {
		editar(ordemId, ID_INEXISTENTE, corpo("X", "CLASSE"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"));
		editar(ID_INEXISTENTE, 1L, corpo("X", "CLASSE"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}

	@Test
	@DisplayName("DELETE dá 204 e a habilidade some do GET da ficha")
	void exclui() throws Exception {
		Long id = salvarHabilidade(ordemId, "Some", TipoHabilidade.CLASSE);

		mockMvc.perform(delete("/fichas/{id}/habilidades/{h}", ordemId, id)).andExpect(status().isNoContent());

		mockMvc.perform(get("/fichas/{id}", ordemId))
				.andExpect(jsonPath("$.habilidades.length()").value(0));
	}

	@Test
	@DisplayName("DELETE de habilidade de outra ficha dá 404 Item não encontrado e ela continua")
	void excluirDeOutraFicha() throws Exception {
		Long id = salvarHabilidade(dndId, "Da outra", TipoHabilidade.CLASSE);

		mockMvc.perform(delete("/fichas/{id}/habilidades/{h}", ordemId, id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"));

		assertTrue(habilidades.findById(id).isPresent());
	}

	@Test
	@DisplayName("DELETE em ficha inexistente dá 404 Ficha não encontrada")
	void excluirEmFichaInexistente() throws Exception {
		mockMvc.perform(delete("/fichas/{id}/habilidades/{h}", ID_INEXISTENTE, 1L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}
}
