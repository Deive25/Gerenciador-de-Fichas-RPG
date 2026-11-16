package dev.spironello.fichasrpg.controller;

import static dev.spironello.fichasrpg.CampanhasDeTeste.como;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import dev.spironello.fichasrpg.AutenticacaoDeTesteConfig;
import dev.spironello.fichasrpg.CampanhasDeTeste;
import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.campanha.Papel;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.repository.FichaRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * O contrato da §5 da spec das campanhas (rotas, corpos e códigos) fora a matriz de permissões: criar, listar, ler,
 * editar, excluir, membros e as fichas da mesa. M é o mestre, A (o usuário de teste padrão) e B são jogadores.
 */
@DisplayName("API /campanhas (contrato)")
class CampanhaApiTest extends IntegrationTestBase {

	private static final long A = AutenticacaoDeTesteConfig.USUARIO_DE_TESTE_ID;
	private static final long M = 3_200_001L;
	private static final long B = 3_200_002L;
	private static final long F = 3_200_003L;

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private FichaRepository fichas;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private ObjectMapper json;

	@BeforeEach
	void prepararUsuarios() {
		CampanhasDeTeste.usuario(jdbc, M, "Mestre");
		CampanhasDeTeste.usuario(jdbc, B, "Jogador B");
		CampanhasDeTeste.usuario(jdbc, F, "De fora");
	}

	@AfterEach
	void limpar() {
		CampanhasDeTeste.limpar(jdbc, fichas, A, M, B, F);
	}

	private long mesaComTresFichas() {
		long campanha = CampanhasDeTeste.campanha(jdbc, "Mesa");
		CampanhasDeTeste.participar(jdbc, campanha, M, Papel.MESTRE);
		CampanhasDeTeste.participar(jdbc, campanha, A, Papel.JOGADOR);
		CampanhasDeTeste.participar(jdbc, campanha, B, Papel.JOGADOR);
		ficha(M, "Do mestre", campanha);
		ficha(A, "De A", campanha);
		ficha(B, "De B", campanha);
		return campanha;
	}

	private long ficha(long dono, String personagem, Long campanha) {
		Ficha ficha = FichasDeTeste.ordem(personagem);
		ficha.setDonoId(dono);
		ficha.setCampanhaId(campanha);
		return fichas.saveAndFlush(ficha).getId();
	}

