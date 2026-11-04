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

import dev.spironello.fichasrpg.model.Pericia;
import dev.spironello.fichasrpg.model.SheetPlayer;
import dev.spironello.fichasrpg.repository.SheetPlayerRepository;

/**
 * Trava o contrato JSON que o frontend Angular consome (Sheet.model.ts): o conjunto de
 * chaves da ficha (incremento 1.6: nomes claros, perícias num objeto). Lê o corpo com
 * JsonPath para não depender da versão do Jackson.
 */
@DisplayName("Contrato JSON da ficha (consumido pelo frontend)")
class ContratoJsonTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SheetPlayerRepository repository;

	@Test
	@DisplayName("GET /fichas/{id} devolve exatamente as 18 chaves de primeiro nível, com pericias como objeto")
	void chavesDoJson() throws Exception {
		SheetPlayer ficha = new SheetPlayer();
		ficha.setNameJ("Arthur");
		ficha.setNameC("Ana");
		ficha.setGender("F");
		ficha.setLifeA(10);
		ficha.getPericias().put(Pericia.ACROBACIA, 40);
		ficha.getPericias().put(Pericia.PERCEPCAO, 55);
		ficha = repository.save(ficha);

		String corpo = mockMvc.perform(get("/fichas/{id}", ficha.getId()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		Map<String, Object> json = JsonPath.read(corpo, "$");

		List<String> esperadas = List.of(
				"id", "jogador", "personagem", "genero", "idade",
				"vidaAtual", "vidaMaxima", "sanidadeAtual", "sanidadeMaxima",
				"forca", "destreza", "constituicao", "inteligencia", "carisma",
				"sabedoria", "tamanho", "sorte", "poder", "pericias");

		assertThat(esperadas).hasSize(19); // as 18 chaves escalares (com id) + o objeto pericias
		assertThat(json.keySet()).containsExactlyInAnyOrderElementsOf(esperadas);
		assertThat(json.get("vidaAtual")).isEqualTo(10);
		// pericias é um objeto com só as preenchidas, chaveado pelo código do enum
		assertThat(json.get("pericias")).isInstanceOf(Map.class);
		Map<String, Object> pericias = JsonPath.read(corpo, "$.pericias");
		assertThat(pericias).containsOnly(Map.entry("ACROBACIA", 40), Map.entry("PERCEPCAO", 55));
	}
}
