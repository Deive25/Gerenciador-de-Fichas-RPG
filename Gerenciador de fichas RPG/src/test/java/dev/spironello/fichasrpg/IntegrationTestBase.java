package dev.spironello.fichasrpg;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
	}
}
