package dev.spironello.fichasrpg.controller;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

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
import dev.spironello.fichasrpg.model.AtributoDnd;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.GrauDnd;
import dev.spironello.fichasrpg.model.PericiaDnd;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.service.Dado;

/** POST /fichas/{id}/testes em D&D (nível 5, proficiência +3), com o {@link Dado} em sequência fixa. */
@DisplayName("API POST /fichas/{id}/testes (D&D)")
class TesteDndControllerTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository repository;

	@MockitoBean
	private Dado dado;

	private Long id;

	@BeforeEach
	void criarFicha() {
		repository.deleteAll();
		FichaDnd ficha = FichasDeTeste.dnd("Ana");
		ficha.setNivel(5);
		ficha.setAtributo(AtributoDnd.DES, 14);
		ficha.setAtributo(AtributoDnd.FOR, 8);
		ficha.setAtributo(AtributoDnd.SAB, 12);
		ficha.getPericias().put(PericiaDnd.FURTIVIDADE, GrauDnd.ESPECIALISTA);
		ficha.setSalvaguardas(Set.of(AtributoDnd.DES));
		id = repository.save(ficha).getId();
	}

	private ResultActions testar(Object alvoId, String corpo) throws Exception {
		return mockMvc.perform(post("/fichas/" + alvoId + "/testes")
				.contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	@Test
	@DisplayName("200 perícia especialista: +2 do DES e 2 x 3 de proficiência = +8")
	void periciaEspecialista() throws Exception {
		when(dado.rolar(20)).thenReturn(11);

		testar(id, "{\"alvo\": \"PERICIA\", \"codigo\": \"FURTIVIDADE\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.alvo").value("PERICIA"))
				.andExpect(jsonPath("$.codigo").value("FURTIVIDADE"))
				.andExpect(jsonPath("$.nome").value("Furtividade"))
				.andExpect(jsonPath("$.dados", contains(11)))
				.andExpect(jsonPath("$.mantido").value(11))
				.andExpect(jsonPath("$.bonus").value(8))
				.andExpect(jsonPath("$.total").value(19));
	}

	@Test
	@DisplayName("200 salvaguarda proficiente: +2 do DES e +3 de proficiência = +5")
	void salvaguardaProficiente() throws Exception {
		when(dado.rolar(20)).thenReturn(10);

		testar(id, "{\"alvo\": \"SALVAGUARDA\", \"codigo\": \"DES\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Salvaguarda de Destreza"))
				.andExpect(jsonPath("$.bonus").value(5))
				.andExpect(jsonPath("$.total").value(15));
	}

	@Test
	@DisplayName("200 salvaguarda sem proficiência com FOR 8: bônus negativo -1")
	void salvaguardaNegativa() throws Exception {
		when(dado.rolar(20)).thenReturn(7);

		testar(id, "{\"alvo\": \"SALVAGUARDA\", \"codigo\": \"FOR\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Salvaguarda de Força"))
				.andExpect(jsonPath("$.bonus").value(-1))
				.andExpect(jsonPath("$.total").value(6));
	}

	@Test
	@DisplayName("200 atributo: só o modificador (SAB 12 = +1)")
	void atributo() throws Exception {
		when(dado.rolar(20)).thenReturn(10);

		testar(id, "{\"alvo\": \"ATRIBUTO\", \"codigo\": \"SAB\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Sabedoria"))
				.andExpect(jsonPath("$.bonus").value(1))
				.andExpect(jsonPath("$.total").value(11));
	}

	@Test
	@DisplayName("200 vantagem rola 2d20 e fica o maior")
	void vantagem() throws Exception {
		when(dado.rolar(20)).thenReturn(4, 17);

		testar(id, "{\"alvo\": \"PERICIA\", \"codigo\": \"FURTIVIDADE\", \"modo\": \"VANTAGEM\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados", contains(4, 17)))
				.andExpect(jsonPath("$.mantido").value(17))
				.andExpect(jsonPath("$.total").value(25));
	}

	@Test
	@DisplayName("200 desvantagem rola 2d20 e fica o menor")
	void desvantagem() throws Exception {
		when(dado.rolar(20)).thenReturn(4, 17);

		testar(id, "{\"alvo\": \"PERICIA\", \"codigo\": \"FURTIVIDADE\", \"modo\": \"DESVANTAGEM\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados", contains(4, 17)))
				.andExpect(jsonPath("$.mantido").value(4))
				.andExpect(jsonPath("$.total").value(12));
	}

	@Test
	@DisplayName("400 para código que não existe em D&D, com a chave codigo em errors")
	void codigoInexistente() throws Exception {
		testar(id, "{\"alvo\": \"PERICIA\", \"codigo\": \"OCULTISMO\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.codigo").value("não existe em D&D"));
	}

	@Test
	@DisplayName("404 para ficha inexistente")
	void fichaInexistente() throws Exception {
		testar(999_999L, "{\"alvo\": \"PERICIA\", \"codigo\": \"FURTIVIDADE\"}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}
}
