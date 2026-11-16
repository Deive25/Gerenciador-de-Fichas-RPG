package dev.spironello.fichasrpg.controller;

import static dev.spironello.fichasrpg.CampanhasDeTeste.como;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

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

/**
 * O convite por link (§5 da spec das campanhas): renovar, desativar, a prévia pública e aceitar. Na campanha C, M é o
 * mestre, A (o usuário de teste padrão) é jogador, e F (de fora) só entra pelo link.
 */
@DisplayName("Convite por link")
class ConviteTest extends IntegrationTestBase {

	private static final long A = AutenticacaoDeTesteConfig.USUARIO_DE_TESTE_ID;
	private static final long M = 3_500_001L;
	private static final long F = 3_500_003L;

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private FichaRepository fichas;
	@Autowired
	private JdbcTemplate jdbc;

	private long campanha;
	private String codigo;

	@BeforeEach
	void prepararMesa() {
		CampanhasDeTeste.usuario(jdbc, M, "Mestre Marta");
		CampanhasDeTeste.usuario(jdbc, F, "De fora");
		campanha = CampanhasDeTeste.campanha(jdbc, "Mesa de sexta");
		CampanhasDeTeste.participar(jdbc, campanha, M, Papel.MESTRE);
		CampanhasDeTeste.participar(jdbc, campanha, A, Papel.JOGADOR);
		codigo = codigoAtual();
	}

	@AfterEach
	void limpar() {
		CampanhasDeTeste.limpar(jdbc, fichas, A, M, F);
	}

	private String codigoAtual() {
		return jdbc.queryForObject("select codigo_convite from campanha where id = ?", String.class, campanha);
	}

	private String renovar() throws Exception {
		return JsonPath.read(mockMvc.perform(post("/campanhas/{id}/convite", campanha).with(como(M))).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(), "$.codigo");
	}