	private ResultActions criar(long como, String corpo) throws Exception {
		return mockMvc.perform(post("/campanhas").with(como(como)).contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	@Test
	@DisplayName("POST /campanhas: 201 com Location, o criador vira mestre e o convite nasce ativo; nome e descrição vêm sem espaços nas pontas")
	void criar() throws Exception {
		String resposta = criar(M, "{\"nome\":\"  Mesa de sexta  \",\"descricao\":\"  Uma noite longa  \"}")
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/campanhas/\\d+")))
				.andExpect(jsonPath("$.nome").value("Mesa de sexta"))
				.andExpect(jsonPath("$.descricao").value("Uma noite longa"))
				.andExpect(jsonPath("$.meuPapel").value("MESTRE"))
				.andExpect(jsonPath("$.mestre.id").value(M))
				.andExpect(jsonPath("$.mestre.nome").value("Mestre"))
				.andExpect(jsonPath("$.convite.ativo").value(true))
				.andReturn().getResponse().getContentAsString();

		assertThat(JsonPath.<String>read(resposta, "$.convite.codigo")).matches("[A-Za-z0-9]{12}");
		Long id = JsonPath.<Number>read(resposta, "$.id").longValue();
		// o corpo do POST é o mesmo do GET /campanhas/{id} (o front lê o id do corpo, o Location não passa pelo CORS)
		String lido = mockMvc.perform(get("/campanhas/{id}", id).with(como(M))).andReturn().getResponse().getContentAsString();
		assertThat(resposta).isEqualTo(lido);
		assertThat(jdbc.queryForObject("select papel from participacao where campanha_id = ? and usuario_id = ?", String.class, id, M))
				.isEqualTo("MESTRE");
	}

	@Test
	@DisplayName("POST /campanhas: a descrição é opcional e vazia vira null")
	void descricaoOpcional() throws Exception {
		criar(M, "{\"nome\":\"Mesa\"}").andExpect(status().isCreated()).andExpect(jsonPath("$.descricao").value((Object) null));
		criar(M, "{\"nome\":\"Mesa\",\"descricao\":\"   \"}").andExpect(status().isCreated())
				.andExpect(jsonPath("$.descricao").value((Object) null));
	}

	@Test
	@DisplayName("POST e PUT /campanhas: nome obrigatório de 1 a 100, descrição até 1000 e JSON estrito dão 400")
	void validacao() throws Exception {
		criar(M, "{\"descricao\":\"x\"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.nome").value("é obrigatório"));
		criar(M, "{\"nome\":\"   \"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.nome").exists());
		criar(M, "{\"nome\":\"" + "n".repeat(101) + "\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nome").value("deve ter no máximo 100 caracteres"));
		criar(M, "{\"nome\":\"" + "n".repeat(100) + "\"}").andExpect(status().isCreated());
		criar(M, "{\"nome\":\"Mesa\",\"descricao\":\"" + "d".repeat(1001) + "\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.descricao").value("deve ter no máximo 1000 caracteres"));
		criar(M, "{\"nome\":\"Mesa\",\"descricao\":\"" + "d".repeat(1000) + "\"}").andExpect(status().isCreated());
		criar(M, "{\"nome\":\"Mesa\",\"codigoConvite\":\"AAAAAAAAAAAA\"}").andExpect(status().isBadRequest());
		criar(M, "{\"nome\":\"Mesa\",\"id\":1}").andExpect(status().isBadRequest());

		long campanha = mesaComTresFichas();
		mockMvc.perform(put("/campanhas/{id}", campanha).with(como(M)).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.nome").exists());
	}

	@Test
	@DisplayName("GET /campanhas: as campanhas de que participa, com papel, mestre e a contagem das fichas que ele vê (sem as do mestre, para o jogador)")
	void listar() throws Exception {
		long campanha = mesaComTresFichas();
		long alheia = CampanhasDeTeste.campanha(jdbc, "Alheia");
		CampanhasDeTeste.participar(jdbc, alheia, F, Papel.MESTRE);

		String doMestre = mockMvc.perform(get("/campanhas").with(como(M))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<List<Object>>read(doMestre, "$")).hasSize(1);
		assertThat(JsonPath.<Map<String, Object>>read(doMestre, "$[0]").keySet()).containsExactlyInAnyOrder("id", "nome", "papel", "mestre", "fichas");
		assertThat(JsonPath.<Number>read(doMestre, "$[0].id").longValue()).isEqualTo(campanha);
		assertThat(JsonPath.<String>read(doMestre, "$[0].papel")).isEqualTo("MESTRE");
		assertThat(JsonPath.<Map<String, Object>>read(doMestre, "$[0].mestre").keySet()).containsExactlyInAnyOrder("id", "nome");
		assertThat(JsonPath.<Integer>read(doMestre, "$[0].fichas")).as("o mestre vê as 3 fichas").isEqualTo(3);

		mockMvc.perform(get("/campanhas")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].papel").value("JOGADOR"))
				.andExpect(jsonPath("$[0].mestre.nome").value("Mestre"))
				.andExpect(jsonPath("$[0].fichas").value(2));
		mockMvc.perform(get("/campanhas").with(como(F))).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].fichas").value(0));
	}

	@Test
	@DisplayName("GET /campanhas/{id}: as chaves do contrato, e a contagem de GET /campanhas é o tamanho de GET /campanhas/{id}/fichas")
	void buscar() throws Exception {
		long campanha = mesaComTresFichas();

		String corpo = mockMvc.perform(get("/campanhas/{id}", campanha).with(como(M))).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$").keySet())
				.containsExactlyInAnyOrder("id", "nome", "descricao", "meuPapel", "mestre", "convite");
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.convite").keySet()).containsExactlyInAnyOrder("codigo", "ativo");

		for (long usuario : new long[] { M, A, B }) {
			int contagem = JsonPath.<Integer>read(mockMvc.perform(get("/campanhas").with(como(usuario))).andReturn().getResponse().getContentAsString(),
					"$[0].fichas");
			int lista = JsonPath.<List<Object>>read(mockMvc.perform(get("/campanhas/{id}/fichas", campanha).with(como(usuario)))
					.andReturn().getResponse().getContentAsString(), "$").size();
			assertThat(contagem).as("usuário %d", usuario).isEqualTo(lista);
		}
	}

	@Test
	@DisplayName("PUT /campanhas/{id}: o mestre substitui nome e descrição e recebe o mesmo corpo do GET")
	void atualizar() throws Exception {
		long campanha = mesaComTresFichas();

		String resposta = mockMvc.perform(put("/campanhas/{id}", campanha).with(como(M)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Outro nome\",\"descricao\":null}")).andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Outro nome")).andExpect(jsonPath("$.descricao").value((Object) null))
				.andReturn().getResponse().getContentAsString();
		String lido = mockMvc.perform(get("/campanhas/{id}", campanha).with(como(M))).andReturn().getResponse().getContentAsString();
		assertThat(resposta).isEqualTo(lido);
	}

	@Test
	@DisplayName("DELETE /campanhas/{id}: 204, a campanha e as participações somem e as fichas voltam aos donos, sem campanha")
	void excluir() throws Exception {
		long campanha = mesaComTresFichas();

		mockMvc.perform(delete("/campanhas/{id}", campanha).with(como(M))).andExpect(status().isNoContent());

		mockMvc.perform(get("/campanhas/{id}", campanha).with(como(M))).andExpect(status().isNotFound());
		assertThat(jdbc.queryForObject("select count(*) from ficha where campanha_id is not null", Integer.class)).isZero();
		assertThat(fichas.findAllByDonoId(A)).hasSize(1);
		assertThat(fichas.findAllByDonoId(M)).hasSize(1);
	}

	@Test
	@DisplayName("GET /campanhas/{id}/membros: o mestre primeiro e depois por ordem de entrada, sem e-mail")
	void membros() throws Exception {
		long campanha = mesaComTresFichas();

		String corpo = mockMvc.perform(get("/campanhas/{id}/membros", campanha)).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(JsonPath.<List<String>>read(corpo, "$[*].papel")).containsExactly("MESTRE", "JOGADOR", "JOGADOR");
		assertThat(JsonPath.<List<Number>>read(corpo, "$[*].usuarioId").stream().map(Number::longValue)).containsExactly(M, A, B);
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$[0]").keySet()).containsExactlyInAnyOrder("usuarioId", "nome", "papel", "entrouEm");
		assertThat(corpo).doesNotContain("email").doesNotContain("@fichas-rpg.invalid");
	}

	@Test
	@DisplayName("GET /campanhas/{id}/fichas: o mestre vê todas; o jogador vê as dos jogadores, sem a do mestre, com dono e campanha")
	void fichasDaMesa() throws Exception {
		long campanha = mesaComTresFichas();

		String doMestre = mockMvc.perform(get("/campanhas/{id}/fichas", campanha).with(como(M))).andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<List<String>>read(doMestre, "$[*].personagem")).containsExactly("Do mestre", "De A", "De B");

		String deA = mockMvc.perform(get("/campanhas/{id}/fichas", campanha)).andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<List<String>>read(deA, "$[*].personagem")).containsExactly("De A", "De B");
		assertThat(JsonPath.<Map<String, Object>>read(deA, "$[0]").keySet())
				.containsExactlyInAnyOrder("id", "sistema", "jogador", "personagem", "idade", "retratoVersao", "dono", "campanha", "recursos");
		assertThat(JsonPath.<Number>read(deA, "$[1].dono.id").longValue()).isEqualTo(B);
		assertThat(JsonPath.<String>read(deA, "$[1].dono.nome")).isEqualTo("Jogador B");
		assertThat(JsonPath.<Number>read(deA, "$[1].campanha.id").longValue()).isEqualTo(campanha);
		assertThat(JsonPath.<String>read(deA, "$[1].campanha.nome")).isEqualTo("Mesa");
	}

	@Test
	@DisplayName("POST /campanhas/{id}/fichas: 201 com Location /fichas/{id}, dono = quem cria e a ficha já na campanha; a do mestre não aparece para o jogador")
	void criarFichaNaMesa() throws Exception {
		long campanha = mesaComTresFichas();
		String corpo = json.writeValueAsString(FichasDeTeste.corpoOrdem());

		MvcResult doJogador = mockMvc.perform(post("/campanhas/{id}/fichas", campanha).contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/fichas/[0-9]+")))
				.andExpect(jsonPath("$.dono.id").value(A)).andExpect(jsonPath("$.dono.nome").value("Usuario de teste"))
				.andExpect(jsonPath("$.campanha.id").value(campanha)).andExpect(jsonPath("$.campanha.nome").value("Mesa"))
				.andExpect(jsonPath("$.versao").value(0))
				.andReturn();
		long idDoJogador = JsonPath.<Number>read(doJogador.getResponse().getContentAsString(), "$.id").longValue();
		assertThat(doJogador.getResponse().getHeader("Location")).isEqualTo("/fichas/" + idDoJogador);

		MvcResult doMestre = mockMvc.perform(post("/campanhas/{id}/fichas", campanha).with(como(M)).contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated()).andReturn();
		long idDoMestre = JsonPath.<Number>read(doMestre.getResponse().getContentAsString(), "$.id").longValue();
		mockMvc.perform(get("/fichas/{id}", idDoMestre).with(como(B))).andExpect(status().isNotFound());
		mockMvc.perform(get("/fichas/{id}", idDoJogador).with(como(M))).andExpect(status().isOk());
		mockMvc.perform(get("/campanhas/{id}/fichas", campanha).with(como(B))).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3));
	}
}
