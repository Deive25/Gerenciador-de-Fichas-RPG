package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Prova que a migração V4 converte as fichas d100 em fichas de Ordem Paranormal sem perder os dados
 * comuns e sem mudar ids (o retrato e as URLs dependem deles). Mesmo roteiro do MigracaoPericiasTest:
 * schema próprio no mesmo container, migra até a V3, insere fichas no formato antigo, migra até a V4 e confere.
 */
@DisplayName("Migração V4: sheet_player vira ficha + ficha_ordem")
class MigracaoSistemasTest extends IntegrationTestBase {

	private static final String SCHEMA = "migracao_sistemas";

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
	@DisplayName("converte as fichas d100 em Ordem: mesmos ids e dados comuns, NEX 5, atributos 1, PV e SAN copiados, retrato intacto")
	void converteFichasAntigas() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(url, usuario, senha);
		jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("drop schema if exists " + SCHEMA + " cascade");

		Flyway.configure().dataSource(dataSource).schemas(SCHEMA).target("3").load().migrate();

		// 1: com vida, sanidade, perícias e retrato; 2: vida e sanidade nulas; 3: sem perícias
		jdbc.update("insert into " + SCHEMA + ".sheet_player (id, namej, namec, gender, age, lifea, lifem, sana, sanm, str, \"int\") "
				+ "overriding system value values (1, 'Ana', 'Lia', 'F', 30, 8, 10, 6, 9, 12, 15)");
		jdbc.update("insert into " + SCHEMA + ".ficha_pericia (ficha_id, pericia, valor) values (1, 'ACROBACIA', 40), (1, 'PERCEPCAO', 55)");
		jdbc.update("insert into " + SCHEMA + ".retrato_ficha (ficha_id, conteudo, atualizado_em) "
				+ "values (1, decode('010203', 'hex'), '2026-01-01T00:00:00.123Z')");
		jdbc.update("insert into " + SCHEMA + ".sheet_player (id, namej, namec, gender) "
				+ "overriding system value values (2, 'Bia', 'Rex', 'M')");
		jdbc.update("insert into " + SCHEMA + ".ficha_pericia (ficha_id, pericia, valor) values (2, 'FURTIVIDADE', 7)");
		jdbc.update("insert into " + SCHEMA + ".sheet_player (id, namej, namec, gender, age) "
				+ "overriding system value values (3, 'Caio', 'Zed', 'X', 22)");

		Flyway.configure().dataSource(dataSource).schemas(SCHEMA).target("4").load().migrate();

		// dados comuns e sistema, com os mesmos ids
		List<Map<String, Object>> fichas = jdbc.queryForList(
				"select id, jogador, personagem, genero, idade, sistema from " + SCHEMA + ".ficha order by id");
		assertThat(fichas).hasSize(3);
		assertThat(fichas.get(0)).containsEntry("id", 1L).containsEntry("jogador", "Ana").containsEntry("personagem", "Lia")
				.containsEntry("genero", "F").containsEntry("idade", 30).containsEntry("sistema", "ORDEM_PARANORMAL");
		assertThat(fichas.get(1)).containsEntry("id", 2L).containsEntry("jogador", "Bia").containsEntry("idade", null)
				.containsEntry("sistema", "ORDEM_PARANORMAL");
		assertThat(fichas.get(2)).containsEntry("id", 3L).containsEntry("personagem", "Zed").containsEntry("idade", 22)
				.containsEntry("sistema", "ORDEM_PARANORMAL");

		// ficha_ordem: NEX 5, atributos 1, PV e SAN copiados, PE e classe nulos
		Map<String, Object> ordem1 = jdbc.queryForMap("select * from " + SCHEMA + ".ficha_ordem where ficha_id = 1");
		assertThat(ordem1).containsEntry("nex", 5).containsEntry("agilidade", 1).containsEntry("forca", 1)
				.containsEntry("intelecto", 1).containsEntry("presenca", 1).containsEntry("vigor", 1)
				.containsEntry("pv_atual", 8).containsEntry("pv_max", 10)
				.containsEntry("san_atual", 6).containsEntry("san_max", 9)
				.containsEntry("pe_atual", null).containsEntry("pe_max", null)
				.containsEntry("classe", null).containsEntry("origem", null);
		Map<String, Object> ordem2 = jdbc.queryForMap("select * from " + SCHEMA + ".ficha_ordem where ficha_id = 2");
		assertThat(ordem2).containsEntry("nex", 5).containsEntry("pv_atual", null).containsEntry("pv_max", null)
				.containsEntry("san_atual", null).containsEntry("san_max", null);
		assertThat(jdbc.queryForObject("select count(*) from " + SCHEMA + ".ficha_ordem", Integer.class)).isEqualTo(3);

		// as perícias d100 não têm equivalente: a tabela nova começa vazia
		assertThat(jdbc.queryForObject("select count(*) from " + SCHEMA + ".ficha_ordem_pericia", Integer.class)).isZero();

		// o retrato continua ligado à mesma ficha, com os mesmos bytes
		assertThat(jdbc.queryForObject("select count(*) from " + SCHEMA + ".retrato_ficha where ficha_id = 1", Integer.class)).isEqualTo(1);
		assertThat(jdbc.queryForObject("select conteudo from " + SCHEMA + ".retrato_ficha where ficha_id = 1", byte[].class))
				.containsExactly(1, 2, 3);

		// o que era do d100 saiu
		assertThat(jdbc.queryForObject("select to_regclass('" + SCHEMA + ".ficha_pericia')", String.class)).isNull();
		assertThat(jdbc.queryForObject("select to_regclass('" + SCHEMA + ".sheet_player')", String.class)).isNull();
		List<String> colunasAntigas = jdbc.queryForList(
				"select column_name from information_schema.columns where table_schema = ? and table_name = 'ficha' "
						+ "and column_name in ('namej','namec','gender','age','lifea','lifem','sana','sanm','str','dex','cons','int','car','sab','tam','sor','pod')",
				String.class, SCHEMA);
		assertThat(colunasAntigas).isEmpty();

		assertThat(jdbc.queryForList("select version from " + SCHEMA + ".flyway_schema_history where success and version is not null order by installed_rank",
				String.class)).containsExactly("1", "2", "3", "4");
	}
}
