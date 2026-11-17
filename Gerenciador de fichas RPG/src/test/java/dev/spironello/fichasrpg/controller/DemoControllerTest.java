package dev.spironello.fichasrpg.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import dev.spironello.fichasrpg.CampanhasDeTeste;
import dev.spironello.fichasrpg.ContasDemoDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.campanha.Papel;

/**
 * POST /auth/demo com o perfil demo ligado (2.6, spec §5.4): sem corpo cria o visitante, a mesa e as duas fichas; com
 * o código de convite de uma mesa demo cria o jogador. Os pedidos usam os tokens REAIS que a resposta devolve.
 */
@ActiveProfiles("demo")
@DisplayName("POST /auth/demo (perfil demo)")
class DemoControllerTest extends IntegrationTestBase {

	private static final long MESTRE_REAL = 3_700_001L;

	@Autowired
	private WebApplicationContext contexto;
	@Autowired
	private JdbcTemplate jdbc;

	private MockMvc mvc;

	@BeforeEach
	void montarMvc() {
		// A base deixa o usuário de teste no contexto; aqui os pedidos chegam anônimos, só com o token real.
		TestSecurityContextHolder.clearContext();
		mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
		ContasDemoDeTeste.apagarTodas(jdbc);
	}

	@AfterEach
	void limpar() {
		ContasDemoDeTeste.apagarTodas(jdbc);
		jdbc.update("delete from usuario where id = ?", MESTRE_REAL);
	}

