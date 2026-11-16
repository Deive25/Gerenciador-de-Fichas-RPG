package dev.spironello.fichasrpg.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.jayway.jsonpath.JsonPath;

import dev.spironello.fichasrpg.AutenticacaoDeTesteConfig;
import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.ImagensDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.repository.FichaRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * Cada pessoa só vê, edita e rola as próprias fichas; a ficha de outro usuário dá 404 (nunca 403). O usuário A é o
 * padrão da base de testes (id 1000000); o B age com um with(jwt()) no próprio pedido.
 */
@DisplayName("Dono da ficha (dois usuários)")
class DonoDaFichaTest extends IntegrationTestBase {

	private static final long ID_B = 2_000_000L;
	private static final RequestPostProcessor COMO_B = jwt().jwt(j -> j.subject(String.valueOf(ID_B)));

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private FichaRepository fichas;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private ObjectMapper json;

	private final List<Long> deA = new ArrayList<>();
	private Long fichaDeA;

	@BeforeEach
	void prepararFichas() {
		jdbc.update("insert into usuario (id, email, nome) values (?, 'outro@fichas-rpg.invalid', 'Outro usuario') "
				+ "on conflict do nothing", ID_B);
		fichaDeA = salvarDeA("Ficha da A");
	}

	@AfterEach
	void limpar() {
		fichas.deleteAll(fichas.findAllByDonoId(ID_B));
		fichas.deleteAllById(deA);
	}

	private Long salvarDeA(String personagem) {
		Long id = fichas.save(FichasDeTeste.ordem(personagem)).getId();
		deA.add(id);
		return id;
	}

