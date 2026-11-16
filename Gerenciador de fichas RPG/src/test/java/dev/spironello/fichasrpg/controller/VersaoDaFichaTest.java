package dev.spironello.fichasrpg.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import dev.spironello.fichasrpg.CampanhasDeTeste;
import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.campanha.Papel;
import dev.spironello.fichasrpg.exception.ApiExceptionHandler;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.repository.FichaRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * Trava otimista (ADR 0025): o PUT leva a versão que o cliente leu; se ela não é a do banco, 409. Toda gravação da linha
 * da ficha sobe a versão (PUT, PATCH de recursos, moedas, espaços e a troca de campanha), e essas rotas rápidas não
 * enviam versão.
 */
@DisplayName("Versão da ficha (trava otimista)")
class VersaoDaFichaTest extends IntegrationTestBase {

	private static final String MENSAGEM = "Esta ficha mudou enquanto você editava. Recarregue para ver a versão nova.";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private FichaRepository fichas;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private ObjectMapper json;

	private final List<Long> criadas = new ArrayList<>();

	@AfterEach
	void limpar() {
		fichas.deleteAllById(criadas);
		jdbc.update("delete from campanha");
	}

	private long novaFicha(Ficha ficha) {
		long id = fichas.saveAndFlush(ficha).getId();
		criadas.add(id);
		return id;
	}

	private static Map<String, Object> semVersao(Map<String, Object> corpo) {
		corpo.remove("versao");
		return corpo;
	}

	private long versao(long id) {
		return jdbc.queryForObject("select versao from ficha where id = ?", Long.class, id);
	}

	private String corpo(Map<String, Object> base, Object versao) throws Exception {
		base.put("versao", versao);
		return json.writeValueAsString(base);
	}

