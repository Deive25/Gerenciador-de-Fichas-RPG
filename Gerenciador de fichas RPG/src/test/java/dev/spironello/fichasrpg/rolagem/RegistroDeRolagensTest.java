package dev.spironello.fichasrpg.rolagem;

import static dev.spironello.fichasrpg.CampanhasDeTeste.como;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import dev.spironello.fichasrpg.AutenticacaoDeTesteConfig;
import dev.spironello.fichasrpg.CampanhasDeTeste;
import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.campanha.Papel;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.repository.AtaqueOrdemRepository;
import dev.spironello.fichasrpg.repository.FichaRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * As três rotas de rolagem (teste, ataque e livre) gravam uma linha em "rolagem" quando a ficha está numa campanha, e
 * só nesse caso. O nome do NPC do mestre nunca é gravado (personagem nulo).
 */
@DisplayName("Registro das rolagens da mesa")
class RegistroDeRolagensTest extends IntegrationTestBase {

	private static final long J = AutenticacaoDeTesteConfig.USUARIO_DE_TESTE_ID;
	private static final long M = 7_100_001L;
	private static final long F = 7_100_003L;

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private FichaRepository fichas;
	@Autowired
	private AtaqueOrdemRepository ataques;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private ObjectMapper json;

	private long campanha;
	private long fichaDoJogador;
	private long fichaDoMestre;

	@BeforeEach
	void prepararMesa() {
		CampanhasDeTeste.usuario(jdbc, M, "Mestre Mor");
		CampanhasDeTeste.usuario(jdbc, F, "De fora");
		campanha = CampanhasDeTeste.campanha(jdbc, "Mesa");
		CampanhasDeTeste.participar(jdbc, campanha, M, Papel.MESTRE);
		CampanhasDeTeste.participar(jdbc, campanha, J, Papel.JOGADOR);
		fichaDoJogador = ficha(J, "Kai", campanha);
		fichaDoMestre = ficha(M, "Ogro Secreto", campanha);
	}

	@AfterEach
	void limpar() {
		CampanhasDeTeste.limpar(jdbc, fichas, J, M, F);
	}

	private long ficha(long dono, String personagem, Long campanhaId) {
		FichaOrdem ficha = FichasDeTeste.ordem(personagem);
		ficha.setDonoId(dono);
		ficha.setCampanhaId(campanhaId);
		return fichas.saveAndFlush(ficha).getId();
	}

	private long ataque(long fichaId) {
		var ataque = FichasDeTeste.ataqueOrdem((FichaOrdem) fichas.findById(fichaId).orElseThrow(), "Katana");
		return ataques.saveAndFlush(ataque).getId();
	}

