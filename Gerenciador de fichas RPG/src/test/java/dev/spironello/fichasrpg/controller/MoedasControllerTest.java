package dev.spironello.fichasrpg.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.repository.ItemDndRepository;
import dev.spironello.fichasrpg.repository.ItemOrdemRepository;

/** Rota PATCH /fichas/{id}/moedas: só mudam as moedas enviadas, e só em ficha de D&D. */
@DisplayName("API PATCH /fichas/{id}/moedas")
class MoedasControllerTest extends IntegrationTestBase {

	private static final Long ID_INEXISTENTE = 999_999L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository fichas;

	@Autowired
	private ItemOrdemRepository itensOrdem;

	@Autowired
	private ItemDndRepository itensDnd;

	private Long dndId;
	private Long ordemId;

	@BeforeEach
	void prepararFichas() {
		itensOrdem.deleteAll();
		itensDnd.deleteAll();
		fichas.deleteAll();
		dndId = fichas.save(FichasDeTeste.dnd("Bram")).getId();
		ordemId = fichas.save(FichasDeTeste.ordem("Lia")).getId();
	}

	private ResultActions enviar(Long fichaId, String json) throws Exception {
		return mockMvc.perform(patch("/fichas/{id}/moedas", fichaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private void conferirMoedasNaFicha(int pc, int pp, int pe, int po, int pl) throws Exception {
		mockMvc.perform(get("/fichas/{id}", dndId))
				.andExpect(jsonPath("$.moedas.pc").value(pc))
				.andExpect(jsonPath("$.moedas.pp").value(pp))
				.andExpect(jsonPath("$.moedas.pe").value(pe))
				.andExpect(jsonPath("$.moedas.po").value(po))
				.andExpect(jsonPath("$.moedas.pl").value(pl));
	}

	@Test
	@DisplayName("PATCH só com po muda o ouro, devolve as cinco moedas e o GET da ficha mostra o mesmo")
	void mudaSoOOuro() throws Exception {
		enviar(dndId, "{\"po\": 10}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pc").value(0))
				.andExpect(jsonPath("$.pp").value(0))
				.andExpect(jsonPath("$.pe").value(0))
				.andExpect(jsonPath("$.po").value(10))
				.andExpect(jsonPath("$.pl").value(0));

		conferirMoedasNaFicha(0, 0, 0, 10, 0);
	}

	@Test
	@DisplayName("um segundo PATCH não mexe nas moedas que não enviou")
	void segundoPatchPreservaAsOutras() throws Exception {
		enviar(dndId, "{\"po\": 10}").andExpect(status().isOk());

		enviar(dndId, "{\"pc\": 5, \"pl\": 1}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pc").value(5))
				.andExpect(jsonPath("$.po").value(10))
				.andExpect(jsonPath("$.pl").value(1));

		conferirMoedasNaFicha(5, 0, 0, 10, 1);
	}

	@Test
	@DisplayName("PATCH vazio dá 200 sem mudança")
	void patchVazio() throws Exception {
		enviar(dndId, "{\"po\": 10}").andExpect(status().isOk());

		enviar(dndId, "{}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.po").value(10));

		conferirMoedasNaFicha(0, 0, 0, 10, 0);
	}

	@Test
	@DisplayName("zero é válido: zera uma moeda que tinha valor")
	void zeroEValido() throws Exception {
		enviar(dndId, "{\"po\": 10}").andExpect(status().isOk());

		enviar(dndId, "{\"po\": 0}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.po").value(0));
	}

	@Test
	@DisplayName("valor negativo dá 400 em errors.po e nada muda")
	void valorNegativo() throws Exception {
		enviar(dndId, "{\"pc\": 3}").andExpect(status().isOk());

		enviar(dndId, "{\"po\": -1, \"pc\": 9}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Dados inválidos"))
				.andExpect(jsonPath("$.errors.po").exists());

		conferirMoedasNaFicha(3, 0, 0, 0, 0);
	}

	@Test
	@DisplayName("chave desconhecida dá 400")
	void chaveDesconhecida() throws Exception {
		enviar(dndId, "{\"po\": 1, \"extra\": 2}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("extra")));
	}

	@Test
	@DisplayName("valor que não é número dá 400")
	void valorNaoNumerico() throws Exception {
		enviar(dndId, "{\"po\": \"muito\"}")
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("ficha de Ordem dá 400 em errors.lista")
	void fichaDeOrdem() throws Exception {
		enviar(ordemId, "{\"po\": 10}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Dados inválidos"))
				.andExpect(jsonPath("$.errors.lista").value("não existe em Ordem Paranormal"));
	}

	@Test
	@DisplayName("ficha inexistente dá 404 Ficha não encontrada")
	void fichaInexistente() throws Exception {
		enviar(ID_INEXISTENTE, "{\"po\": 10}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}
}
