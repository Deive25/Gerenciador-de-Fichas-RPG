package dev.spironello.fichasrpg.controller;

import static dev.spironello.fichasrpg.CampanhasDeTeste.como;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

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
 * Sair, ser removido e apagar a campanha devolvem as fichas a "sem campanha" pela chave estrangeira composta, sem o
 * serviço carregar nem regravar as fichas (Foco 2 do plano): depois, o dono vê campanha nula e um PUT com a versão
 * lida dá 200 e não traz a campanha de volta. E o banco recusa a ficha numa campanha de que o dono não participa.
 */
@DisplayName("Campanha da ficha (chave estrangeira composta)")
class CampanhaDaFichaTest extends IntegrationTestBase {

	private static final long A = AutenticacaoDeTesteConfig.USUARIO_DE_TESTE_ID;
	private static final long M = 3_300_001L;
	private static final long B = 3_300_002L;
	private static final long F = 3_300_003L;

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
	private long fichaDeB;
	private long fichaDeM;

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
		fichaDeB = ficha(B, "De B", campanha);
		fichaDeM = ficha(M, "Do mestre", campanha);
	}

	@AfterEach
	void limpar() {
		CampanhasDeTeste.limpar(jdbc, fichas, A, M, B, F);
	}

	private long ficha(long dono, String personagem, Long campanhaId) {
		Ficha ficha = FichasDeTeste.ordem(personagem);
		ficha.setDonoId(dono);
		ficha.setCampanhaId(campanhaId);
		return fichas.saveAndFlush(ficha).getId();
	}

	private Long campanhaNoBanco(long fichaId) {
		return jdbc.queryForObject("select campanha_id from ficha where id = ?", Long.class, fichaId);
	}

	/** O dono lê a ficha (campanha nula) e salva de novo com a versão lida: tem de dar 200, sem trazer a campanha de volta. */
	private void donoVeSemCampanhaESalva(long donoId, long fichaId) throws Exception {
		String lida = mockMvc.perform(get("/fichas/{id}", fichaId).with(como(donoId))).andExpect(status().isOk())
				.andExpect(jsonPath("$.campanha").doesNotExist()).andReturn().getResponse().getContentAsString();
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("versao", JsonPath.<Number>read(lida, "$.versao").longValue());
		mockMvc.perform(put("/fichas/{id}", fichaId).with(como(donoId)).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(corpo))).andExpect(status().isOk()).andExpect(jsonPath("$.campanha").doesNotExist());
		assertThat(campanhaNoBanco(fichaId)).as("o PUT não regravou a campanha antiga").isNull();
	}

	@Test
	@DisplayName("Foco 2: o jogador sai e a ficha dele volta a 'sem campanha'; um PUT com a versão lida dá 200 e a campanha não volta")
	void sair() throws Exception {
		mockMvc.perform(delete("/campanhas/{id}/membros/{u}", campanha, A)).andExpect(status().isNoContent());

		assertThat(campanhaNoBanco(fichaDeA)).isNull();
		assertThat(campanhaNoBanco(fichaDeB)).as("a ficha de quem ficou continua na campanha").isEqualTo(campanha);
		assertThat(campanhaNoBanco(fichaDeM)).isEqualTo(campanha);
		donoVeSemCampanhaESalva(A, fichaDeA);
	}

	@Test
	@DisplayName("Foco 2: o mestre remove o jogador e a ficha dele volta a 'sem campanha'; um PUT com a versão lida dá 200")
	void remover() throws Exception {
		mockMvc.perform(delete("/campanhas/{id}/membros/{u}", campanha, B).with(como(M))).andExpect(status().isNoContent());

		assertThat(campanhaNoBanco(fichaDeB)).isNull();
		assertThat(campanhaNoBanco(fichaDeA)).isEqualTo(campanha);
		donoVeSemCampanhaESalva(B, fichaDeB);
		// a ficha continua do jogador: o mestre que o removeu já não a enxerga
		mockMvc.perform(get("/fichas/{id}", fichaDeB).with(como(M))).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Foco 2: excluir a campanha devolve todas as fichas, inclusive a do mestre, e as participações somem")
	void excluirACampanha() throws Exception {
		mockMvc.perform(delete("/campanhas/{id}", campanha).with(como(M))).andExpect(status().isNoContent());

		assertThat(jdbc.queryForObject("select count(*) from participacao where campanha_id = ?", Integer.class, campanha)).isZero();
		assertThat(jdbc.queryForObject("select count(*) from campanha where id = ?", Integer.class, campanha)).isZero();
		for (long dono : new long[] { A, B, M }) {
			long id = dono == A ? fichaDeA : dono == B ? fichaDeB : fichaDeM;
			assertThat(campanhaNoBanco(id)).isNull();
			donoVeSemCampanhaESalva(dono, id);
		}
	}

	@Test
	@DisplayName("o banco recusa a ficha numa campanha de que o dono não participa, e depois de sair o dono também não a coloca de volta sozinho")
	void bancoRecusaFichaDeQuemNaoParticipa() {
		assertThatThrownBy(() -> ficha(F, "De fora", campanha)).isInstanceOf(DataIntegrityViolationException.class);

		jdbc.update("delete from participacao where campanha_id = ? and usuario_id = ?", campanha, B);
		Ficha deB = fichas.findById(fichaDeB).orElseThrow();
		assertThat(deB.getCampanhaId()).isNull();
		deB.setCampanhaId(campanha);
		assertThatThrownBy(() -> fichas.saveAndFlush(deB)).isInstanceOf(DataIntegrityViolationException.class);
	}
}
