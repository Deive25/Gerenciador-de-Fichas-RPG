package dev.spironello.fichasrpg.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

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
 * A prova contrária do {@link IpDoVisitanteTest}: quando o pedido vem de um proxy confiável (aqui o 127.0.0.1, como o
 * proxy interno do Render em produção), o Tomcat usa o X-Forwarded-For e o contador passa a ser o do visitante, não o do
 * proxy. Sem isto, todos os visitantes do Render dividiriam um contador só (o IP do proxy). Mostra também que o teste
 * anterior não passa à toa: o que muda entre os dois é só a lista de proxies confiáveis.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = { "app.limite.auth.max=3", "app.limite.auth.janela=PT10M",
		"server.tomcat.remoteip.internal-proxies=127\\.0\\.0\\.1|0:0:0:0:0:0:0:1|::1" })
@DisplayName("IP do visitante: atrás de um proxy confiável vale o X-Forwarded-For")
class IpDoVisitanteViaProxyTest extends IntegrationTestBase {

	@Value("${local.server.port}")
	private int porta;
	@Autowired
	private LimiteDeTentativasFilter filtro;

	@BeforeEach
	void zerar() {
		filtro.reiniciar();
	}

	@Test
	@DisplayName("visitantes diferentes (X-Forwarded-For diferentes) têm contadores diferentes, e o mesmo visitante é limitado")
	void contadorPorVisitante() throws Exception {
		List<Integer> variados = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			variados.add(IpDoVisitanteTest.postarLogin("203.0.113." + i, porta));
		}
		assertThat(variados).containsOnly(401);

		List<Integer> mesmoVisitante = new ArrayList<>();
		for (int i = 1; i <= 4; i++) {
			mesmoVisitante.add(IpDoVisitanteTest.postarLogin("198.51.100.7", porta));
		}
		assertThat(mesmoVisitante).containsExactly(401, 401, 401, 429);
	}
}
