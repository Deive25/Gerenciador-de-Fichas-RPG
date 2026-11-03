package Atos.SpringAPI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import Atos.SpringAPI.Models.SheetPlayer;
import Atos.SpringAPI.Repositories.SheetPlayerRepository;

/**
 * Trava o contrato JSON que o frontend Angular consome (Sheet.model.ts): o conjunto de
 * chaves da ficha. Lê o corpo com JsonPath para não depender da versão do Jackson.
 */
@DisplayName("Contrato JSON da ficha (consumido pelo frontend)")
class ContratoJsonTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SheetPlayerRepository repository;

	@Test
	@DisplayName("GET /sheetPlayer/{id} devolve exatamente as 41 chaves esperadas (atributos em minúsculas)")
	void chavesDoJson() throws Exception {
		SheetPlayer ficha = new SheetPlayer();
		ficha.setNameJ("Arthur");
		ficha.setNameC("Ana");
		ficha.setGender("F");
		ficha = repository.save(ficha);

		String corpo = mockMvc.perform(get("/sheetPlayer/{id}", ficha.getId()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		Map<String, Object> json = JsonPath.read(corpo, "$");

		Set<String> esperadas = new HashSet<>(List.of(
				"id", "nameJ", "nameC", "gender", "age", "lifeA", "lifeM", "sanA", "sanM",
				"str", "dex", "cons", "int", "car", "sab", "tam", "sor", "pod"));
		for (int i = 1; i <= 23; i++) {
			esperadas.add("atribute" + i);
		}

		assertThat(esperadas).hasSize(41);
		assertThat(json.keySet()).containsExactlyInAnyOrderElementsOf(esperadas);
	}
}