	@Test
	@DisplayName("sem corpo: 200 com os tokens, o usuário demo 'Visitante NNNN' e o id da campanha")
	void semConvite() throws Exception {
		mvc.perform(post("/auth/demo"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken", notNullValue()))
				.andExpect(jsonPath("$.refreshToken", notNullValue()))
				.andExpect(jsonPath("$.campanhaId", notNullValue()))
				.andExpect(jsonPath("$.usuario.demo").value(true))
				.andExpect(jsonPath("$.usuario.nome", matchesPattern("Visitante \\d{4}")));
	}

	@Test
	@DisplayName("o visitante é uma conta sem senha, sem Google e com e-mail interno (.invalid); não entra por /auth/login")
	void contaSemComoEntrar() throws Exception {
		Map<String, Object> demo = entrarSemConvite();

		Map<String, Object> usuario = jdbc.queryForMap("select email, senha_hash, google_sub, demo from usuario where id = ?", demo.get("usuarioId"));
		assertThat(usuario.get("senha_hash")).isNull();
		assertThat(usuario.get("google_sub")).isNull();
		assertThat(usuario.get("demo")).isEqualTo(true);
		assertThat((String) usuario.get("email")).endsWith("@demo.invalid");

		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + usuario.get("email") + "\",\"senha\":\"qualquer-senha-longa\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("cria a campanha 'Mesa de demonstração' com o visitante de mestre e duas fichas preenchidas")
	void mesaEFichas() throws Exception {
		Map<String, Object> demo = entrarSemConvite();
		long campanha = (long) demo.get("campanhaId");
		long usuario = (long) demo.get("usuarioId");

		assertThat(jdbc.queryForObject("select nome from campanha where id = ?", String.class, campanha)).isEqualTo("Mesa de demonstração");
		assertThat(jdbc.queryForObject("select papel from participacao where campanha_id = ? and usuario_id = ?", String.class, campanha, usuario))
				.isEqualTo("MESTRE");
		List<Map<String, Object>> fichas = jdbc.queryForList("select id, sistema from ficha where dono_id = ? and campanha_id = ? order by id", usuario, campanha);
		assertThat(fichas).extracting(f -> f.get("sistema")).containsExactly("ORDEM_PARANORMAL", "DND");

		long ordem = (long) fichas.get(0).get("id");
		long dnd = (long) fichas.get(1).get("id");
		assertThat(contar("ficha_ordem_pericia", ordem)).isPositive();
		assertThat(contar("ficha_ordem_item", ordem)).isEqualTo(1);
		assertThat(contar("ficha_ordem_ataque", ordem)).isEqualTo(1);
		assertThat(contar("ficha_habilidade", ordem)).isEqualTo(1);
		assertThat(contar("ficha_dnd_pericia", dnd)).isPositive();
		assertThat(contar("ficha_dnd_item", dnd)).isEqualTo(1);
		assertThat(contar("ficha_dnd_ataque", dnd)).isEqualTo(1);
		assertThat(contar("ficha_habilidade", dnd)).isEqualTo(1);
	}

	@Test
	@DisplayName("com o token devolvido, o visitante lê a mesa e as duas fichas pela API normal (e /me diz demo)")
	void tokenFuncionaNaApi() throws Exception {
		Map<String, Object> demo = entrarSemConvite();
		String bearer = "Bearer " + demo.get("accessToken");
		long campanha = (long) demo.get("campanhaId");

		mvc.perform(get("/me").header("Authorization", bearer)).andExpect(status().isOk()).andExpect(jsonPath("$.demo").value(true));
		mvc.perform(get("/campanhas/{id}", campanha).header("Authorization", bearer)).andExpect(status().isOk())
				.andExpect(jsonPath("$.meuPapel").value("MESTRE")).andExpect(jsonPath("$.convite.codigo", notNullValue()));
		mvc.perform(get("/campanhas/{id}/fichas", campanha).header("Authorization", bearer)).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
		for (Long id : jdbc.queryForList("select id from ficha where campanha_id = ?", Long.class, campanha)) {
			mvc.perform(get("/fichas/{id}", id).header("Authorization", bearer)).andExpect(status().isOk());
		}
	}

	@Test
	@DisplayName("com o código de uma mesa demo: cria o jogador demo, membro da mesa, com uma ficha própria nela")
	void comConvite() throws Exception {
		Map<String, Object> mestre = entrarSemConvite();
		long campanha = (long) mestre.get("campanhaId");
		String codigo = jdbc.queryForObject("select codigo_convite from campanha where id = ?", String.class, campanha);

		String corpo = mvc.perform(post("/auth/demo").contentType(MediaType.APPLICATION_JSON).content("{\"codigoConvite\":\"" + codigo + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.campanhaId").value(campanha))
				.andExpect(jsonPath("$.usuario.demo").value(true))
				.andReturn().getResponse().getContentAsString();
		long jogador = JsonPath.<Number>read(corpo, "$.usuario.id").longValue();
		assertThat(jogador).isNotEqualTo((long) mestre.get("usuarioId"));

		assertThat(jdbc.queryForObject("select papel from participacao where campanha_id = ? and usuario_id = ?", String.class, campanha, jogador))
				.isEqualTo("JOGADOR");
		assertThat(jdbc.queryForObject("select count(*) from ficha where dono_id = ? and campanha_id = ?", Integer.class, jogador, campanha)).isEqualTo(1);
		assertThat(jdbc.queryForObject("select count(*) from campanha", Integer.class)).isEqualTo(1);

		// o mestre vê as 3 fichas da mesa; o jogador, só a dele (as do mestre ficam ocultas)
		String bearerMestre = "Bearer " + mestre.get("accessToken");
		String bearerJogador = "Bearer " + JsonPath.read(corpo, "$.accessToken");
		mvc.perform(get("/campanhas/{id}/fichas", campanha).header("Authorization", bearerMestre)).andExpect(jsonPath("$.length()").value(3));
		mvc.perform(get("/campanhas/{id}/fichas", campanha).header("Authorization", bearerJogador)).andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	@DisplayName("convite de campanha real, desativado ou inexistente: 404, e nenhuma conta demo é criada")
	void conviteRecusado() throws Exception {
		CampanhasDeTeste.usuario(jdbc, MESTRE_REAL, "Mestre real");
		long real = CampanhasDeTeste.campanha(jdbc, "Mesa real");
		CampanhasDeTeste.participar(jdbc, real, MESTRE_REAL, Papel.MESTRE);
		String codigoReal = jdbc.queryForObject("select codigo_convite from campanha where id = ?", String.class, real);

		mvc.perform(post("/auth/demo").contentType(MediaType.APPLICATION_JSON).content("{\"codigoConvite\":\"" + codigoReal + "\"}"))
				.andExpect(status().isNotFound());
		assertThat(jdbc.queryForObject("select count(*) from usuario where demo", Integer.class)).isZero();

		Map<String, Object> mestre = entrarSemConvite();
		jdbc.update("update campanha set convite_ativo = false where id = ?", mestre.get("campanhaId"));
		String codigoDesativado = jdbc.queryForObject("select codigo_convite from campanha where id = ?", String.class, mestre.get("campanhaId"));
		for (String codigo : List.of(codigoDesativado, "NAOEXISTE123", "curto")) {
			mvc.perform(post("/auth/demo").contentType(MediaType.APPLICATION_JSON).content("{\"codigoConvite\":\"" + codigo + "\"}"))
					.andExpect(status().isNotFound());
		}
		assertThat(jdbc.queryForObject("select count(*) from usuario where demo", Integer.class)).isEqualTo(1);
		assertThat(jdbc.queryForObject("select count(*) from participacao where usuario_id = ?", Integer.class, MESTRE_REAL)).isEqualTo(1);
	}

	@Test
	@DisplayName("corpo com chave desconhecida dá 400 (JSON estrito)")
	void corpoEstrito() throws Exception {
		mvc.perform(post("/auth/demo").contentType(MediaType.APPLICATION_JSON).content("{\"outro\":1}"))
				.andExpect(status().isBadRequest());
	}

	private Map<String, Object> entrarSemConvite() throws Exception {
		String corpo = mvc.perform(post("/auth/demo")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return Map.of(
				"accessToken", JsonPath.<String>read(corpo, "$.accessToken"),
				"campanhaId", JsonPath.<Number>read(corpo, "$.campanhaId").longValue(),
				"usuarioId", JsonPath.<Number>read(corpo, "$.usuario.id").longValue());
	}

	private int contar(String tabela, long fichaId) {
		return jdbc.queryForObject("select count(*) from " + tabela + " where ficha_id = ?", Integer.class, fichaId);
	}
}
