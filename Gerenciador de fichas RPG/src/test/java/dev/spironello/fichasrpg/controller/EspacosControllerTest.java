package dev.spironello.fichasrpg.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.repository.FichaRepository;

/** Rota PATCH /fichas/{id}/espacos: grava só os níveis enviados, devolve os nove, e só em ficha de D&D. */
@DisplayName("API PATCH /fichas/{id}/espacos")
class EspacosControllerTest extends IntegrationTestBase {

	private static final Long ID_INEXISTENTE = 999_999L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository fichas;

	@Autowired
	private JdbcTemplate jdbc;

	private Long dndId;
	private Long ordemId;

	@BeforeEach
	void prepararFichas() {
		fichas.deleteAll();
		dndId = fichas.save(FichasDeTeste.dnd("Bram")).getId();
		ordemId = fichas.save(FichasDeTeste.ordem("Lia")).getId();
	}

	private ResultActions enviar(Long fichaId, String json) throws Exception {
		return mockMvc.perform(patch("/fichas/{id}/espacos", fichaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private void conferirNaFicha(int nivel, int total, int usados) throws Exception {
		mockMvc.perform(get("/fichas/{id}", dndId))
				.andExpect(jsonPath("$.espacos['" + nivel + "'].total").value(total))
				.andExpect(jsonPath("$.espacos['" + nivel + "'].usados").value(usados));
	}

	private int linhasNoBanco() {
		return jdbc.queryForObject("select count(*) from ficha_dnd_espaco where ficha_id = ?", Integer.class, dndId);
	}

	@Test
	@DisplayName("ficha de D&D nova traz os nove níveis em 0 de 0, nas chaves \"1\" a \"9\"")
	void fichaNovaTemNoveNiveisZerados() throws Exception {
		mockMvc.perform(get("/fichas/{id}", dndId))
				.andExpect(jsonPath("$.espacos.length()").value(9))
				.andExpect(jsonPath("$.espacos['1'].total").value(0))
				.andExpect(jsonPath("$.espacos['1'].usados").value(0))
				.andExpect(jsonPath("$.espacos['9'].total").value(0))
				.andExpect(jsonPath("$.espacos['9'].usados").value(0));
	}

	@Test
	@DisplayName("PATCH de um nível grava só ele, devolve os nove e o GET da ficha mostra o mesmo")
	void mudaUmNivel() throws Exception {
		enviar(dndId, "{\"1\": {\"total\": 4, \"usados\": 1}}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(9))
				.andExpect(jsonPath("$.['1'].total").value(4))
				.andExpect(jsonPath("$.['1'].usados").value(1))
				.andExpect(jsonPath("$.['2'].total").value(0))
				.andExpect(jsonPath("$.['9'].usados").value(0));

		conferirNaFicha(1, 4, 1);
		conferirNaFicha(2, 0, 0);
		assertEquals(1, linhasNoBanco());
	}

	@Test
	@DisplayName("um segundo PATCH não mexe nos níveis que não enviou e atualiza o que enviou")
	void patchParcialPreservaOsOutros() throws Exception {
		enviar(dndId, "{\"1\": {\"total\": 4, \"usados\": 1}, \"3\": {\"total\": 2, \"usados\": 0}}")
				.andExpect(status().isOk());

		enviar(dndId, "{\"3\": {\"total\": 3, \"usados\": 3}}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.['1'].total").value(4))
				.andExpect(jsonPath("$.['1'].usados").value(1))
				.andExpect(jsonPath("$.['3'].total").value(3))
				.andExpect(jsonPath("$.['3'].usados").value(3));

		conferirNaFicha(1, 4, 1);
		conferirNaFicha(3, 3, 3);
		assertEquals(2, linhasNoBanco());
	}

	@Test
	@DisplayName("PATCH vazio dá 200 sem mudança")
	void patchVazio() throws Exception {
		enviar(dndId, "{\"2\": {\"total\": 2, \"usados\": 1}}").andExpect(status().isOk());

		enviar(dndId, "{}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.['2'].usados").value(1));

		conferirNaFicha(2, 2, 1);
	}

	@Test
	@DisplayName("zero é válido: zera os usados (descanso longo) e aceita total 0 com usados 0")
	void zeroEValido() throws Exception {
		enviar(dndId, "{\"4\": {\"total\": 3, \"usados\": 2}}").andExpect(status().isOk());

		enviar(dndId, "{\"4\": {\"total\": 3, \"usados\": 0}, \"5\": {\"total\": 0, \"usados\": 0}}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.['4'].usados").value(0))
				.andExpect(jsonPath("$.['5'].total").value(0));
	}

	@Test
	@DisplayName("usados igual ao total passa (borda)")
	void usadosIgualAoTotal() throws Exception {
		enviar(dndId, "{\"9\": {\"total\": 1, \"usados\": 1}}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.['9'].usados").value(1));
	}