	@Test
	@DisplayName("gerenciar o convite: só o mestre; o jogador recebe 403 e quem está fora, 404")
	void soOMestre() throws Exception {
		mockMvc.perform(post("/campanhas/{id}/convite", campanha)).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value("Você não tem permissão para isso nesta campanha."));
		mockMvc.perform(delete("/campanhas/{id}/convite", campanha)).andExpect(status().isForbidden());
		mockMvc.perform(post("/campanhas/{id}/convite", campanha).with(como(F))).andExpect(status().isNotFound());
		mockMvc.perform(delete("/campanhas/{id}/convite", campanha).with(como(F))).andExpect(status().isNotFound());
		assertThat(codigoAtual()).isEqualTo(codigo);
		assertThat(jdbc.queryForObject("select convite_ativo from campanha where id = ?", Boolean.class, campanha)).isTrue();
	}

	@Test
	@DisplayName("a prévia funciona sem token e mostra só os nomes da campanha e do mestre")
	void previaPublica() throws Exception {
		String corpo = mockMvc.perform(get("/convites/{codigo}", codigo).with(anonymous())).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$").keySet()).containsExactlyInAnyOrder("campanhaId", "campanha", "mestre");
		assertThat(JsonPath.<Number>read(corpo, "$.campanhaId").longValue()).isEqualTo(campanha);
		assertThat(JsonPath.<String>read(corpo, "$.campanha")).isEqualTo("Mesa de sexta");
		assertThat(JsonPath.<String>read(corpo, "$.mestre")).isEqualTo("Mestre Marta");
		assertThat(corpo).doesNotContain("email").doesNotContain("@fichas-rpg.invalid");
	}

	@Test
	@DisplayName("código inexistente ou malformado: a prévia e o aceite dão 404 'Convite inválido ou desativado'")
	void codigoInvalido() throws Exception {
		for (String invalido : new String[] { "NAOEXISTE123", "curto", "x".repeat(40), "a.b-c_d", "com espaco" }) {
			mockMvc.perform(get("/convites/{codigo}", invalido).with(anonymous())).andExpect(status().isNotFound())
					.andExpect(jsonPath("$.title").value("Convite inválido ou desativado"))
					.andExpect(jsonPath("$.detail").value("Convite inválido ou desativado"));
			mockMvc.perform(post("/convites/{codigo}/aceitar", invalido).with(como(F))).andExpect(status().isNotFound())
					.andExpect(jsonPath("$.title").value("Convite inválido ou desativado"));
		}
	}

	@Test
	@DisplayName("aceitar: 201 com Location e entra como jogador; a segunda vez dá 200 e continua uma participação só")
	void aceitar() throws Exception {
		mockMvc.perform(post("/convites/{codigo}/aceitar", codigo).with(como(F))).andExpect(status().isCreated())
				.andExpect(header().string("Location", "/campanhas/" + campanha)).andExpect(jsonPath("$.campanhaId").value(campanha));
		assertThat(jdbc.queryForObject("select papel from participacao where campanha_id = ? and usuario_id = ?", String.class, campanha, F))
				.isEqualTo("JOGADOR");

		mockMvc.perform(post("/convites/{codigo}/aceitar", codigo).with(como(F))).andExpect(status().isOk())
				.andExpect(jsonPath("$.campanhaId").value(campanha));
		assertThat(jdbc.queryForObject("select count(*) from participacao where campanha_id = ? and usuario_id = ?", Integer.class, campanha, F))
				.isEqualTo(1);

		// já dentro, ele vê a campanha como jogador (e sem o convite)
		mockMvc.perform(get("/campanhas/{id}", campanha).with(como(F))).andExpect(status().isOk())
				.andExpect(jsonPath("$.meuPapel").value("JOGADOR")).andExpect(jsonPath("$.convite").doesNotExist());
	}

	@Test
	@DisplayName("quem já é membro recebe 200, inclusive o mestre, e o papel dele não muda")
	void jaEMembro() throws Exception {
		mockMvc.perform(post("/convites/{codigo}/aceitar", codigo).with(como(M))).andExpect(status().isOk())
				.andExpect(jsonPath("$.campanhaId").value(campanha));
		mockMvc.perform(post("/convites/{codigo}/aceitar", codigo)).andExpect(status().isOk());
		assertThat(jdbc.queryForObject("select papel from participacao where campanha_id = ? and usuario_id = ?", String.class, campanha, M))
				.isEqualTo("MESTRE");
		assertThat(jdbc.queryForObject("select count(*) from participacao where campanha_id = ?", Integer.class, campanha)).isEqualTo(2);
	}

	@Test
	@DisplayName("Foco 3: renovar derruba o código antigo (prévia e aceitar dão 404), inclusive para quem já foi membro")
	void renovarDerrubaOAntigo() throws Exception {
		mockMvc.perform(post("/convites/{codigo}/aceitar", codigo).with(como(F))).andExpect(status().isCreated());
		mockMvc.perform(delete("/campanhas/{id}/membros/{u}", campanha, F).with(como(M))).andExpect(status().isNoContent());

		String novo = renovar();

		assertThat(novo).isNotEqualTo(codigo).matches("[A-Za-z0-9]{12}");
		assertThat(codigoAtual()).isEqualTo(novo);
		mockMvc.perform(get("/convites/{codigo}", codigo).with(anonymous())).andExpect(status().isNotFound());
		mockMvc.perform(post("/convites/{codigo}/aceitar", codigo).with(como(F))).andExpect(status().isNotFound());
		// quem já é membro (A) também recebe 404 com o código antigo, e não o 200 de "já está na campanha"
		mockMvc.perform(post("/convites/{codigo}/aceitar", codigo)).andExpect(status().isNotFound());
		assertThat(jdbc.queryForObject("select count(*) from participacao where campanha_id = ? and usuario_id = ?", Integer.class, campanha, F))
				.as("o jogador removido não voltou pelo código antigo").isZero();

		mockMvc.perform(get("/convites/{codigo}", novo).with(anonymous())).andExpect(status().isOk());
		mockMvc.perform(post("/convites/{codigo}/aceitar", novo).with(como(F))).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("desativar: 204; a prévia e o aceite dão 404, o mestre vê convite inativo, e renovar ativa de novo com outro código")
	void desativar() throws Exception {
		mockMvc.perform(delete("/campanhas/{id}/convite", campanha).with(como(M))).andExpect(status().isNoContent());

		mockMvc.perform(get("/convites/{codigo}", codigo).with(anonymous())).andExpect(status().isNotFound());
		mockMvc.perform(post("/convites/{codigo}/aceitar", codigo).with(como(F))).andExpect(status().isNotFound());
		mockMvc.perform(get("/campanhas/{id}", campanha).with(como(M))).andExpect(status().isOk())
				.andExpect(jsonPath("$.convite.ativo").value(false)).andExpect(jsonPath("$.convite.codigo").value(codigo));

		mockMvc.perform(post("/campanhas/{id}/convite", campanha).with(como(M))).andExpect(status().isOk())
				.andExpect(jsonPath("$.ativo").value(true));
		assertThat(codigoAtual()).isNotEqualTo(codigo);
		mockMvc.perform(get("/convites/{codigo}", codigoAtual()).with(anonymous())).andExpect(status().isOk());
	}

	@Test
	@DisplayName("o código de POST /campanhas/{id}/convite tem 12 caracteres de A-Z, a-z e 0-9 e o corpo é {codigo, ativo}")
	void formatoDoCodigo() throws Exception {
		String corpo = mockMvc.perform(post("/campanhas/{id}/convite", campanha).with(como(M))).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$").keySet()).containsExactlyInAnyOrder("codigo", "ativo");
		assertThat(JsonPath.<String>read(corpo, "$.codigo")).matches("[A-Za-z0-9]{12}");
		assertThat(JsonPath.<Boolean>read(corpo, "$.ativo")).isTrue();
	}

	@Test
	@DisplayName("aceitar sem token dá 401")
	void aceitarSemToken() throws Exception {
		mockMvc.perform(post("/convites/{codigo}/aceitar", codigo).with(anonymous())).andExpect(status().isUnauthorized());
	}
}
