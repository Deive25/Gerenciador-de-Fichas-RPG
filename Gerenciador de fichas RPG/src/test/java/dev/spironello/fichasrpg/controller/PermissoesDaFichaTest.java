package dev.spironello.fichasrpg.controller;

import static dev.spironello.fichasrpg.CampanhasDeTeste.como;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import dev.spironello.fichasrpg.AutenticacaoDeTesteConfig;
import dev.spironello.fichasrpg.CampanhasDeTeste;
import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.campanha.Papel;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.repository.FichaRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * A primeira tabela da §4 da spec das campanhas, célula por célula. Na campanha C, M é o mestre, A (o usuário de teste
 * padrão) e B são jogadores, e F não participa. Cada pessoa age em cada ficha por uma rota de leitura (GET), uma de
 * escrita (PUT e POST de item), a do retrato, a de excluir e a de trocar de campanha.
 */
@DisplayName("Permissões da ficha (dono, mestre, outro jogador e quem está fora)")
class PermissoesDaFichaTest extends IntegrationTestBase {

	private static final long A = AutenticacaoDeTesteConfig.USUARIO_DE_TESTE_ID;
	private static final long M = 3_000_001L;
	private static final long B = 3_000_002L;
	private static final long F = 3_000_003L;

	private static final String ITEM = "{\"nome\":\"X\",\"categoria\":0,\"espacos\":1,\"quantidade\":1}";
	private static final String ROLAGEM = "{\"expressao\":\"1d6\"}";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private FichaRepository fichas;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private ObjectMapper json;

	private long campanha;
	private long fichaDeA;
	private long fichaDeM;
	private long fichaDeB;
	private long fichaSemCampanhaDeA;

	@BeforeEach
	void prepararMesa() {
		CampanhasDeTeste.usuario(jdbc, M, "Mestre");
		CampanhasDeTeste.usuario(jdbc, B, "Jogador B");
		CampanhasDeTeste.usuario(jdbc, F, "De fora");
		campanha = CampanhasDeTeste.campanha(jdbc, "Mesa");
		CampanhasDeTeste.participar(jdbc, campanha, M, Papel.MESTRE);
		CampanhasDeTeste.participar(jdbc, campanha, A, Papel.JOGADOR);
		CampanhasDeTeste.participar(jdbc, campanha, B, Papel.JOGADOR);
		fichaDeA = ficha(A, "De A", campanha);
		fichaDeM = ficha(M, "De M", campanha);
		fichaDeB = ficha(B, "De B", campanha);
		fichaSemCampanhaDeA = ficha(A, "Solta de A", null);
	}

	@AfterEach
	void limpar() {
		CampanhasDeTeste.limpar(jdbc, fichas, A, M, B, F);
	}

	private long ficha(long dono, String personagem, Long campanhaId) {
		Ficha ficha = FichasDeTeste.ordem(personagem);
		ficha.setDonoId(dono);
		ficha.setCampanhaId(campanhaId);
		long id = fichas.saveAndFlush(ficha).getId();
		jdbc.update("insert into retrato_ficha (ficha_id, conteudo, atualizado_em) values (?, ?, now())", id, new byte[] { 1, 2, 3 });
		return id;
	}

