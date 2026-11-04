package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Prova que a migração V3 só acrescenta a tabela retrato_ficha: as fichas que já existiam continuam
 * iguais e ficam sem retrato. Mesmo roteiro do MigracaoPericiasTest, num schema próprio do container.
 */
@DisplayName("Migração V3: tabela de retratos, sem mexer nas fichas existentes")
class MigracaoRetratoTest extends IntegrationTestBase {

	private static final String SCHEMA = "migracao_retrato";

	@Value("${spring.datasource.url}")
	private String url;
	@Value("${spring.datasource.username}")
	private String usuario;
	@Value("${spring.datasource.password}")
	private String senha;

	private JdbcTemplate jdbc;

	@AfterEach
	void limparSchema() {
		if (jdbc != null) {
			jdbc.execute("drop schema if exists " + SCHEMA + " cascade");
		}
	}

	@Test
	@DisplayName("fichas da V2 continuam lá e ficam sem retrato")
	void fichasAntigasFicamSemRetrato() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(url, usuario, senha);
		jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("drop schema if exists " + SCHEMA + " cascade");

		Flyway.configure().dataSource(dataSource).schemas(SCHEMA).target("2").load().migrate();
		jdbc.update("insert into " + SCHEMA + ".sheet_player (id, namej, namec, gender, lifea) "
				+ "overriding system value values (1, 'Ana', 'Lia', 'F', 9)");

		Flyway.configure().dataSource(dataSource).schemas(SCHEMA).load().migrate();

		assertThat(jdbc.queryForObject("select count(*) from " + SCHEMA + ".retrato_ficha", Integer.class)).isZero();
		assertThat(jdbc.queryForObject("select namec from " + SCHEMA + ".sheet_player where id = 1", String.class))
				.isEqualTo("Lia");
	}
}