	private String testar(long usuario, long fichaId) throws Exception {
		return mockMvc.perform(post("/fichas/{id}/testes", fichaId).with(como(usuario))
				.contentType(MediaType.APPLICATION_JSON).content("{\"alvo\":\"PERICIA\",\"codigo\":\"LUTA\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
	}

	private String atacar(long usuario, long fichaId, long ataqueId) throws Exception {
		return mockMvc.perform(post("/fichas/{id}/ataques/{a}/rolagem", fichaId, ataqueId).with(como(usuario)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
	}

	private String rolarLivre(long usuario, long fichaId, String expressao) throws Exception {
		return mockMvc.perform(post("/fichas/{id}/rolagens", fichaId).with(como(usuario))
				.contentType(MediaType.APPLICATION_JSON).content("{\"expressao\":\"" + expressao + "\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
	}

	private List<Map<String, Object>> linhas() {
		return jdbc.queryForList("select usuario_id, ficha_id, personagem, tipo, rotulo, total, detalhe::text as detalhe "
				+ "from rolagem order by id");
	}

	private JsonNode detalhe(Map<String, Object> linha) {
		return json.readTree((String) linha.get("detalhe"));
	}

	@Test
	@DisplayName("o teste grava tipo, rótulo, total e o detalhe igual à resposta")
	void testeGrava() throws Exception {
		String resposta = testar(J, fichaDoJogador);

		assertThat(linhas()).hasSize(1);
		Map<String, Object> linha = linhas().get(0);
		JsonNode esperado = json.readTree(resposta);
		assertThat(linha).containsEntry("tipo", "TESTE").containsEntry("usuario_id", J).containsEntry("ficha_id", fichaDoJogador)
				.containsEntry("personagem", "Kai").containsEntry("rotulo", esperado.get("nome").asString())
				.containsEntry("total", esperado.get("total").asInt());
		assertThat(detalhe(linha)).isEqualTo(esperado);
	}

	@Test
	@DisplayName("o ataque grava o d20 como total e a resposta inteira (com o dano) no detalhe")
	void ataqueGrava() throws Exception {
		long ataqueId = ataque(fichaDoJogador);
		String resposta = atacar(J, fichaDoJogador, ataqueId);

		assertThat(linhas()).hasSize(1);
		Map<String, Object> linha = linhas().get(0);
		JsonNode esperado = json.readTree(resposta);
		assertThat(linha).containsEntry("tipo", "ATAQUE").containsEntry("rotulo", "Katana")
				.containsEntry("total", esperado.get("ataque").get("total").asInt());
		assertThat(detalhe(linha)).isEqualTo(esperado);
		assertThat(detalhe(linha).has("dano")).isTrue();
	}

	@Test
	@DisplayName("a rolagem livre grava a expressão canônica como rótulo")
	void livreGrava() throws Exception {
		String resposta = rolarLivre(J, fichaDoJogador, "2D6 + 3");

		assertThat(linhas()).hasSize(1);
		Map<String, Object> linha = linhas().get(0);
		JsonNode esperado = json.readTree(resposta);
		assertThat(linha).containsEntry("tipo", "LIVRE").containsEntry("rotulo", esperado.get("expressao").asString())
				.containsEntry("total", esperado.get("total").asInt());
		assertThat(detalhe(linha)).isEqualTo(esperado);
	}

	@Test
	@DisplayName("a ficha sem campanha não grava nada, nos três tipos")
	void semCampanhaNaoGrava() throws Exception {
		long solta = ficha(J, "Solta", null);
		long ataqueId = ataque(solta);

		testar(J, solta);
		atacar(J, solta, ataqueId);
		rolarLivre(J, solta, "1d20");

		assertThat(linhas()).isEmpty();
	}

	@Test
	@DisplayName("a notação inválida (400) não grava nada")
	void notacaoInvalidaNaoGrava() throws Exception {
		mockMvc.perform(post("/fichas/{id}/rolagens", fichaDoJogador).with(como(J)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"expressao\":\"banana\"}")).andExpect(status().isBadRequest());

		assertThat(linhas()).isEmpty();
	}

	@Test
	@DisplayName("a ficha do mestre grava personagem nulo, nos três tipos, e o nome do NPC não aparece em lugar nenhum da linha")
	void fichaDoMestreNaoGravaONome() throws Exception {
		long ataqueId = ataque(fichaDoMestre);

		testar(M, fichaDoMestre);
		atacar(M, fichaDoMestre, ataqueId);
		rolarLivre(M, fichaDoMestre, "1d20");

		List<Map<String, Object>> linhas = linhas();
		assertThat(linhas).hasSize(3);
		assertThat(linhas).allSatisfy(linha -> {
			assertThat(linha.get("personagem")).isNull();
			assertThat(linha.get("usuario_id")).isEqualTo(M);
			assertThat(linha.toString()).doesNotContain("Ogro Secreto");
		});
		assertThat(jdbc.queryForObject("select count(*) from rolagem where rotulo like '%Ogro%' or detalhe::text like '%Ogro%'",
				Integer.class)).isZero();
	}

	@Test
	@DisplayName("o mestre rolando com a ficha do jogador grava o mestre como autor e o personagem do jogador")
	void mestreRolandoComFichaDoJogador() throws Exception {
		testar(M, fichaDoJogador);

		Map<String, Object> linha = linhas().get(0);
		assertThat(linha).containsEntry("usuario_id", M).containsEntry("personagem", "Kai").containsEntry("ficha_id", fichaDoJogador);
	}

	@Test
	@DisplayName("a ficha tirada da campanha depois de a tela abrir não grava: vale a campanha do momento da rolagem")
	void fichaTiradaDaCampanhaNaoGrava() throws Exception {
		mockMvc.perform(put("/fichas/{id}/campanha", fichaDoJogador).with(como(J)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"campanhaId\":null}")).andExpect(status().isOk());

		testar(J, fichaDoJogador);
		rolarLivre(J, fichaDoJogador, "1d20");

		assertThat(linhas()).isEmpty();
	}
}