	private org.springframework.test.web.servlet.ResultActions salvar(long id, String corpo) throws Exception {
		return mockMvc.perform(put("/fichas/{id}", id).contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	@Test
	@DisplayName("a ficha nasce com versão 0, e o POST ignora a versão do corpo")
	void nasceComZero() throws Exception {
		String criada = mockMvc.perform(post("/fichas").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(FichasDeTeste.corpoOrdem(), 99))).andExpect(status().isCreated()).andExpect(jsonPath("$.versao").value(0))
				.andReturn().getResponse().getContentAsString();
		criadas.add(JsonPath.<Number>read(criada, "$.id").longValue());
	}

	@Test
	@DisplayName("PUT com a versão atual dá 200 e devolve a versão nova; salvar de novo com a versão da resposta também dá 200 (Foco 4)")
	void putComVersaoAtual() throws Exception {
		long id = novaFicha(FichasDeTeste.ordem("Ana"));
		Map<String, Object> primeiro = FichasDeTeste.corpoOrdem();
		primeiro.put("personagem", "Primeira");

		String resposta = salvar(id, corpo(primeiro, 0)).andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(1))
				.andReturn().getResponse().getContentAsString();
		assertThat(versao(id)).isEqualTo(1);

		Map<String, Object> segundo = FichasDeTeste.corpoOrdem();
		segundo.put("personagem", "Segunda");
		salvar(id, corpo(segundo, JsonPath.<Number>read(resposta, "$.versao"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.versao").value(2)).andExpect(jsonPath("$.personagem").value("Segunda"));
	}

	@Test
	@DisplayName("PUT com versão velha dá 409 com a mensagem do contrato e não grava nada")
	void putComVersaoVelha() throws Exception {
		long id = novaFicha(FichasDeTeste.ordem("Ana"));
		salvar(id, corpo(FichasDeTeste.corpoOrdem(), 0)).andExpect(status().isOk());

		Map<String, Object> velho = FichasDeTeste.corpoOrdem();
		velho.put("personagem", "Velha");
		salvar(id, corpo(velho, 0)).andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(MENSAGEM))
				.andExpect(jsonPath("$.title").value("Ficha desatualizada"));
		salvar(id, corpo(velho, 5)).andExpect(status().isConflict());

		assertThat(jdbc.queryForObject("select personagem from ficha where id = ?", String.class, id)).isEqualTo("Ana");
		assertThat(versao(id)).isEqualTo(1);
	}

	@Test
	@DisplayName("PUT sem versão dá 400 com errors.versao")
	void putSemVersao() throws Exception {
		long id = novaFicha(FichasDeTeste.ordem("Ana"));

		mockMvc.perform(put("/fichas/{id}", id).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(semVersao(FichasDeTeste.corpoOrdem()))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.versao").value("é obrigatória"));
		mockMvc.perform(put("/fichas/{id}", id).contentType(MediaType.APPLICATION_JSON).content(corpo(FichasDeTeste.corpoOrdem(), null)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.versao").value("é obrigatória"));
		assertThat(versao(id)).isZero();
	}

	@Test
	@DisplayName("a versão vale também para D&D")
	void dnd() throws Exception {
		long id = novaFicha(FichasDeTeste.dnd("Bram"));

		salvar(id, corpo(FichasDeTeste.corpoDnd(), 0)).andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(1));
		salvar(id, corpo(FichasDeTeste.corpoDnd(), 0)).andExpect(status().isConflict());
	}

	@Test
	@DisplayName("o PATCH /recursos sobe a versão sem enviar versão, e um PUT com a versão de antes dá 409")
	void patchDeRecursosSobeAVersao() throws Exception {
		long id = novaFicha(FichasDeTeste.ordem("Ana"));
		jdbc.update("update ficha_ordem set pv_max = 20, pv_atual = 20 where ficha_id = ?", id);

		mockMvc.perform(patch("/fichas/{id}/recursos", id).contentType(MediaType.APPLICATION_JSON).content("{\"PV\":15}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(1));
		assertThat(versao(id)).isEqualTo(1);
		mockMvc.perform(patch("/fichas/{id}/recursos", id).contentType(MediaType.APPLICATION_JSON).content("{\"PV\":10}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(2));

		salvar(id, corpo(FichasDeTeste.corpoOrdem(), 0)).andExpect(status().isConflict());
	}

	@Test
	@DisplayName("a troca de campanha sobe a versão")
	void trocaDeCampanhaSobeAVersao() throws Exception {
		long campanha = CampanhasDeTeste.campanha(jdbc, "Mesa");
		CampanhasDeTeste.participar(jdbc, campanha, dev.spironello.fichasrpg.AutenticacaoDeTesteConfig.USUARIO_DE_TESTE_ID, Papel.MESTRE);
		long id = novaFicha(FichasDeTeste.ordem("Ana"));

		mockMvc.perform(put("/fichas/{id}/campanha", id).contentType(MediaType.APPLICATION_JSON).content("{\"campanhaId\":" + campanha + "}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(1)).andExpect(jsonPath("$.campanha.id").value(campanha));
		mockMvc.perform(put("/fichas/{id}/campanha", id).contentType(MediaType.APPLICATION_JSON).content("{\"campanhaId\":null}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(2));
		assertThat(versao(id)).isEqualTo(2);
	}

	@Test
	@DisplayName("moedas e espaços de magia (D&D) sobem a versão; as listas (itens) e o retrato não mexem nela")
	void moedasEEspacosSobemListasNao() throws Exception {
		long id = novaFicha(FichasDeTeste.dnd("Bram"));

		mockMvc.perform(patch("/fichas/{id}/moedas", id).contentType(MediaType.APPLICATION_JSON).content("{\"po\":5}"))
				.andExpect(status().isOk());
		assertThat(versao(id)).as("moedas").isEqualTo(1);

		mockMvc.perform(patch("/fichas/{id}/espacos", id).contentType(MediaType.APPLICATION_JSON).content("{\"1\":{\"total\":3,\"usados\":1}}"))
				.andExpect(status().isOk());
		assertThat(versao(id)).as("espaços").isEqualTo(2);

		mockMvc.perform(post("/fichas/{id}/itens", id).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Corda\",\"quantidade\":1,\"peso\":1}"))
				.andExpect(status().isCreated());
		mockMvc.perform(put("/fichas/{id}/retrato", id)
				.contentType(MediaType.IMAGE_JPEG).content(dev.spironello.fichasrpg.ImagensDeTeste.jpeg(40, 50))).andExpect(status().isNoContent());
		assertThat(versao(id)).as("itens e retrato").isEqualTo(2);
	}

	@Test
	@DisplayName("o GET devolve a versão atual")
	void getDevolveAVersao() throws Exception {
		long id = novaFicha(FichasDeTeste.ordem("Ana"));
		salvar(id, corpo(FichasDeTeste.corpoOrdem(), 0)).andExpect(status().isOk());

		mockMvc.perform(get("/fichas/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(1));
	}

	@Test
	@DisplayName("a falha da trava otimista do Hibernate (dois pedidos ao mesmo tempo) vira o mesmo 409")
	void conflitoDoHibernateVira409() {
		ProblemDetail problema = new ApiExceptionHandler().handleFichaDesatualizada(new ObjectOptimisticLockingFailureException(Ficha.class, 1L));

		assertThat(problema.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(problema.getDetail()).isEqualTo(MENSAGEM);
	}
}
