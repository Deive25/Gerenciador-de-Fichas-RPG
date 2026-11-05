package dev.spironello.fichasrpg.controller;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.model.AtributoOrdem;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.GrauOrdem;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.service.Dado;

/**
 * POST /fichas/{id}/testes de ponta a ponta (HTTP, serviço e PostgreSQL real). O {@link Dado} é um mock
 * do @MockitoBean que devolve uma sequência fixa de valores.
 */
@DisplayName("API POST /fichas/{id}/testes")
class TesteControllerTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository repository;

	@MockitoBean
	private Dado dado;

	@BeforeEach
	void limparBanco() {
		repository.deleteAll();
	}

	private Long criarFicha(AtributoOrdem atributo, int valor, PericiaOrdem pericia, GrauOrdem grau) {
		FichaOrdem ficha = FichasDeTeste.ordem("Ana");
		ficha.setAtributo(atributo, valor);
		if (pericia != null) {
			ficha.getPericias().put(pericia, grau);
		}
		return repository.save(ficha).getId();
	}

	private ResultActions testar(Object id, String corpo) throws Exception {
		return mockMvc.perform(post("/fichas/" + id + "/testes")
				.contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	@Test
	@DisplayName("200 perícia com INT 3 e Treinado: rola 3d20, fica o maior e soma +5")
	void periciaTreinada() throws Exception {
		Long id = criarFicha(AtributoOrdem.INT, 3, PericiaOrdem.INVESTIGACAO, GrauOrdem.TREINADO);
		when(dado.rolar(20)).thenReturn(4, 17, 9);

		testar(id, "{\"alvo\": \"PERICIA\", \"codigo\": \"INVESTIGACAO\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.alvo").value("PERICIA"))
				.andExpect(jsonPath("$.codigo").value("INVESTIGACAO"))
				.andExpect(jsonPath("$.nome").value("Investigação"))
				.andExpect(jsonPath("$.dados", contains(4, 17, 9)))
				.andExpect(jsonPath("$.mantido").value(17))
				.andExpect(jsonPath("$.bonus").value(5))
				.andExpect(jsonPath("$.total").value(22));
	}

	@Test
	@DisplayName("200 atributo 0 com Expert: rola 2d20, fica o menor e soma +15")
	void atributoZeroComExpert() throws Exception {
		FichaOrdem ficha = FichasDeTeste.ordem("Ana");
		ficha.setNex(70);
		ficha.setAtributo(AtributoOrdem.AGI, 0);
		ficha.getPericias().put(PericiaOrdem.ACROBACIA, GrauOrdem.EXPERT);
		Long id = repository.save(ficha).getId();
		when(dado.rolar(20)).thenReturn(14, 6);

		testar(id, "{\"alvo\": \"PERICIA\", \"codigo\": \"ACROBACIA\", \"modo\": \"NORMAL\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados", contains(14, 6)))
				.andExpect(jsonPath("$.mantido").value(6))
				.andExpect(jsonPath("$.bonus").value(15))
				.andExpect(jsonPath("$.total").value(21));
	}

	@Test
	@DisplayName("200 perícia destreinada não ganha bônus")
	void periciaDestreinada() throws Exception {
		Long id = criarFicha(AtributoOrdem.FOR, 2, null, null);
		when(dado.rolar(20)).thenReturn(3, 12);

		testar(id, "{\"alvo\": \"PERICIA\", \"codigo\": \"LUTA\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bonus").value(0))
				.andExpect(jsonPath("$.total").value(12));
	}

	@Test
	@DisplayName("200 ATRIBUTO/VIG rola pelo valor do atributo e não tem bônus")
	void atributoSemBonus() throws Exception {
		Long id = criarFicha(AtributoOrdem.VIG, 2, null, null);
		when(dado.rolar(20)).thenReturn(8, 15);

		testar(id, "{\"alvo\": \"ATRIBUTO\", \"codigo\": \"VIG\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.alvo").value("ATRIBUTO"))
				.andExpect(jsonPath("$.codigo").value("VIG"))
				.andExpect(jsonPath("$.nome").value("Vigor"))
				.andExpect(jsonPath("$.dados", contains(8, 15)))
				.andExpect(jsonPath("$.bonus").value(0))
				.andExpect(jsonPath("$.total").value(15));
	}

	@Test
	@DisplayName("400 para modo VANTAGEM, com a chave modo em errors")
	void modoVantagem() throws Exception {
		Long id = criarFicha(AtributoOrdem.INT, 3, null, null);

		testar(id, "{\"alvo\": \"PERICIA\", \"codigo\": \"LUTA\", \"modo\": \"VANTAGEM\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.modo").exists());
	}

	@Test
	@DisplayName("400 para alvo SALVAGUARDA, com a chave alvo em errors")
	void alvoSalvaguarda() throws Exception {
		Long id = criarFicha(AtributoOrdem.INT, 3, null, null);

		testar(id, "{\"alvo\": \"SALVAGUARDA\", \"codigo\": \"FORTITUDE\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.alvo").exists());
	}

	@Test
	@DisplayName("400 para código que não existe em Ordem, com a chave codigo em errors")
	void codigoInexistente() throws Exception {
		Long id = criarFicha(AtributoOrdem.INT, 3, null, null);

		testar(id, "{\"alvo\": \"PERICIA\", \"codigo\": \"VOAR\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.codigo").value("não existe em Ordem Paranormal"));
	}

	@Test
	@DisplayName("400 quando um atributo é pedido como perícia (e vice-versa)")
	void alvoTrocado() throws Exception {
		Long id = criarFicha(AtributoOrdem.INT, 3, null, null);

		testar(id, "{\"alvo\": \"PERICIA\", \"codigo\": \"VIG\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.codigo").exists());
		testar(id, "{\"alvo\": \"ATRIBUTO\", \"codigo\": \"LUTA\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.codigo").exists());
	}

	@Test
	@DisplayName("400 sem alvo ou com código em branco (Bean Validation)")
	void camposObrigatorios() throws Exception {
		Long id = criarFicha(AtributoOrdem.INT, 3, null, null);

		testar(id, "{\"codigo\": \"LUTA\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.alvo").exists());
		testar(id, "{\"alvo\": \"PERICIA\", \"codigo\": \" \"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.codigo").exists());
	}

	@Test
	@DisplayName("404 para ficha inexistente")
	void fichaInexistente() throws Exception {
		testar(999_999L, "{\"alvo\": \"PERICIA\", \"codigo\": \"LUTA\"}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}
}
