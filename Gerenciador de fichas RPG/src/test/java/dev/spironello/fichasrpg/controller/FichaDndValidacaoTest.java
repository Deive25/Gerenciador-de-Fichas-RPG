package dev.spironello.fichasrpg.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.test.web.servlet.ResultActions;

import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.repository.FichaRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * Entrada inválida da ficha de D&D. Mantém de propósito os dois formatos de chave: o Bean Validation dá
 * "atributos[FOR]" (valor fora do intervalo) e a regra cruzada dá "atributos.CON" (atributo ausente).
 */
@DisplayName("API /fichas com D&D (validação de entrada)")
class FichaDndValidacaoTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository repository;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void limparBanco() {
		repository.deleteAll();
	}

	@Test
	@DisplayName("nível 0 e nível 21 devolvem 400 no campo nivel")
	void nivelForaDoIntervalo() throws Exception {
		for (int nivel : new int[] { 0, 21 }) {
			Map<String, Object> corpo = FichasDeTeste.corpoDnd();
			corpo.put("nivel", nivel);

			problema400(enviar("POST", "/fichas", corpo))
					.andExpect(jsonPath("$.errors.nivel").value("deve estar entre 1 e 20"));
		}
		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("atributo FOR 31 devolve 400 em atributos[FOR]")
	void atributoForaDoIntervalo() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoDnd();
		Map<String, Object> atributos = new LinkedHashMap<>();
		atributos.put("FOR", 31);
		atributos.put("DES", 10);
		atributos.put("CON", 10);
		atributos.put("INT", 10);
		atributos.put("SAB", 10);
		atributos.put("CAR", 10);
		corpo.put("atributos", atributos);

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.errors['atributos[FOR]']").value("deve estar entre 1 e 30"));
		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("falta o atributo CON: 400 em atributos.CON")
	void atributoAusente() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoDnd();
		@SuppressWarnings("unchecked")
		Map<String, Object> atributos = (Map<String, Object>) corpo.get("atributos");
		atributos.remove("CON");

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.errors['atributos.CON']").value("é obrigatório"));
		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("falta a edição: 400 em edicao")
	void semEdicao() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoDnd();
		corpo.remove("edicao");

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.errors.edicao").value("é obrigatório"));
	}

	@Test
	@DisplayName("PV_TEMP com maximo: 400 em recursos.PV_TEMP.maximo")
	void pvTemporarioComMaximo() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoDnd();
		corpo.put("recursos", Map.of("PV_TEMP", Map.of("atual", 3, "maximo", 5)));

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.errors['recursos.PV_TEMP.maximo']").value("PV temporário não tem máximo"));
		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("PV negativo: 400 em recursos[PV].atual")
	void pvNegativo() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoDnd();
		corpo.put("recursos", Map.of("PV", Map.of("atual", -1)));

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.errors['recursos[PV].atual']").value("deve ser 0 ou mais"));
	}

	@Test
	@DisplayName("perícia OCULTISMO (só existe em Ordem) devolve 400")
	void periciaDeOrdem() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoDnd();
		corpo.put("pericias", Map.of("OCULTISMO", "PROFICIENTE"));

		problema400(enviar("POST", "/fichas", corpo));
		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("grau de Ordem (TREINADO) numa perícia de D&D devolve 400")
	void grauDeOrdem() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoDnd();
		corpo.put("pericias", Map.of("ATLETISMO", "TREINADO"));

		problema400(enviar("POST", "/fichas", corpo));
	}

	@Test
	@DisplayName("chave desconhecida devolve 400 citando a chave")
	void chaveDesconhecida() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoDnd();
		corpo.put("mana", 5);

		problema400(enviar("POST", "/fichas", corpo))
				.andExpect(jsonPath("$.detail", containsString("mana")));
	}

	@Test
	@DisplayName("id no corpo é ignorado: o PUT altera só a ficha da URL")
	void idNoCorpoIgnorado() throws Exception {
		FichaDnd alvo = repository.save(FichasDeTeste.dnd("Alvo"));
		FichaDnd outra = repository.save(FichasDeTeste.dnd("Outra"));
		Map<String, Object> corpo = FichasDeTeste.corpoDnd();
		corpo.put("id", outra.getId());
		corpo.put("personagem", "Atualizada");

		enviar("PUT", "/fichas/" + alvo.getId(), corpo)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(alvo.getId()));

		assertThat(repository.findById(alvo.getId()).orElseThrow().getPersonagem()).isEqualTo("Atualizada");
		assertThat(repository.findById(outra.getId()).orElseThrow().getPersonagem()).isEqualTo("Outra");
	}

	private ResultActions enviar(String metodo, String url, Map<String, Object> corpo) throws Exception {
		var requisicao = "PUT".equals(metodo) ? put(url) : post(url);
		return mockMvc.perform(requisicao.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)));
	}

	private ResultActions problema400(ResultActions resultado) throws Exception {
		return resultado
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").exists());
	}
}
