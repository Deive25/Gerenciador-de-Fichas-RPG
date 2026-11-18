package dev.spironello.fichasrpg;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base dos testes de integração: sobe a aplicação inteira (Spring Boot + JPA) contra um
 * PostgreSQL real rodando em container (Testcontainers). Precisa do Docker em execução.
 *
 * O container é "singleton": inicia uma única vez por execução de testes e é compartilhado
 * por todas as classes que herdam desta base. Assim o Spring também reaproveita o mesmo
 * contexto entre as classes (mais rápido). Com @Container o container reiniciaria a cada
 * classe e o contexto em cache ficaria apontando para uma porta que não existe mais.
 * O container é removido ao fim da JVM pelo Ryuk, do próprio Testcontainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ RelogioDeTesteConfig.class, AutenticacaoDeTesteConfig.class, ChavesDoGoogleDeTeste.class })
// O limite de tentativas do /auth/* (2.6) é de 10 por minuto por IP; os testes fazem mais logins que isso no mesmo "IP" (o MockMvc
// é sempre 127.0.0.1), então aqui ele é folgado. Quem testa o limite (LimiteDeTentativasTest) declara os seus valores.
@TestPropertySource(properties = { "app.limite.auth.max=100000", "app.limite.demo.max=100000" })
public abstract class IntegrationTestBase {

	// Mesma versão do docker-compose.yml, para testar no banco que se usa de verdade.
	private static final PostgreSQLContainer POSTGRES =
			new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

	static {
		POSTGRES.start();
	}

	// Sobrescreve a configuração do banco do application.properties (que lê variáveis
	// de ambiente) com os dados do container, assim os testes não dependem de .env.
	@DynamicPropertySource
	static void configurarBanco(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		// Segredo do JWT SO de teste (>= 32 bytes). O de verdade vem do .env (JWT_SEGREDO) e nunca vai para o repositorio.
		registry.add("app.jwt.segredo", () -> "segredo-de-teste-nao-usar-em-producao-0123456789");
	}

	@Autowired
	private JdbcTemplate jdbcDaBase;

	/**
	 * Todo teste roda como o usuário de teste (id fixo): a linha dele é garantida no banco (as fichas de teste têm
	 * dono obrigatório) e o contexto de segurança vale também para quem chama os serviços direto, sem o MockMvc.
	 */
	@BeforeEach
	void prepararUsuarioDeTeste() {
		jdbcDaBase.update("insert into usuario (id, email, nome) values (?, 'teste@fichas-rpg.invalid', 'Usuario de teste') "
				+ "on conflict do nothing", AutenticacaoDeTesteConfig.USUARIO_DE_TESTE_ID);
		Jwt jwt = Jwt.withTokenValue("token-de-teste").header("alg", "HS256")
				.subject(String.valueOf(AutenticacaoDeTesteConfig.USUARIO_DE_TESTE_ID)).build();
		TestSecurityContextHolder.setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
	}

	@AfterEach
	void limparContextoDeSeguranca() {
		TestSecurityContextHolder.clearContext();
	}
}
