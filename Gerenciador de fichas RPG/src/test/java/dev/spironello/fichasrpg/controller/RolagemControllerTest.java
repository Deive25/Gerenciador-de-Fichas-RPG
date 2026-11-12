package dev.spironello.fichasrpg.controller;

import static org.hamcrest.Matchers.hasSize;
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
import dev.spironello.fichasrpg.model.AtaqueDnd;
import dev.spironello.fichasrpg.model.AtaqueOrdem;
import dev.spironello.fichasrpg.model.AtributoDnd;
import dev.spironello.fichasrpg.model.AtributoOrdem;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.GrauOrdem;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.repository.AtaqueDndRepository;
import dev.spironello.fichasrpg.repository.AtaqueOrdemRepository;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.service.Dado;

/**
 * POST /fichas/{id}/ataques/{ataqueId}/rolagem e POST /fichas/{id}/rolagens de ponta a ponta, com o {@link Dado}
 * trocado por um mock que devolve valores fixos por número de faces.
 */
@DisplayName("API de rolagens (ataque e livre)")
class RolagemControllerTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private FichaRepository fichas;
	@Autowired
	private AtaqueOrdemRepository ataquesOrdem;
	@Autowired
	private AtaqueDndRepository ataquesDnd;

	@MockitoBean
	private Dado dado;

	@BeforeEach
	void limpar() {
		fichas.deleteAll();
	}

	private ResultActions rolarAtaque(Long fichaId, Long ataqueId, String corpo) throws Exception {
		var req = post("/fichas/{id}/ataques/{a}/rolagem", fichaId, ataqueId);
		if (corpo != null) {
			req = req.contentType(MediaType.APPLICATION_JSON).content(corpo);
		}
		return mockMvc.perform(req);
	}

	private ResultActions rolarLivre(Object fichaId, String corpo) throws Exception {
		return mockMvc.perform(post("/fichas/" + fichaId + "/rolagens")
				.contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	/** Ordem: Força 3, Luta Treinado (+5), ataque com bônus 2, 1d8+3, margem e multiplicador dados. */
	private Long ataqueOrdem(FichaOrdem ficha, int margem, int multiplicador) {
		AtaqueOrdem a = FichasDeTeste.ataqueOrdem(ficha, "Katana");
		a.setBonus(2);
		a.setMargem(margem);
		a.setMultiplicador(multiplicador);
		return ataquesOrdem.save(a).getId();
	}

	private FichaOrdem fichaOrdem() {
		FichaOrdem ficha = FichasDeTeste.ordem("Lia");
		ficha.setAtributo(AtributoOrdem.FOR, 3);
		ficha.getPericias().put(PericiaOrdem.LUTA, GrauOrdem.TREINADO);
		return fichas.save(ficha);
	}

	/** D&D: Força 16 (+3), nível 5 (proficiência +3). */
	private FichaDnd fichaDnd() {
		FichaDnd ficha = FichasDeTeste.dnd("Bram");
		ficha.setAtributo(AtributoDnd.FOR, 16);
		ficha.setNivel(5);
		return fichas.save(ficha);
	}

	private Long ataqueDnd(FichaDnd ficha, boolean proficiente, int bonus) {
		AtaqueDnd a = FichasDeTeste.ataqueDnd(ficha, "Espada");
		a.setProficiente(proficiente);
		a.setBonus(bonus);
		return ataquesDnd.save(a).getId();
	}

	// ---- ataque: Ordem ----

	@Test
	@DisplayName("Ordem sem crítico: 3d20 (FOR 3), mantém o maior, soma grau +5 e bônus +2; dano 1d8+3 uma vez")
	void ordemSemCritico() throws Exception {
		FichaOrdem ficha = fichaOrdem();
		Long id = ataqueOrdem(ficha, 19, 3);
		when(dado.rolar(20)).thenReturn(4, 18, 9);
		when(dado.rolar(8)).thenReturn(6);

		rolarAtaque(ficha.getId(), id, null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ataqueId").value(id))
				.andExpect(jsonPath("$.nome").value("Katana"))
				.andExpect(jsonPath("$.ataque.dados[0]").value(4))
				.andExpect(jsonPath("$.ataque.mantido").value(18))
				.andExpect(jsonPath("$.ataque.bonus").value(7))
				.andExpect(jsonPath("$.ataque.total").value(25))
				.andExpect(jsonPath("$.dano.expressao").value("1d8+3"))
				.andExpect(jsonPath("$.dano.dados", hasSize(1)))
				.andExpect(jsonPath("$.dano.dados[0].lados").value(8))
				.andExpect(jsonPath("$.dano.dados[0].valor").value(6))
				.andExpect(jsonPath("$.dano.bonus").value(3))
				.andExpect(jsonPath("$.dano.total").value(9))
				.andExpect(jsonPath("$.critico").value(false));
	}

	@Test
	@DisplayName("Ordem com crítico (mantido 19 na margem 19, multiplicador 3): 3 dados de dano e a constante uma vez")
	void ordemComCritico() throws Exception {
		FichaOrdem ficha = fichaOrdem();
		Long id = ataqueOrdem(ficha, 19, 3);
		when(dado.rolar(20)).thenReturn(2, 19, 5);
		when(dado.rolar(8)).thenReturn(6, 5, 4);

		rolarAtaque(ficha.getId(), id, "{}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ataque.mantido").value(19))
				.andExpect(jsonPath("$.critico").value(true))
				.andExpect(jsonPath("$.dano.dados", hasSize(3)))
				.andExpect(jsonPath("$.dano.bonus").value(3))
				.andExpect(jsonPath("$.dano.total").value(18));
	}

	@Test
	@DisplayName("Ordem: modo VANTAGEM dá 400 em errors.modo; NORMAL passa")
	void ordemComModo() throws Exception {
		FichaOrdem ficha = fichaOrdem();
		Long id = ataqueOrdem(ficha, 20, 2);
		when(dado.rolar(20)).thenReturn(10, 10, 10);
		when(dado.rolar(8)).thenReturn(1);

		rolarAtaque(ficha.getId(), id, "{\"modo\":\"VANTAGEM\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.modo").exists());
		rolarAtaque(ficha.getId(), id, "{\"modo\":\"NORMAL\"}").andExpect(status().isOk());
	}

	// ---- ataque: D&D ----

	@Test
	@DisplayName("D&D proficiente: bônus = mod +3, proficiência +3 e bônus do ataque +1; 20 natural é crítico e dobra o dano")
	void dndCritico() throws Exception {
		FichaDnd ficha = fichaDnd();
		Long id = ataqueDnd(ficha, true, 1);
		when(dado.rolar(20)).thenReturn(20);
		when(dado.rolar(8)).thenReturn(5, 2);

		rolarAtaque(ficha.getId(), id, null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ataque.dados[0]").value(20))
				.andExpect(jsonPath("$.ataque.bonus").value(7))
				.andExpect(jsonPath("$.ataque.total").value(27))
				.andExpect(jsonPath("$.critico").value(true))
				.andExpect(jsonPath("$.dano.dados", hasSize(2)))
				.andExpect(jsonPath("$.dano.total").value(10));
	}

	@Test
	@DisplayName("D&D não proficiente com 19: só o modificador, sem crítico, dano 1d8+3 uma vez")
	void dndSemCritico() throws Exception {
		FichaDnd ficha = fichaDnd();
		Long id = ataqueDnd(ficha, false, 0);
		when(dado.rolar(20)).thenReturn(19);
		when(dado.rolar(8)).thenReturn(5);

		rolarAtaque(ficha.getId(), id, null)
				.andExpect(jsonPath("$.ataque.bonus").value(3))
				.andExpect(jsonPath("$.ataque.total").value(22))
				.andExpect(jsonPath("$.critico").value(false))
				.andExpect(jsonPath("$.dano.dados", hasSize(1)))
				.andExpect(jsonPath("$.dano.total").value(8));
	}

	@Test
	@DisplayName("D&D com desvantagem [20, 3]: mantém 3 e não é crítico")
	void dndDesvantagemSemCritico() throws Exception {
		FichaDnd ficha = fichaDnd();
		Long id = ataqueDnd(ficha, false, 0);
		when(dado.rolar(20)).thenReturn(20, 3);
		when(dado.rolar(8)).thenReturn(5);

		rolarAtaque(ficha.getId(), id, "{\"modo\":\"DESVANTAGEM\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ataque.dados", hasSize(2)))
				.andExpect(jsonPath("$.ataque.mantido").value(3))
				.andExpect(jsonPath("$.critico").value(false));
	}

	@Test
	@DisplayName("D&D com vantagem [20, 3]: mantém 20 e é crítico")
	void dndVantagemComCritico() throws Exception {
		FichaDnd ficha = fichaDnd();
		Long id = ataqueDnd(ficha, false, 0);
		when(dado.rolar(20)).thenReturn(20, 3);
		when(dado.rolar(8)).thenReturn(5, 5);

		rolarAtaque(ficha.getId(), id, "{\"modo\":\"VANTAGEM\"}")
				.andExpect(jsonPath("$.ataque.mantido").value(20))
				.andExpect(jsonPath("$.critico").value(true));
	}

	@Test
	@DisplayName("ataque inexistente, de outra ficha ou ficha inexistente dão 404; modo desconhecido dá 400")
	void naoEncontradoEModoInvalido() throws Exception {
		FichaDnd ficha = fichaDnd();
		Long id = ataqueDnd(ficha, false, 0);
		FichaDnd outra = (FichaDnd) fichas.save(FichasDeTeste.dnd("Outra"));

		rolarAtaque(ficha.getId(), 999_999L, null).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"));
		rolarAtaque(outra.getId(), id, null).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Item não encontrado"));
		rolarAtaque(999_999L, id, null).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
		rolarAtaque(ficha.getId(), id, "{\"modo\":\"XYZ\"}").andExpect(status().isBadRequest());
	}

	// ---- rolagem livre ----

	@Test
	@DisplayName("rolagem livre válida (D&D e Ordem): expressão canônica, dados, bônus e total; nada é gravado")
	void livreValida() throws Exception {
		FichaDnd dnd = fichaDnd();
		FichaOrdem ordem = fichaOrdem();
		when(dado.rolar(6)).thenReturn(4, 1);
		when(dado.rolar(4)).thenReturn(3);

		for (Long id : new Long[] { dnd.getId(), ordem.getId() }) {
			rolarLivre(id, "{\"expressao\":\" 2D6 + 1d4 + 3 \"}")
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.expressao").value("2d6+1d4+3"))
					.andExpect(jsonPath("$.dados", hasSize(3)))
					.andExpect(jsonPath("$.dados[0].lados").value(6))
					.andExpect(jsonPath("$.dados[0].valor").value(4))
					.andExpect(jsonPath("$.dados[2].lados").value(4))
					.andExpect(jsonPath("$.dados[2].valor").value(3))
					.andExpect(jsonPath("$.bonus").value(3))
					.andExpect(jsonPath("$.total").value(11));
			when(dado.rolar(6)).thenReturn(4, 1);
			when(dado.rolar(4)).thenReturn(3);
		}
	}

	@Test
	@DisplayName("rolagem livre inválida, ausente ou vazia dá 400 em errors.expressao")
	void livreInvalida() throws Exception {
		Long id = fichaDnd().getId();
		rolarLivre(id, "{\"expressao\":\"1d7\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.expressao").value("use d4, d6, d8, d10, d12, d20 ou d100"));
		rolarLivre(id, "{\"expressao\":\"11d6+10d6\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.expressao").value("no máximo 20 dados"));
		rolarLivre(id, "{\"expressao\":\"\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.expressao").exists());
		rolarLivre(id, "{}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.expressao").exists());
	}

	@Test
	@DisplayName("rolagem livre em ficha inexistente dá 404 Ficha não encontrada")
	void livreFichaInexistente() throws Exception {
		rolarLivre(999_999L, "{\"expressao\":\"1d6\"}").andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}
}