	@Test
	@DisplayName("usados maior que o total dá 400 em errors.\"3.usados\" e nada muda")
	void usadosMaiorQueOTotal() throws Exception {
		enviar(dndId, "{\"1\": {\"total\": 1, \"usados\": 0}}").andExpect(status().isOk());

		enviar(dndId, "{\"1\": {\"total\": 9, \"usados\": 0}, \"3\": {\"total\": 2, \"usados\": 3}}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Dados inválidos"))
				.andExpect(jsonPath("$.errors.['3.usados']").value("não pode passar do total"));

		conferirNaFicha(1, 1, 0);
		conferirNaFicha(3, 0, 0);
	}

	@Test
	@DisplayName("nível 10, 0 e chave que não é número dão 400 em errors com a chave enviada")
	void nivelForaDeUmANove() throws Exception {
		enviar(dndId, "{\"10\": {\"total\": 1, \"usados\": 0}}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.['10']").value("nível deve estar entre 1 e 9"));
		enviar(dndId, "{\"0\": {\"total\": 1, \"usados\": 0}}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.['0']").value("nível deve estar entre 1 e 9"));
		enviar(dndId, "{\"abc\": {\"total\": 1, \"usados\": 0}, \"01\": {\"total\": 1, \"usados\": 0}}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.abc").value("nível deve estar entre 1 e 9"))
				.andExpect(jsonPath("$.errors.['01']").value("nível deve estar entre 1 e 9"));

		assertEquals(0, linhasNoBanco());
	}

	@Test
	@DisplayName("total ou usados ausente dá 400 \"é obrigatório\" em errors.\"3.total\" e \"3.usados\"")
	void camposObrigatorios() throws Exception {
		enviar(dndId, "{\"3\": {\"usados\": 0}}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.['3.total']").value("é obrigatório"))
				.andExpect(jsonPath("$.errors.['3.usados']").doesNotExist());
		enviar(dndId, "{\"3\": {\"total\": 2}}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.['3.usados']").value("é obrigatório"));
		enviar(dndId, "{\"3\": {}}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.['3.total']").value("é obrigatório"))
				.andExpect(jsonPath("$.errors.['3.usados']").value("é obrigatório"));
		enviar(dndId, "{\"3\": null}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.['3.total']").value("é obrigatório"))
				.andExpect(jsonPath("$.errors.['3.usados']").value("é obrigatório"));
	}

	@Test
	@DisplayName("total ou usados negativo dá 400 \"deve ser 0 ou mais\"")
	void negativos() throws Exception {
		enviar(dndId, "{\"3\": {\"total\": -1, \"usados\": -2}}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.['3.total']").value("deve ser 0 ou mais"))
				.andExpect(jsonPath("$.errors.['3.usados']").value("deve ser 0 ou mais"));
	}

	@Test
	@DisplayName("os erros de vários níveis vêm juntos numa resposta só")
	void variosErrosDeUmaVez() throws Exception {
		enviar(dndId, "{\"10\": {\"total\": 1, \"usados\": 0}, \"2\": {\"usados\": 0}, \"3\": {\"total\": 1, \"usados\": 2}}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.['10']").exists())
				.andExpect(jsonPath("$.errors.['2.total']").value("é obrigatório"))
				.andExpect(jsonPath("$.errors.['3.usados']").value("não pode passar do total"));
	}

	@Test
	@DisplayName("chave desconhecida dentro do nível dá 400")
	void chaveDesconhecida() throws Exception {
		enviar(dndId, "{\"1\": {\"total\": 1, \"usados\": 0, \"extra\": 2}}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("extra")));
	}

	@Test
	@DisplayName("valor que não é número dá 400")
	void valorNaoNumerico() throws Exception {
		enviar(dndId, "{\"1\": {\"total\": \"muitos\", \"usados\": 0}}")
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("ficha de Ordem dá 400 em errors.lista")
	void fichaDeOrdem() throws Exception {
		enviar(ordemId, "{\"1\": {\"total\": 1, \"usados\": 0}}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Dados inválidos"))
				.andExpect(jsonPath("$.errors.lista").value("não existe em Ordem Paranormal"));
	}

	@Test
	@DisplayName("ficha inexistente dá 404 Ficha não encontrada")
	void fichaInexistente() throws Exception {
		enviar(ID_INEXISTENTE, "{\"1\": {\"total\": 1, \"usados\": 0}}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}

	@Test
	@DisplayName("DELETE da ficha com espaços dá 204 e as linhas somem do banco")
	void excluirFichaApagaOsEspacos() throws Exception {
		enviar(dndId, "{\"1\": {\"total\": 4, \"usados\": 1}, \"2\": {\"total\": 2, \"usados\": 0}}")
				.andExpect(status().isOk());

		mockMvc.perform(delete("/fichas/{id}", dndId)).andExpect(status().isNoContent());

		assertEquals(0, jdbc.queryForObject("select count(*) from ficha_dnd_espaco", Integer.class));
	}
}