	private String corpoDoPut(long fichaId) throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("versao", jdbc.queryForObject("select versao from ficha where id = ?", Long.class, fichaId));
		return json.writeValueAsString(corpo);
	}

	private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder pedido, String corpo) {
		return pedido.contentType(MediaType.APPLICATION_JSON).content(corpo);
	}

	/** Quem age: o id e o post-processor (o dono padrão da base é o A). */
	private record Ator(String nome, long id) {
		RequestPostProcessor token() {
			return como(id);
		}
	}

	private record Rota(String nome, Function<Long, MockHttpServletRequestBuilder> pedido) {
	}

	private int codigo(MockHttpServletRequestBuilder pedido, Ator ator) throws Exception {
		return mockMvc.perform(pedido.with(ator.token())).andReturn().getResponse().getStatus();
	}

	/** As rotas da primeira tabela da §4 (menos "excluir" e "trocar de campanha", que mudam o estado e têm teste à parte). */
	private List<Rota> rotasDeLeituraEEscrita() {
		return List.of(
				new Rota("GET /fichas/{id}", id -> get("/fichas/{id}", id)),
				new Rota("PUT /fichas/{id}", id -> {
					try {
						return json(put("/fichas/{id}", id), corpoDoPut(id));
					} catch (Exception e) {
						throw new IllegalStateException(e);
					}
				}),
				new Rota("POST /fichas/{id}/itens", id -> json(post("/fichas/{id}/itens", id), ITEM)),
				new Rota("POST /fichas/{id}/rolagens", id -> json(post("/fichas/{id}/rolagens", id), ROLAGEM)));
	}

	private void conferir(List<String> divergencias, String rota, Ator ator, long fichaId, int esperado,
			MockHttpServletRequestBuilder pedido) throws Exception {
		int obtido = codigo(pedido, ator);
		if (obtido != esperado) {
			divergencias.add(rota + " da ficha " + fichaId + " como " + ator.nome() + ": esperava " + esperado + " e veio " + obtido);
		}
	}

	@Test
	@DisplayName("ficha de A numa campanha: o dono e o mestre leem e editam; o outro jogador recebe 403; quem está fora recebe 404")
	void fichaDeUmJogador() throws Exception {
		List<String> divergencias = new ArrayList<>();
		Ator dono = new Ator("o dono (A)", A);
		Ator mestre = new Ator("o mestre (M)", M);
		Ator outro = new Ator("outro jogador (B)", B);
		Ator fora = new Ator("quem está fora (F)", F);

		for (Rota rota : rotasDeLeituraEEscrita()) {
			int criado = rota.nome().startsWith("POST /fichas/{id}/itens") ? 201 : 200;
			conferir(divergencias, rota.nome(), dono, fichaDeA, criado, rota.pedido().apply(fichaDeA));
			conferir(divergencias, rota.nome(), mestre, fichaDeA, criado, rota.pedido().apply(fichaDeA));
			conferir(divergencias, rota.nome(), outro, fichaDeA, 403, rota.pedido().apply(fichaDeA));
			conferir(divergencias, rota.nome(), fora, fichaDeA, 404, rota.pedido().apply(fichaDeA));
		}
		assertThat(divergencias).isEmpty();
	}

	@Test
	@DisplayName("o retrato: o dono, o mestre e o outro jogador baixam (o cartão do resumo mostra); quem está fora recebe 404; enviar e apagar só o dono e o mestre")
	void retrato() throws Exception {
		List<String> divergencias = new ArrayList<>();
		Ator dono = new Ator("o dono (A)", A);
		Ator mestre = new Ator("o mestre (M)", M);
		Ator outro = new Ator("outro jogador (B)", B);
		Ator fora = new Ator("quem está fora (F)", F);

		conferir(divergencias, "GET retrato", dono, fichaDeA, 200, get("/fichas/{id}/retrato", fichaDeA));
		conferir(divergencias, "GET retrato", mestre, fichaDeA, 200, get("/fichas/{id}/retrato", fichaDeA));
		conferir(divergencias, "GET retrato", outro, fichaDeA, 200, get("/fichas/{id}/retrato", fichaDeA));
		conferir(divergencias, "GET retrato", fora, fichaDeA, 404, get("/fichas/{id}/retrato", fichaDeA));

		conferir(divergencias, "DELETE retrato", outro, fichaDeA, 403, delete("/fichas/{id}/retrato", fichaDeA));
		conferir(divergencias, "DELETE retrato", fora, fichaDeA, 404, delete("/fichas/{id}/retrato", fichaDeA));
		conferir(divergencias, "DELETE retrato", mestre, fichaDeA, 204, delete("/fichas/{id}/retrato", fichaDeA));
		assertThat(divergencias).isEmpty();
	}

	@Test
	@DisplayName("excluir: só o dono (204); o mestre e o outro jogador recebem 403 e quem está fora, 404; a ficha continua lá")
	void excluir() throws Exception {
		List<String> divergencias = new ArrayList<>();
		conferir(divergencias, "DELETE", new Ator("o mestre (M)", M), fichaDeA, 403, delete("/fichas/{id}", fichaDeA));
		conferir(divergencias, "DELETE", new Ator("outro jogador (B)", B), fichaDeA, 403, delete("/fichas/{id}", fichaDeA));
		conferir(divergencias, "DELETE", new Ator("quem está fora (F)", F), fichaDeA, 404, delete("/fichas/{id}", fichaDeA));
		assertThat(divergencias).isEmpty();
		assertThat(fichas.existsById(fichaDeA)).isTrue();

		mockMvc.perform(delete("/fichas/{id}", fichaDeA).with(como(A))).andExpect(status().isNoContent());
		assertThat(fichas.existsById(fichaDeA)).isFalse();
	}

	@Test
	@DisplayName("levar a outra campanha ou tirar da campanha: só o dono; o mestre e o outro jogador recebem 403 e quem está fora, 404")
	void trocarDeCampanha() throws Exception {
		List<String> divergencias = new ArrayList<>();
		String tirar = "{\"campanhaId\":null}";
		conferir(divergencias, "PUT campanha", new Ator("o mestre (M)", M), fichaDeA, 403,
				json(put("/fichas/{id}/campanha", fichaDeA), tirar));
		conferir(divergencias, "PUT campanha", new Ator("outro jogador (B)", B), fichaDeA, 403,
				json(put("/fichas/{id}/campanha", fichaDeA), tirar));
		conferir(divergencias, "PUT campanha", new Ator("quem está fora (F)", F), fichaDeA, 404,
				json(put("/fichas/{id}/campanha", fichaDeA), tirar));
		assertThat(divergencias).isEmpty();
		assertThat(jdbc.queryForObject("select campanha_id from ficha where id = ?", Long.class, fichaDeA)).isEqualTo(campanha);

		mockMvc.perform(json(put("/fichas/{id}/campanha", fichaDeA), tirar).with(como(A)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.campanha").doesNotExist());
		mockMvc.perform(json(put("/fichas/{id}/campanha", fichaDeA), "{\"campanhaId\":" + campanha + "}").with(como(A)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.campanha.id").value(campanha))
				.andExpect(jsonPath("$.campanha.nome").value("Mesa")).andExpect(jsonPath("$.dono.nome").exists());
	}

	@Test
	@DisplayName("levar a ficha a uma campanha de que o dono não participa dá 404 'Campanha não encontrada'")
	void trocarParaCampanhaDeOutros() throws Exception {
		long alheia = CampanhasDeTeste.campanha(jdbc, "Alheia");
		CampanhasDeTeste.participar(jdbc, alheia, F, Papel.MESTRE);

		mockMvc.perform(json(put("/fichas/{id}/campanha", fichaSemCampanhaDeA), "{\"campanhaId\":" + alheia + "}").with(como(A)))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.title").value("Campanha não encontrada"));
		assertThat(jdbc.queryForObject("select campanha_id from ficha where id = ?", Long.class, fichaSemCampanhaDeA)).isNull();
	}

	@Test
	@DisplayName("Foco 1: a ficha do mestre dá 404 para o jogador em tudo (ler, editar, itens, rolagens, retrato, excluir e trocar de campanha)")
	void fichaDoMestreFicaOcultaParaOJogador() throws Exception {
		List<String> divergencias = new ArrayList<>();
		for (Ator jogador : List.of(new Ator("jogador A", A), new Ator("jogador B", B))) {
			for (Rota rota : rotasDeLeituraEEscrita()) {
				conferir(divergencias, rota.nome(), jogador, fichaDeM, 404, rota.pedido().apply(fichaDeM));
			}
			conferir(divergencias, "GET retrato", jogador, fichaDeM, 404, get("/fichas/{id}/retrato", fichaDeM));
			conferir(divergencias, "PUT retrato", jogador, fichaDeM, 404,
					put("/fichas/{id}/retrato", fichaDeM).contentType(MediaType.IMAGE_JPEG).content(new byte[] { 1 }));
			conferir(divergencias, "DELETE retrato", jogador, fichaDeM, 404, delete("/fichas/{id}/retrato", fichaDeM));
			conferir(divergencias, "DELETE", jogador, fichaDeM, 404, delete("/fichas/{id}", fichaDeM));
			conferir(divergencias, "PUT campanha", jogador, fichaDeM, 404,
					json(put("/fichas/{id}/campanha", fichaDeM), "{\"campanhaId\":null}"));
		}
		assertThat(divergencias).isEmpty();
		// o dono (o mestre) segue com acesso normal
		mockMvc.perform(get("/fichas/{id}", fichaDeM).with(como(M))).andExpect(status().isOk());
	}

	@Test
	@DisplayName("o mestre acessa a ficha de um jogador mas não a ficha de B que não está em campanha nenhuma; o mesmo vale para B na ficha solta de A")
	void fichaSemCampanhaSoParaODono() throws Exception {
		List<String> divergencias = new ArrayList<>();
		for (Rota rota : rotasDeLeituraEEscrita()) {
			conferir(divergencias, rota.nome(), new Ator("o dono (A)", A), fichaSemCampanhaDeA,
					rota.nome().startsWith("POST /fichas/{id}/itens") ? 201 : 200, rota.pedido().apply(fichaSemCampanhaDeA));
			conferir(divergencias, rota.nome(), new Ator("outro jogador (B)", B), fichaSemCampanhaDeA, 404,
					rota.pedido().apply(fichaSemCampanhaDeA));
			conferir(divergencias, rota.nome(), new Ator("o mestre (M)", M), fichaSemCampanhaDeA, 404,
					rota.pedido().apply(fichaSemCampanhaDeA));
		}
		conferir(divergencias, "GET retrato", new Ator("outro jogador (B)", B), fichaSemCampanhaDeA, 404,
				get("/fichas/{id}/retrato", fichaSemCampanhaDeA));
		assertThat(divergencias).isEmpty();
	}

	@Test
	@DisplayName("ficha que não existe dá 404 e sem token dá 401")
	void inexistenteESemToken() throws Exception {
		mockMvc.perform(get("/fichas/{id}", 987654321L).with(como(M))).andExpect(status().isNotFound());
		mockMvc.perform(get("/fichas/{id}", fichaDeA).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("GET /fichas (Minhas fichas) lista só as fichas de que quem está logado é dono, em campanha ou fora dela")
	void minhasFichas() throws Exception {
		String doMestre = mockMvc.perform(get("/fichas").with(como(M))).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(doMestre).contains("De M").doesNotContain("De A").doesNotContain("De B");

		String deA = mockMvc.perform(get("/fichas")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertThat(deA).contains("De A").contains("Solta de A").doesNotContain("De M").doesNotContain("De B");
	}
}
