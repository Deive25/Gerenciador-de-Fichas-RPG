package dev.spironello.fichasrpg.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import dev.spironello.fichasrpg.IntegrationTestBase;

/**
 * Foco da revisão 2 do 2.6: o limite de tentativas usa o IP do visitante, e não o do proxy do Render, e um
 * {@code X-Forwarded-For} enviado direto por um cliente de fora não zera o contador. Com {@code server.forward-headers-strategy=native}
 * (application.properties) quem decide é o Tomcat: ele só troca o IP pelo do cabeçalho quando o pedido vem de um proxy
 * interno. Aqui o servidor é de verdade (porta aleatória) e o cliente chega de 127.0.0.1, que NÃO está na lista de proxies
 * confiáveis do teste: logo o cabeçalho é ignorado. O {@link IpDoVisitanteViaProxyTest} é a prova contrária.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = { "app.limite.auth.max=3", "app.limite.auth.janela=PT10M",
		"server.tomcat.remoteip.internal-proxies=192\\.0\\.2\\.1" })
@DisplayName("IP do visitante: X-Forwarded-For de quem não é proxy é ignorado")
class IpDoVisitanteTest extends IntegrationTestBase {

	@Value("${local.server.port}")
	private int porta;
	@Autowired
	private LimiteDeTentativasFilter filtro;

	@BeforeEach
	void zerar() {
		filtro.reiniciar();
	}

	@Test
	@DisplayName("4 logins com um X-Forwarded-For diferente em cada um: o 4º dá 429 (o contador é o do IP real)")
	void cabecalhoFalsoNaoZeraOContador() throws Exception {
		List<Integer> codigos = new ArrayList<>();
		for (int i = 1; i <= 4; i++) {
			codigos.add(postarLogin("203.0.113." + i));
		}

		assertThat(codigos).containsExactly(401, 401, 401, 429);
	}

	/** O mesmo POST /auth/login de sempre, com o X-Forwarded-For dado, por HTTP de verdade. */
	static int postarLogin(String xForwardedFor, int porta) throws Exception {
		HttpRequest pedido = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + "/auth/login"))
				.header("Content-Type", "application/json")
				.header("X-Forwarded-For", xForwardedFor)
				.POST(BodyPublishers.ofString("{\"email\":\"ninguem@limite.test\",\"senha\":\"uma-senha-qualquer\"}"))
				.build();
		return HttpClient.newHttpClient().send(pedido, BodyHandlers.discarding()).statusCode();
	}

	private int postarLogin(String xForwardedFor) throws Exception {
		return postarLogin(xForwardedFor, porta);
	}
}
