package dev.spironello.fichasrpg.controller;

import static dev.spironello.fichasrpg.CampanhasDeTeste.como;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import dev.spironello.fichasrpg.AutenticacaoDeTesteConfig;
import dev.spironello.fichasrpg.CampanhasDeTeste;
import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.campanha.Papel;
import dev.spironello.fichasrpg.repository.FichaRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * A segunda tabela da §4 da spec das campanhas e as regras de quem entra e sai (membros). Na campanha C, M é o mestre,
 * A (o usuário de teste padrão) e B são jogadores, e F não participa.
 */
@DisplayName("Permissões da campanha (mestre, jogador e quem está fora)")
class PermissoesDaCampanhaTest extends IntegrationTestBase {

	private static final long A = AutenticacaoDeTesteConfig.USUARIO_DE_TESTE_ID;
	private static final long M = 3_100_001L;
	private static final long B = 3_100_002L;
	private static final long F = 3_100_003L;

	private static final String EDICAO = "{\"nome\":\"Nova\",\"descricao\":\"Outra\"}";

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
		CampanhasDeTeste.usuario(jdbc, M, "Mestre");
		CampanhasDeTeste.usuario(jdbc, B, "Jogador B");
		CampanhasDeTeste.usuario(jdbc, F, "De fora");
		campanha = CampanhasDeTeste.campanha(jdbc, "Mesa");
		CampanhasDeTeste.participar(jdbc, campanha, M, Papel.MESTRE);
		CampanhasDeTeste.participar(jdbc, campanha, A, Papel.JOGADOR);
		CampanhasDeTeste.participar(jdbc, campanha, B, Papel.JOGADOR);
	}

	@AfterEach
	void limpar() {
		CampanhasDeTeste.limpar(jdbc, fichas, A, M, B, F);
	}

	private record Ator(String nome, long id) {
	}

	private static final Ator MESTRE = new Ator("o mestre (M)", M);
	private static final Ator JOGADOR = new Ator("jogador (A)", A);
	private static final Ator OUTRO_JOGADOR = new Ator("jogador (B)", B);
	private static final Ator FORA = new Ator("quem está fora (F)", F);

	private void conferir(List<String> divergencias, String rota, Ator ator, int esperado, MockHttpServletRequestBuilder pedido)
			throws Exception {
		int obtido = mockMvc.perform(pedido.with(como(ator.id()))).andReturn().getResponse().getStatus();
		if (obtido != esperado) {
			divergencias.add(rota + " como " + ator.nome() + ": esperava " + esperado + " e veio " + obtido);
		}
	}

	private MockHttpServletRequestBuilder comCorpo(MockHttpServletRequestBuilder pedido, String corpo) {
		return pedido.contentType(MediaType.APPLICATION_JSON).content(corpo);
	}

	@Test
	@DisplayName("ver a campanha, os membros e as fichas da mesa: o mestre e os jogadores veem; quem está fora recebe 404")
	void ler() throws Exception {
		List<String> divergencias = new ArrayList<>();
		for (Ator ator : List.of(MESTRE, JOGADOR, OUTRO_JOGADOR)) {
			conferir(divergencias, "GET /campanhas/{id}", ator, 200, get("/campanhas/{id}", campanha));
			conferir(divergencias, "GET membros", ator, 200, get("/campanhas/{id}/membros", campanha));
			conferir(divergencias, "GET fichas", ator, 200, get("/campanhas/{id}/fichas", campanha));
		}
		conferir(divergencias, "GET /campanhas/{id}", FORA, 404, get("/campanhas/{id}", campanha));
		conferir(divergencias, "GET membros", FORA, 404, get("/campanhas/{id}/membros", campanha));
		conferir(divergencias, "GET fichas", FORA, 404, get("/campanhas/{id}/fichas", campanha));
		assertThat(divergencias).isEmpty();
	}

	@Test
	@DisplayName("criar ficha na campanha: o mestre e os jogadores criam (201); quem está fora recebe 404")
	void criarFicha() throws Exception {
		String corpo = json.writeValueAsString(FichasDeTeste.corpoOrdem());
		List<String> divergencias = new ArrayList<>();
		conferir(divergencias, "POST fichas", MESTRE, 201, comCorpo(post("/campanhas/{id}/fichas", campanha), corpo));
		conferir(divergencias, "POST fichas", JOGADOR, 201, comCorpo(post("/campanhas/{id}/fichas", campanha), corpo));
		conferir(divergencias, "POST fichas", FORA, 404, comCorpo(post("/campanhas/{id}/fichas", campanha), corpo));
		assertThat(divergencias).isEmpty();
	}

	@Test
	@DisplayName("editar, excluir e remover jogador: só o mestre; o jogador recebe 403 e quem está fora, 404")
	void soOMestre() throws Exception {
		List<String> divergencias = new ArrayList<>();
		for (Ator ator : List.of(JOGADOR, OUTRO_JOGADOR)) {
			conferir(divergencias, "PUT /campanhas/{id}", ator, 403, comCorpo(put("/campanhas/{id}", campanha), EDICAO));
			conferir(divergencias, "DELETE /campanhas/{id}", ator, 403, delete("/campanhas/{id}", campanha));
		}
		conferir(divergencias, "DELETE membro (jogador remove outro)", JOGADOR, 403, delete("/campanhas/{id}/membros/{u}", campanha, B));
		conferir(divergencias, "PUT /campanhas/{id}", FORA, 404, comCorpo(put("/campanhas/{id}", campanha), EDICAO));
		conferir(divergencias, "DELETE /campanhas/{id}", FORA, 404, delete("/campanhas/{id}", campanha));
		conferir(divergencias, "DELETE membro", FORA, 404, delete("/campanhas/{id}/membros/{u}", campanha, B));
		assertThat(divergencias).isEmpty();
		// nada mudou
		assertThat(jdbc.queryForObject("select nome from campanha where id = ?", String.class, campanha)).isEqualTo("Mesa");
		assertThat(jdbc.queryForObject("select count(*) from participacao where campanha_id = ?", Integer.class, campanha)).isEqualTo(3);
	}

	@Test
	@DisplayName("o mestre edita (200), remove um jogador (204) e exclui a campanha (204)")
	void mestrePode() throws Exception {
		List<String> divergencias = new ArrayList<>();
		conferir(divergencias, "PUT /campanhas/{id}", MESTRE, 200, comCorpo(put("/campanhas/{id}", campanha), EDICAO));
		conferir(divergencias, "DELETE membro", MESTRE, 204, delete("/campanhas/{id}/membros/{u}", campanha, B));
		conferir(divergencias, "DELETE /campanhas/{id}", MESTRE, 204, delete("/campanhas/{id}", campanha));
		assertThat(divergencias).isEmpty();
	}

	@Test
	@DisplayName("o convite só aparece para o mestre: o jogador recebe convite nulo no GET da campanha")
	void conviteSoParaOMestre() throws Exception {
		mockMvc.perform(get("/campanhas/{id}", campanha).with(como(M))).andExpect(status().isOk())
				.andExpect(jsonPath("$.meuPapel").value("MESTRE")).andExpect(jsonPath("$.convite.codigo").isString())
				.andExpect(jsonPath("$.convite.ativo").value(true));
		mockMvc.perform(get("/campanhas/{id}", campanha).with(como(A))).andExpect(status().isOk())
				.andExpect(jsonPath("$.meuPapel").value("JOGADOR")).andExpect(jsonPath("$.convite").doesNotExist());
	}

	@Test
	@DisplayName("sair: o jogador sai (204); o mestre recebe 409 com a mensagem do contrato; quem está fora, 404")
	void sair() throws Exception {
		mockMvc.perform(delete("/campanhas/{id}/membros/{u}", campanha, M).with(como(M)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("O mestre não sai da campanha. Para encerrá-la, exclua a campanha."));
		mockMvc.perform(delete("/campanhas/{id}/membros/{u}", campanha, F).with(como(F))).andExpect(status().isNotFound());
		mockMvc.perform(delete("/campanhas/{id}/membros/{u}", campanha, B).with(como(B))).andExpect(status().isNoContent());
		assertThat(jdbc.queryForObject("select count(*) from participacao where campanha_id = ? and usuario_id = ?", Integer.class, campanha, B))
				.isZero();
		// depois de sair, a campanha deixa de existir para ele
		mockMvc.perform(get("/campanhas/{id}", campanha).with(como(B))).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("o alvo é o mestre: 409 se quem pede é o mestre, 403 se é um jogador; o alvo que não é membro dá 404")
	void alvoDaRemocao() throws Exception {
		List<String> divergencias = new ArrayList<>();
		conferir(divergencias, "mestre remove o mestre", MESTRE, 409, delete("/campanhas/{id}/membros/{u}", campanha, M));
		conferir(divergencias, "jogador remove o mestre", JOGADOR, 403, delete("/campanhas/{id}/membros/{u}", campanha, M));
		conferir(divergencias, "jogador remove outro jogador", JOGADOR, 403, delete("/campanhas/{id}/membros/{u}", campanha, B));
		conferir(divergencias, "mestre remove quem não é membro", MESTRE, 404, delete("/campanhas/{id}/membros/{u}", campanha, F));
		assertThat(divergencias).isEmpty();
		assertThat(jdbc.queryForObject("select count(*) from participacao where campanha_id = ?", Integer.class, campanha)).isEqualTo(3);
	}

	@Test
	@DisplayName("sem token todas as rotas de campanha dão 401")
	void semToken() throws Exception {
		List<MockHttpServletRequestBuilder> pedidos = List.of(
				get("/campanhas"), post("/campanhas"), get("/campanhas/{id}", campanha), put("/campanhas/{id}", campanha),
				delete("/campanhas/{id}", campanha), get("/campanhas/{id}/membros", campanha),
				delete("/campanhas/{id}/membros/{u}", campanha, B), get("/campanhas/{id}/fichas", campanha),
				post("/campanhas/{id}/fichas", campanha), post("/campanhas/{id}/convite", campanha),
				delete("/campanhas/{id}/convite", campanha), post("/convites/{codigo}/aceitar", "QUALQUERCOISA"));
		List<String> divergencias = new ArrayList<>();
		for (MockHttpServletRequestBuilder pedido : pedidos) {
			var resposta = mockMvc.perform(pedido.with(anonymous())).andReturn().getResponse();
			if (resposta.getStatus() != 401) {
				divergencias.add("esperava 401 e veio " + resposta.getStatus());
			}
		}
		assertThat(divergencias).isEmpty();
	}
}
