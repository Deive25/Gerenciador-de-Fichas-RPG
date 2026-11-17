package dev.spironello.fichasrpg.rolagem;

import static dev.spironello.fichasrpg.CampanhasDeTeste.como;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import dev.spironello.fichasrpg.AutenticacaoDeTesteConfig;
import dev.spironello.fichasrpg.CampanhasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.campanha.Papel;
import dev.spironello.fichasrpg.repository.FichaRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** GET /campanhas/{id}/rolagens: a ordem, o cursor "antes", o limite, o acesso e o formato do JSON (spec §5). */
@DisplayName("API GET /campanhas/{id}/rolagens")
class HistoricoDeRolagensTest extends IntegrationTestBase {

	private static final long A = AutenticacaoDeTesteConfig.USUARIO_DE_TESTE_ID;
	private static final long M = 7_200_001L;
	private static final long F = 7_200_003L;

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private FichaRepository fichas;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private ObjectMapper json;

	private long campanha;

	@BeforeEach
	void prepararMesa() {
		CampanhasDeTeste.usuario(jdbc, M, "Mestre Mor");
		CampanhasDeTeste.usuario(jdbc, F, "De fora");
		campanha = CampanhasDeTeste.campanha(jdbc, "Mesa");
		CampanhasDeTeste.participar(jdbc, campanha, M, Papel.MESTRE);
		CampanhasDeTeste.participar(jdbc, campanha, A, Papel.JOGADOR);
	}

	@AfterEach
	void limpar() {
		CampanhasDeTeste.limpar(jdbc, fichas, A, M, F);
	}

	private long gravar(long usuario, String personagem, String tipo, String rotulo, int total, String detalhe) {
		return jdbc.queryForObject("insert into rolagem (campanha_id, usuario_id, personagem, tipo, rotulo, total, detalhe) "
				+ "values (?, ?, ?, ?, ?, ?, ?::jsonb) returning id", Long.class, campanha, usuario, personagem, tipo, rotulo, total, detalhe);
	}

	private void gravarVarias(int quantidade) {
		jdbc.update("insert into rolagem (campanha_id, usuario_id, personagem, tipo, rotulo, total, detalhe) "
				+ "select ?, ?, 'Kai', 'LIVRE', '1d20', 10, '{}'::jsonb from generate_series(1, ?)", campanha, A, quantidade);
	}

	private List<Long> ids(String corpo) {
		List<Long> ids = new ArrayList<>();
		for (Number n : JsonPath.<List<Number>>read(corpo, "$[*].id")) {
			ids.add(n.longValue());
		}
		return ids;
	}

	@Test
	@DisplayName("vem da mais nova para a mais antiga, e 'antes' traz só os ids menores")
	void ordemECursor() throws Exception {
		long a = gravar(A, "Kai", "LIVRE", "1d20", 10, "{}");
		long b = gravar(M, null, "LIVRE", "1d20", 11, "{}");
		long c = gravar(A, "Kai", "LIVRE", "1d20", 12, "{}");

		String todas = mockMvc.perform(get("/campanhas/{id}/rolagens", campanha)).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(ids(todas)).containsExactly(c, b, a);

		String antes = mockMvc.perform(get("/campanhas/{id}/rolagens", campanha).param("antes", String.valueOf(c)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertThat(ids(antes)).containsExactly(b, a);

		mockMvc.perform(get("/campanhas/{id}/rolagens", campanha).param("antes", String.valueOf(a)))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	@DisplayName("o limite usa 50 por padrão, aceita 100 e dá 400 com 0 ou 101")
	void limite() throws Exception {
		gravarVarias(120);

		mockMvc.perform(get("/campanhas/{id}/rolagens", campanha)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(50)));
		mockMvc.perform(get("/campanhas/{id}/rolagens", campanha).param("limite", "100")).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(100)));
		mockMvc.perform(get("/campanhas/{id}/rolagens", campanha).param("limite", "1")).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));
		mockMvc.perform(get("/campanhas/{id}/rolagens", campanha).param("limite", "0")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Dados inválidos")).andExpect(jsonPath("$.errors.limite").exists());
		mockMvc.perform(get("/campanhas/{id}/rolagens", campanha).param("limite", "101")).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("quem não é membro recebe 404, e sem token, 401")
	void acesso() throws Exception {
		gravar(A, "Kai", "LIVRE", "1d20", 10, "{}");

		mockMvc.perform(get("/campanhas/{id}/rolagens", campanha).with(como(F))).andExpect(status().isNotFound());
		mockMvc.perform(get("/campanhas/{id}/rolagens", campanha + 1000)).andExpect(status().isNotFound());
		mockMvc.perform(get("/campanhas/{id}/rolagens", campanha).with(anonymous())).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("o JSON tem exatamente os campos da spec, o detalhe sai como objeto e o nome do NPC não aparece")
	void formato() throws Exception {
		gravar(A, "Kai", "TESTE", "Furtividade", 25, "{\"dados\": [12, 20], \"total\": 25}");
		gravar(M, null, "LIVRE", "2d6+3", 9, "{\"total\": 9}");

		String corpo = mockMvc.perform(get("/campanhas/{id}/rolagens", campanha)).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JsonNode lista = json.readTree(corpo);

		JsonNode doMestre = lista.get(0);
		assertThat(doMestre.propertyNames()).containsExactlyInAnyOrder("id", "campanhaId", "criadaEm", "autor", "personagem",
				"doMestre", "tipo", "rotulo", "total", "detalhe");
		assertThat(doMestre.get("autor").propertyNames()).containsExactlyInAnyOrder("id", "nome");
		assertThat(doMestre.get("autor").get("nome").asString()).isEqualTo("Mestre Mor");
		assertThat(doMestre.get("autor").get("id").asLong()).isEqualTo(M);
		assertThat(doMestre.get("campanhaId").asLong()).isEqualTo(campanha);
		assertThat(doMestre.get("personagem").isNull()).isTrue();
		assertThat(doMestre.get("doMestre").asBoolean()).isTrue();
		assertThat(doMestre.get("tipo").asString()).isEqualTo("LIVRE");
		assertThat(doMestre.get("detalhe").isObject()).as("o detalhe sai como objeto, e não como texto").isTrue();

		JsonNode doJogador = lista.get(1);
		assertThat(doJogador.get("personagem").asString()).isEqualTo("Kai");
		assertThat(doJogador.get("doMestre").asBoolean()).isFalse();
		assertThat(doJogador.get("rotulo").asString()).isEqualTo("Furtividade");
		assertThat(doJogador.get("total").asInt()).isEqualTo(25);
		assertThat(doJogador.get("detalhe").get("dados").isArray()).isTrue();
		assertThat(doJogador.get("detalhe").get("total").asInt()).isEqualTo(25);
		assertThat(doJogador.get("criadaEm").asString()).matches("\\d{4}-\\d{2}-\\d{2}T.*Z");
	}
}
