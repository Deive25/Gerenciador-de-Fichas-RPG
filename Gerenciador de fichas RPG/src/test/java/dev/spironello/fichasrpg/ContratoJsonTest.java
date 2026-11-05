package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.GrauOrdem;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.repository.FichaRepository;

/**
 * Trava o contrato JSON que o frontend Angular consome: o conjunto de chaves da ficha de Ordem e o do
 * resumo da listagem. Lê o corpo com JsonPath para não depender da versão do Jackson.
 */
@DisplayName("Contrato JSON da ficha (consumido pelo frontend)")
class ContratoJsonTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository repository;

	@Test
	@DisplayName("GET /fichas/{id} devolve exatamente as 13 chaves da ficha de Ordem, com mapas por código")
	void chavesDaFicha() throws Exception {
		FichaOrdem ficha = FichasDeTeste.ordem("Ana");
		ficha.setPvAtual(10);
		ficha.getPericias().put(PericiaOrdem.ACROBACIA, GrauOrdem.TREINADO);
		ficha.getPericias().put(PericiaOrdem.PERCEPCAO, GrauOrdem.TREINADO);
		ficha = repository.save(ficha);

		String corpo = mockMvc.perform(get("/fichas/{id}", ficha.getId()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		Map<String, Object> json = JsonPath.read(corpo, "$");

		List<String> esperadas = List.of(
				"id", "sistema", "jogador", "personagem", "genero", "idade", "retratoVersao",
				"classe", "origem", "nex", "atributos", "recursos", "pericias");

		assertThat(json.keySet()).containsExactlyInAnyOrderElementsOf(esperadas);
		assertThat(json.get("sistema")).isEqualTo("ORDEM_PARANORMAL");
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.atributos").keySet())
				.containsExactlyInAnyOrder("AGI", "FOR", "INT", "PRE", "VIG");
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.recursos").keySet())
				.containsExactlyInAnyOrder("PV", "SAN", "PE");
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.recursos.PV").keySet())
				.containsExactlyInAnyOrder("atual", "maximo");
		// pericias é um objeto com só as treinadas, chaveado pelo código do enum
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.pericias"))
				.containsOnly(Map.entry("ACROBACIA", "TREINADO"), Map.entry("PERCEPCAO", "TREINADO"));
	}

	@Test
	@DisplayName("GET /fichas devolve itens com exatamente as 7 chaves do resumo")
	void chavesDoResumo() throws Exception {
		repository.save(FichasDeTeste.ordem("Ana"));

		String corpo = mockMvc.perform(get("/fichas"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		Map<String, Object> primeiro = JsonPath.read(corpo, "$[0]");

		assertThat(primeiro.keySet()).containsExactlyInAnyOrder(
				"id", "sistema", "jogador", "personagem", "idade", "retratoVersao", "recursos");
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$[0].recursos[0]").keySet())
				.containsExactlyInAnyOrder("codigo", "atual", "maximo");
	}
}