	@Test
	@DisplayName("GET, PUT e DELETE /fichas/{id} da ficha de A dão 404 para B, e a ficha de A continua intacta")
	void fichaDeOutroDa404() throws Exception {
		String corpo = json.writeValueAsString(FichasDeTeste.corpoOrdem());

		mockMvc.perform(get("/fichas/{id}", fichaDeA).with(COMO_B)).andExpect(status().isNotFound());
		mockMvc.perform(put("/fichas/{id}", fichaDeA).with(COMO_B).contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isNotFound());
		mockMvc.perform(patch("/fichas/{id}/recursos", fichaDeA).with(COMO_B).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/fichas/{id}", fichaDeA).with(COMO_B)).andExpect(status().isNotFound());

		Ficha intacta = fichas.findById(fichaDeA).orElseThrow();
		assertThat(intacta.getPersonagem()).isEqualTo("Ficha da A");
		assertThat(intacta.getDonoId()).isEqualTo(AutenticacaoDeTesteConfig.USUARIO_DE_TESTE_ID);
	}

	@Test
	@DisplayName("os recursos da ficha (itens, magias, rituais, ataques, habilidades, descrição, moedas, espaços, testes, rolagem e retrato) dão 404 para B")
	void recursosDeOutroDao404() throws Exception {
		record Rota(HttpMethod metodo, String caminho, String tipo, byte[] corpo) {
		}
		byte[] jpeg = ImagensDeTeste.jpeg(40, 50);
		String tipoJson = MediaType.APPLICATION_JSON_VALUE;
		List<Rota> rotas = List.of(
				new Rota(HttpMethod.POST, "/itens", tipoJson, "{\"nome\":\"X\",\"categoria\":0,\"espacos\":1,\"quantidade\":1}".getBytes()),
				new Rota(HttpMethod.POST, "/magias", tipoJson, "{\"nome\":\"X\",\"nivel\":0}".getBytes()),
				new Rota(HttpMethod.POST, "/rituais", tipoJson, "{\"nome\":\"X\",\"circulo\":1,\"elemento\":\"SANGUE\"}".getBytes()),
				new Rota(HttpMethod.POST, "/ataques", tipoJson, "{\"nome\":\"X\",\"dano\":\"1d6\",\"pericia\":\"LUTA\"}".getBytes()),
				new Rota(HttpMethod.POST, "/habilidades", tipoJson, "{\"nome\":\"X\",\"tipo\":\"CLASSE\"}".getBytes()),
				new Rota(HttpMethod.PUT, "/descricao", tipoJson, "{}".getBytes()),
				new Rota(HttpMethod.PATCH, "/moedas", tipoJson, "{\"po\":1}".getBytes()),
				new Rota(HttpMethod.PATCH, "/espacos", tipoJson, "{\"1\":{\"total\":1,\"usados\":0}}".getBytes()),
				new Rota(HttpMethod.POST, "/testes", tipoJson, "{\"alvo\":\"ATRIBUTO\",\"codigo\":\"INT\"}".getBytes()),
				new Rota(HttpMethod.POST, "/rolagens", tipoJson, "{\"expressao\":\"1d6\"}".getBytes()),
				new Rota(HttpMethod.GET, "/retrato", null, null),
				new Rota(HttpMethod.PUT, "/retrato", MediaType.IMAGE_JPEG_VALUE, jpeg),
				new Rota(HttpMethod.DELETE, "/retrato", null, null));

		for (Rota rota : rotas) {
			MockHttpServletRequestBuilder pedido = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
					.request(rota.metodo(), "/fichas/{id}" + rota.caminho(), fichaDeA).with(COMO_B);
			if (rota.tipo() != null) {
				pedido.contentType(rota.tipo()).content(rota.corpo());
			}
			int codigo = mockMvc.perform(pedido).andReturn().getResponse().getStatus();
			assertThat(codigo).as("%s /fichas/{id}%s como outro usuário", rota.metodo(), rota.caminho()).isEqualTo(404);
		}
	}

	@Test
	@DisplayName("as mesmas rotas funcionam para o dono (rolagem e teste dão 200), então o 404 do outro é mesmo por causa do dono")
	void donoAcessaNormalmente() throws Exception {
		mockMvc.perform(post("/fichas/{id}/rolagens", fichaDeA).contentType(MediaType.APPLICATION_JSON)
				.content("{\"expressao\":\"1d6\"}")).andExpect(status().isOk());
		mockMvc.perform(post("/fichas/{id}/testes", fichaDeA).contentType(MediaType.APPLICATION_JSON)
				.content("{\"alvo\":\"ATRIBUTO\",\"codigo\":\"INT\"}")).andExpect(status().isOk());
		mockMvc.perform(get("/fichas/{id}", fichaDeA)).andExpect(status().isOk())
				.andExpect(jsonPath("$.personagem").value("Ficha da A"));
	}

	@Test
	@DisplayName("o 404 da ficha de outro é igual ao de uma ficha que não existe (a API não revela quais ids estão em uso)")
	void mesmoErroDeFichaInexistente() throws Exception {
		String doOutro = mockMvc.perform(get("/fichas/{id}", fichaDeA).with(COMO_B)).andExpect(status().isNotFound())
				.andReturn().getResponse().getContentAsString();
		String inexistente = mockMvc.perform(get("/fichas/{id}", 987654321L).with(COMO_B)).andExpect(status().isNotFound())
				.andReturn().getResponse().getContentAsString();

		assertThat(JsonPath.<String>read(doOutro, "$.title")).isEqualTo(JsonPath.<String>read(inexistente, "$.title"));
		assertThat(JsonPath.<Integer>read(doOutro, "$.status")).isEqualTo(JsonPath.<Integer>read(inexistente, "$.status"));
	}

	@Test
	@DisplayName("a listagem de cada um só traz as próprias fichas")
	void listagemSoComAsProprias() throws Exception {
		Long outraDeA = salvarDeA("Outra da A");
		Long fichaDeB = fichas.save(donoB(FichasDeTeste.ordem("Ficha do B"))).getId();

		List<Integer> idsDeA = JsonPath.read(mockMvc.perform(get("/fichas")).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(), "$[*].id");
		List<Integer> idsDeB = JsonPath.read(mockMvc.perform(get("/fichas").with(COMO_B)).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(), "$[*].id");

		assertThat(idsDeA).contains(fichaDeA.intValue(), outraDeA.intValue()).doesNotContain(fichaDeB.intValue());
		assertThat(idsDeB).containsExactly(fichaDeB.intValue());
	}

	@Test
	@DisplayName("POST /fichas grava como dono quem está logado, e só ele enxerga a ficha criada")
	void criarGravaODono() throws Exception {
		String corpo = json.writeValueAsString(FichasDeTeste.corpoOrdem());

		String resposta = mockMvc.perform(post("/fichas").with(COMO_B).contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		Integer id = JsonPath.read(resposta, "$.id");

		assertThat(jdbc.queryForObject("select dono_id from ficha where id = ?", Long.class, id)).isEqualTo(ID_B);
		mockMvc.perform(get("/fichas/{id}", id).with(COMO_B)).andExpect(status().isOk());
		mockMvc.perform(get("/fichas/{id}", id)).andExpect(status().isNotFound()); // A (o padrão) não vê
		assertThat(resposta).doesNotContain("donoId").doesNotContain("dono_id"); // o JSON da ficha não muda
	}

	@Test
	@DisplayName("o with(jwt()) do pedido prevalece sobre o jwt() padrão da base de testes")
	void jwtDoPedidoPrevaleceSobreOPadrao() throws Exception {
		Long fichaDeB = fichas.save(donoB(FichasDeTeste.ordem("Ficha do B"))).getId();

		// Sem with(): vale o padrão (A). Com with(COMO_B): vale o B.
		mockMvc.perform(get("/fichas/{id}", fichaDeA)).andExpect(status().isOk());
		mockMvc.perform(get("/fichas/{id}", fichaDeB)).andExpect(status().isNotFound());
		mockMvc.perform(get("/fichas/{id}", fichaDeB).with(COMO_B)).andExpect(status().isOk());
		mockMvc.perform(get("/fichas/{id}", fichaDeA).with(COMO_B)).andExpect(status().isNotFound());
	}

	private static Ficha donoB(Ficha ficha) {
		ficha.setDonoId(ID_B);
		return ficha;
	}
}
