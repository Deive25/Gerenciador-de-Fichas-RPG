package dev.spironello.fichasrpg.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.model.Pericia;
import dev.spironello.fichasrpg.model.SheetPlayer;
import dev.spironello.fichasrpg.repository.SheetPlayerRepository;
import dev.spironello.fichasrpg.service.Dado;

/**
 * POST /fichas/{id}/testes-de-pericia de ponta a ponta (HTTP, serviço e PostgreSQL real).
 * O {@link Dado} é trocado por um mock do @MockitoBean: é a forma mais simples de fixar a
 * rolagem sem criar uma classe de configuração só para o teste.
 */
@DisplayName("API /fichas/{id}/testes-de-pericia")
class TestePericiaControllerTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SheetPlayerRepository repository;

	@MockitoBean
	private Dado dado;

	@BeforeEach
	void limparBanco() {
		repository.deleteAll();
	}

	private Long criarFichaComAcrobacia(int valor) {
		SheetPlayer ficha = new SheetPlayer();
		ficha.setNameJ("Jogador");
		ficha.setNameC("Personagem");
		ficha.setGender("F");
		ficha.getPericias().put(Pericia.ACROBACIA, valor);
		return repository.save(ficha).getId();
	}

	private ResultActions testar(Object id, String corpo) throws Exception {
		return mockMvc.perform(post("/fichas/" + id + "/testes-de-pericia")
				.contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	@Test
	@DisplayName("200 com perícia, nome, valor, rolagem e resultado")
	void sucessoExtremo() throws Exception {
		Long id = criarFichaComAcrobacia(40);
		when(dado.rolarD100()).thenReturn(7);

		testar(id, "{\"pericia\": \"ACROBACIA\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pericia").value("ACROBACIA"))
				.andExpect(jsonPath("$.nome").value("Acrobacia"))
				.andExpect(jsonPath("$.valor").value(40))
				.andExpect(jsonPath("$.rolagem").value(7))
				.andExpect(jsonPath("$.resultado").value("SUCESSO_EXTREMO"));
	}

	@Test
	@DisplayName("200 FALHA para perícia que a ficha não tem (valor 0)")
	void periciaSemValor() throws Exception {
		Long id = criarFichaComAcrobacia(40);
		when(dado.rolarD100()).thenReturn(1);

		testar(id, "{\"pericia\": \"CHARME\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valor").value(0))
				.andExpect(jsonPath("$.resultado").value("FALHA"));
	}

	@Test
	@DisplayName("404 ProblemDetail para ficha inexistente")
	void fichaInexistente() throws Exception {
		testar(999999, "{\"pericia\": \"ACROBACIA\"}")
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}

	@Test
	@DisplayName("400 quando falta a chave pericia")
	void semPericia() throws Exception {
		Long id = criarFichaComAcrobacia(40);

		testar(id, "{}")
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Há campos inválidos na requisição. Veja a propriedade errors."))
				.andExpect(jsonPath("$.errors.pericia").exists());
	}

	@Test
	@DisplayName("400 citando o campo pericia quando o código não existe (\"VOAR\")")
	void codigoInexistente() throws Exception {
		Long id = criarFichaComAcrobacia(40);

		testar(id, "{\"pericia\": \"VOAR\"}")
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail", containsString("pericia")));
	}

	@Test
	@DisplayName("400 para chave desconhecida no corpo")
	void chaveDesconhecida() throws Exception {
		Long id = criarFichaComAcrobacia(40);

		testar(id, "{\"pericia\": \"ACROBACIA\", \"bonus\": 10}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("bonus")));
	}
}
