package dev.spironello.fichasrpg.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.RelogioDeTesteConfig.RelogioAjustavel;

/**
 * O limite de tentativas das rotas de entrada (2.6, spec §5.6; fecha um risco do ADR 0022): janela fixa por IP e por
 * rota, 429 com Retry-After. Os limites aqui são pequenos (3 por minuto e 2 por 10 minutos) para o teste ser curto; o
 * padrão da aplicação é 10 por minuto e 5 por 10 minutos (application.properties). O relógio é o do teste.
 */
@TestPropertySource(properties = { "app.limite.auth.max=3", "app.limite.auth.janela=PT1M", "app.limite.demo.max=2",
		"app.limite.demo.janela=PT10M" })
@DisplayName("Limite de tentativas (/auth/login, /registro, /google e /demo)")
class LimiteDeTentativasTest extends IntegrationTestBase {

	private static final Instant INICIO = Instant.parse("2026-01-01T00:00:00Z");
	private static final String CORPO_LOGIN = "{\"email\":\"ninguem@limite.test\",\"senha\":\"uma-senha-qualquer\"}";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private LimiteDeTentativasFilter filtro;
	@Autowired
	private RelogioAjustavel relogio;

	@BeforeEach
	void zerar() {
		relogio.definir(INICIO);
		filtro.reiniciar();
	}

	@Test
	@DisplayName("login: 3 tentativas passam (401 de senha errada) e a 4ª dá 429 com Retry-After e o erro no formato do projeto")
	void quartaTentativaDa429() throws Exception {
		for (int i = 0; i < 3; i++) {
			login("10.0.0.1").andExpect(status().isUnauthorized());
		}

		login("10.0.0.1")
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(429))
				.andExpect(jsonPath("$.title").value("Muitas tentativas"))
				.andExpect(jsonPath("$.detail").value("Muitas tentativas. Tente de novo em alguns minutos."));
	}

	@Test
	@DisplayName("o Retry-After vai diminuindo com o tempo da janela e, passada a janela, o contador recomeça")
	void janelaRecomeca() throws Exception {
		for (int i = 0; i < 4; i++) {
			login("10.0.0.1");
		}

		relogio.avancar(Duration.ofSeconds(20));
		login("10.0.0.1").andExpect(status().isTooManyRequests()).andExpect(header().string(HttpHeaders.RETRY_AFTER, "40"));

		relogio.avancar(Duration.ofSeconds(40)); // 60 s desde a primeira: a janela fechou
		login("10.0.0.1").andExpect(status().isUnauthorized());
		for (int i = 0; i < 2; i++) {
			login("10.0.0.1").andExpect(status().isUnauthorized());
		}
		login("10.0.0.1").andExpect(status().isTooManyRequests());
	}

	@Test
	@DisplayName("o contador é por IP: quem está bloqueado não bloqueia os outros")
	void porIp() throws Exception {
		for (int i = 0; i < 4; i++) {
			login("10.0.0.1");
		}
		login("10.0.0.1").andExpect(status().isTooManyRequests());

		login("10.0.0.2").andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("o contador é por rota: o login bloqueado não bloqueia o registro, que tem a sua própria conta")
	void porRota() throws Exception {
		for (int i = 0; i < 4; i++) {
			login("10.0.0.1");
		}
		login("10.0.0.1").andExpect(status().isTooManyRequests());

		for (int i = 0; i < 3; i++) {
			registro("10.0.0.1").andExpect(status().isBadRequest());
		}
		registro("10.0.0.1").andExpect(status().isTooManyRequests());
	}

	@Test
	@DisplayName("/auth/google também tem limite")
	void google() throws Exception {
		for (int i = 0; i < 3; i++) {
			mockMvc.perform(daAmostra(post("/auth/google"), "10.0.0.1").contentType(MediaType.APPLICATION_JSON).content("{}"))
					.andExpect(status().isBadRequest());
		}
		mockMvc.perform(daAmostra(post("/auth/google"), "10.0.0.1").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isTooManyRequests());
	}

	@Test
	@DisplayName("/auth/demo tem um limite menor e uma janela maior: 2 a cada 10 minutos aqui (conta mesmo sem o perfil demo)")
	void demo() throws Exception {
		demo("10.0.0.1").andExpect(status().isNotFound());
		demo("10.0.0.1").andExpect(status().isNotFound());

		demo("10.0.0.1").andExpect(status().isTooManyRequests()).andExpect(header().string(HttpHeaders.RETRY_AFTER, "600"));

		relogio.avancar(Duration.ofMinutes(9));
		demo("10.0.0.1").andExpect(status().isTooManyRequests());
		relogio.avancar(Duration.ofMinutes(1));
		demo("10.0.0.1").andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("/auth/refresh fica de fora: a rotação já o protege")
	void refreshSemLimite() throws Exception {
		for (int i = 0; i < 12; i++) {
			mockMvc.perform(daAmostra(post("/auth/refresh"), "10.0.0.1").contentType(MediaType.APPLICATION_JSON)
					.content("{\"refreshToken\":\"nao-existe\"}")).andExpect(status().isUnauthorized());
		}
	}

	@Test
	@DisplayName("a resposta 429 leva o cabeçalho do CORS, senão o navegador esconderia a mensagem do front")
	void respostaTemCors() throws Exception {
		for (int i = 0; i < 4; i++) {
			login("10.0.0.1");
		}

		mockMvc.perform(daAmostra(post("/auth/login"), "10.0.0.1").header(HttpHeaders.ORIGIN, "http://localhost:4200")
				.contentType(MediaType.APPLICATION_JSON).content(CORPO_LOGIN))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
	}

	@Test
	@DisplayName("as janelas vencidas são limpas: o mapa não cresce com os IPs que passaram")
	void limpaAsJanelasVencidas() throws Exception {
		for (int ip = 1; ip <= 5; ip++) {
			login("10.0.1." + ip);
		}
		assertThat(filtro.janelasAtivas()).isEqualTo(5);

		relogio.avancar(Duration.ofMinutes(2));
		login("10.0.2.1"); // o pedido seguinte, passado o minuto, dispara a limpeza

		assertThat(filtro.janelasAtivas()).isEqualTo(1);
	}

	private ResultActions login(String ip) throws Exception {
		return mockMvc.perform(daAmostra(post("/auth/login"), ip).contentType(MediaType.APPLICATION_JSON).content(CORPO_LOGIN));
	}

	private ResultActions registro(String ip) throws Exception {
		return mockMvc.perform(daAmostra(post("/auth/registro"), ip).contentType(MediaType.APPLICATION_JSON).content("{}"));
	}

	private ResultActions demo(String ip) throws Exception {
		return mockMvc.perform(daAmostra(post("/auth/demo"), ip));
	}

	/** O pedido anônimo vindo do IP dado (no MockMvc, o IP do cliente é o remoteAddr). */
	private MockHttpServletRequestBuilder daAmostra(MockHttpServletRequestBuilder pedido, String ip) {
		return pedido.with(anonymous()).with(requisicao -> {
			requisicao.setRemoteAddr(ip);
			return requisicao;
		});
	}
}
