package dev.spironello.fichasrpg.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.repository.FichaRepository;
import tools.jackson.databind.ObjectMapper;

/** Rota PUT /fichas/{id}/descricao: texto livre da ficha (aparência, história e anotações), nos dois sistemas. */
@DisplayName("API /fichas/{id}/descricao")
class DescricaoControllerTest extends IntegrationTestBase {

	private static final Long ID_INEXISTENTE = 999_999L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository fichas;

	@Autowired
	private ObjectMapper objectMapper;

	private Long fichaId;

	@BeforeEach
	void prepararFicha() {
		fichas.deleteAll();
		fichaId = fichas.save(FichasDeTeste.ordem("Lia")).getId();
	}

	private org.springframework.test.web.servlet.ResultActions enviar(Long id, Map<String, Object> corpo)
			throws Exception {
		return mockMvc.perform(put("/fichas/{id}/descricao", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)));
	}

	@Test
	@DisplayName("PUT grava os três campos (200) e o GET da ficha traz a mesma descrição, com as quebras de linha")
	void gravaEBusca() throws Exception {
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("aparencia", "Alta,\ncabelo preto");
		corpo.put("historia", "Nasceu em\n\nSão Paulo");
		corpo.put("anotacoes", "Lembrar do ritual");

		enviar(fichaId, corpo)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.aparencia").value("Alta,\ncabelo preto"))
				.andExpect(jsonPath("$.historia").value("Nasceu em\n\nSão Paulo"))
				.andExpect(jsonPath("$.anotacoes").value("Lembrar do ritual"));

		mockMvc.perform(get("/fichas/{id}", fichaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.descricao.aparencia").value("Alta,\ncabelo preto"))
				.andExpect(jsonPath("$.descricao.historia").value("Nasceu em\n\nSão Paulo"))
				.andExpect(jsonPath("$.descricao.anotacoes").value("Lembrar do ritual"));
	}

	@Test
	@DisplayName("a rota vale também para ficha de D&D")
	void valeParaDnd() throws Exception {
		Long dnd = fichas.save(FichasDeTeste.dnd("Bram")).getId();

		enviar(dnd, Map.of("historia", "Ex-soldado"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/fichas/{id}", dnd))
				.andExpect(jsonPath("$.descricao.historia").value("Ex-soldado"));
	}

	@Test
	@DisplayName("PUT é substituição completa: campos ausentes, vazios ou só com espaços voltam null")
	void vazioViraNulo() throws Exception {
		enviar(fichaId, Map.of("aparencia", "x", "historia", "y", "anotacoes", "z")).andExpect(status().isOk());

		enviar(fichaId, Map.of("aparencia", "", "historia", "  \n "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.aparencia").isEmpty())
				.andExpect(jsonPath("$.historia").isEmpty())
				.andExpect(jsonPath("$.anotacoes").isEmpty());

		mockMvc.perform(get("/fichas/{id}", fichaId))
				.andExpect(jsonPath("$.descricao.aparencia").isEmpty())
				.andExpect(jsonPath("$.descricao.historia").isEmpty())
				.andExpect(jsonPath("$.descricao.anotacoes").isEmpty());
	}

	@Test
	@DisplayName("4000 caracteres passam e 4001 dá 400 no campo")
	void limiteDe4000() throws Exception {
		enviar(fichaId, Map.of("historia", "a".repeat(4000))).andExpect(status().isOk());

		enviar(fichaId, Map.of("historia", "a".repeat(4001)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Dados inválidos"))
				.andExpect(jsonPath("$.errors.historia").exists());
	}

	@Test
	@DisplayName("chave desconhecida dá 400")
	void chaveDesconhecida() throws Exception {
		enviar(fichaId, Map.of("aparencia", "x", "extra", "y"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("extra")));
	}

	@Test
	@DisplayName("ficha inexistente dá 404 Ficha não encontrada")
	void fichaInexistente() throws Exception {
		enviar(ID_INEXISTENTE, Map.of("aparencia", "x"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}
}
