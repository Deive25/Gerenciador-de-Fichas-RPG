package dev.spironello.fichasrpg.controller;

import static dev.spironello.fichasrpg.CampanhasDeTeste.como;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import dev.spironello.fichasrpg.AutenticacaoDeTesteConfig;
import dev.spironello.fichasrpg.CampanhasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.campanha.Papel;
import dev.spironello.fichasrpg.repository.FichaRepository;

/**
 * O que a conta demo muda no resto da API, com ou sem o perfil (2.6): o campo {@code demo} no /me e na prévia do
 * convite, e a regra de que uma conta demo nunca entra numa campanha de gente real. RM é o mestre real, DM o mestre
 * demo, V o visitante demo e A (o usuário de teste padrão) uma conta real.
 */
@DisplayName("Conta demo (campo demo e isolamento das campanhas reais)")
class ContaDemoTest extends IntegrationTestBase {

	private static final long A = AutenticacaoDeTesteConfig.USUARIO_DE_TESTE_ID;
	private static final long RM = 3_600_001L;
	private static final long DM = 3_600_002L;
	private static final long V = 3_600_003L;

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private FichaRepository fichas;

	private long campanhaReal;
	private long campanhaDemo;

	@BeforeEach
	void prepararMesas() {
		CampanhasDeTeste.usuario(jdbc, RM, "Mestre real");
		CampanhasDeTeste.usuario(jdbc, DM, "Visitante 1111");
		CampanhasDeTeste.usuario(jdbc, V, "Visitante 2222");
		jdbc.update("update usuario set demo = true where id in (?, ?)", DM, V);
		campanhaReal = CampanhasDeTeste.campanha(jdbc, "Mesa real");
		CampanhasDeTeste.participar(jdbc, campanhaReal, RM, Papel.MESTRE);
		campanhaDemo = CampanhasDeTeste.campanha(jdbc, "Mesa de demonstração");
		CampanhasDeTeste.participar(jdbc, campanhaDemo, DM, Papel.MESTRE);
	}

	@AfterEach
	void limpar() {
		CampanhasDeTeste.limpar(jdbc, fichas, A, RM, DM, V);
	}

	@Test
	@DisplayName("a V14 deixa demo = false nas contas que já existem e nas novas sem o campo")
	void padraoFalso() {
		assertThat(jdbc.queryForObject("select demo from usuario where id = ?", Boolean.class, RM)).isFalse();
		assertThat(jdbc.queryForObject("select demo from usuario where id = ?", Boolean.class, A)).isFalse();
	}

	@Test
	@DisplayName("/me diz se a conta é demo")
	void meExpoeDemo() throws Exception {
		mockMvc.perform(get("/me")).andExpect(status().isOk()).andExpect(jsonPath("$.demo").value(false));
		mockMvc.perform(get("/me").with(como(V))).andExpect(status().isOk()).andExpect(jsonPath("$.demo").value(true));
	}

	@Test
	@DisplayName("a prévia do convite traz demo = true quando o mestre é demo (e omite a chave numa campanha real)")
	void previaExpoeDemo() throws Exception {
		mockMvc.perform(get("/convites/{codigo}", codigo(campanhaReal)).with(anonymous()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.demo").doesNotExist());
		mockMvc.perform(get("/convites/{codigo}", codigo(campanhaDemo)).with(anonymous()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.demo").value(true));
	}

	@Test
	@DisplayName("a conta demo não entra por convite numa campanha real: 404, igual a um código que não existe")
	void demoNaoEntraEmCampanhaReal() throws Exception {
		mockMvc.perform(post("/convites/{codigo}/aceitar", codigo(campanhaReal)).with(como(V)))
				.andExpect(status().isNotFound());

		assertThat(jdbc.queryForObject("select count(*) from participacao where usuario_id = ?", Integer.class, V)).isZero();
	}

	@Test
	@DisplayName("a conta demo entra por convite numa campanha demo")
	void demoEntraEmCampanhaDemo() throws Exception {
		mockMvc.perform(post("/convites/{codigo}/aceitar", codigo(campanhaDemo)).with(como(V)))
				.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("uma conta real continua entrando por convite em qualquer campanha, inclusive a demo")
	void realEntraEmQualquerCampanha() throws Exception {
		mockMvc.perform(post("/convites/{codigo}/aceitar", codigo(campanhaReal))).andExpect(status().isCreated());
		mockMvc.perform(post("/convites/{codigo}/aceitar", codigo(campanhaDemo))).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("sem participar de uma campanha real, a conta demo não a vê nem leva uma ficha para ela (404)")
	void demoNaoAcessaCampanhaRealPeloId() throws Exception {
		mockMvc.perform(get("/campanhas/{id}", campanhaReal).with(como(V))).andExpect(status().isNotFound());
		mockMvc.perform(get("/campanhas/{id}/fichas", campanhaReal).with(como(V))).andExpect(status().isNotFound());
	}

	private String codigo(long campanhaId) {
		return jdbc.queryForObject("select codigo_convite from campanha where id = ?", String.class, campanhaId);
	}
}
