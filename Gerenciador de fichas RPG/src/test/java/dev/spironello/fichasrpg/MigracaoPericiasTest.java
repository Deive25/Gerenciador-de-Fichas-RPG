package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Prova que a migração V2 move as perícias das colunas atributeN para a tabela ficha_pericia
 * sem perder nem inventar dados. Usa a API do Flyway num schema próprio (migracao_teste) do mesmo
 * container, para não interferir no schema "public" que os outros testes usam.
 * Roteiro: migra só até a V1, insere fichas no formato antigo, migra até a última versão e confere.
 */
@DisplayName("Migração V2: perícias das colunas para a tabela ficha_pericia")
class MigracaoPericiasTest extends IntegrationTestBase {

	private static final String SCHEMA = "migracao_teste";

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
	@DisplayName("copia só as perícias preenchidas, remove as colunas antigas e preserva o resto da ficha")
	void migraSemPerderDados() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(url, usuario, senha);
		jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("drop schema if exists " + SCHEMA + " cascade");

		Flyway ate1 = Flyway.configure().dataSource(dataSource).schemas(SCHEMA).target("1").load();
		ate1.migrate();

		// formato antigo: perícias nas colunas atributeN
		jdbc.update("insert into " + SCHEMA + ".sheet_player (id, namej, namec, gender, age, str, \"int\", atribute1, atribute16, atribute23) "
				+ "overriding system value values (1, 'Ana', 'Lia', 'F', 30, 12, 15, 40, 55, 10)");
		jdbc.update("insert into " + SCHEMA + ".sheet_player (id, namej, namec, gender, lifea, atribute2, atribute12) "
				+ "overriding system value values (2, 'Bia', 'Rex', 'M', 9, 0, 7)");
		// ficha sem nenhuma perícia: não pode gerar linha
		jdbc.update("insert into " + SCHEMA + ".sheet_player (id, namej, namec, gender) "
				+ "overriding system value values (3, 'Caio', 'Zed', 'X')");

		Flyway ultima = Flyway.configure().dataSource(dataSource).schemas(SCHEMA).load();
		ultima.migrate();

		// as linhas de ficha_pericia são exatamente as esperadas (ficha, código, valor); o 0 é valor e não ausência
		List<String> linhas = new ArrayList<>();
		jdbc.query("select ficha_id, pericia, valor from " + SCHEMA + ".ficha_pericia order by ficha_id, pericia",
				rs -> {
					linhas.add(rs.getLong("ficha_id") + ":" + rs.getString("pericia") + "=" + rs.getInt("valor"));
				});
		assertThat(linhas).containsExactlyInAnyOrder(
				"1:ACROBACIA=40", "1:PERCEPCAO=55", "1:ARREMESSAR=10",
				"2:ATLETISMO=0", "2:INVESTIGACAO=7");

		// as colunas atributeN não existem mais
		List<String> colunasAntigas = jdbc.queryForList(
				"select column_name from information_schema.columns where table_schema = ? and table_name = 'sheet_player' "
						+ "and column_name like 'atribute%'", String.class, SCHEMA);
		assertThat(colunasAntigas).isEmpty();

		// os demais dados das fichas ficaram intactos
		Map<String, Object> ana = jdbc.queryForMap("select * from " + SCHEMA + ".sheet_player where id = 1");
		assertThat(ana).containsEntry("namej", "Ana").containsEntry("namec", "Lia").containsEntry("gender", "F")
				.containsEntry("age", 30).containsEntry("str", 12).containsEntry("int", 15)
				.containsEntry("lifea", null).containsEntry("pod", null);
		Map<String, Object> bia = jdbc.queryForMap("select * from " + SCHEMA + ".sheet_player where id = 2");
		assertThat(bia).containsEntry("namej", "Bia").containsEntry("lifea", 9).containsEntry("age", null);
		assertThat(jdbc.queryForObject("select count(*) from " + SCHEMA + ".sheet_player", Integer.class)).isEqualTo(3);

		// a V2 ficou registrada no histórico do Flyway
		assertThat(jdbc.queryForList("select version from " + SCHEMA + ".flyway_schema_history where success and version is not null order by installed_rank",
				String.class)).containsExactly("1", "2");
	}
}
