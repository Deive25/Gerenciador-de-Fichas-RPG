package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Prova que a migração V10 passa as fichas que já existiam (dos dois sistemas) para o usuário "legado" sem mudar ids,
 * e que o dono passa a ser obrigatório. Mesmo roteiro do MigracaoSistemasTest: schema próprio no mesmo container,
 * migra até a V9, insere fichas no formato antigo, migra até a V10 e confere.
 */
@DisplayName("Migração V10: usuários e dono da ficha")
class MigracaoLoginTest extends IntegrationTestBase {

	private static final String SCHEMA = "migracao_login";

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
	@DisplayName("as fichas de Ordem e de D&D da V9 ficam com o legado (mesmos ids), o legado não entra com senha e o dono_id vira obrigatório")
	void fichasAntigasVaoParaOLegado() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(url, usuario, senha);
		jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("drop schema if exists " + SCHEMA + " cascade");

		Flyway.configure().dataSource(dataSource).schemas(SCHEMA).target("9").load().migrate();

		jdbc.update("insert into " + SCHEMA + ".ficha (id, jogador, personagem, genero, sistema) overriding system value "
				+ "values (1, 'Ana', 'Lia', 'F', 'ORDEM_PARANORMAL')");
		jdbc.update("insert into " + SCHEMA + ".ficha_ordem (ficha_id, nex, agilidade, forca, intelecto, presenca, vigor) "
				+ "values (1, 5, 1, 1, 1, 1, 1)");
		jdbc.update("insert into " + SCHEMA + ".ficha (id, jogador, personagem, genero, sistema) overriding system value "
				+ "values (2, 'Bia', 'Rex', 'M', 'DND')");
		jdbc.update("insert into " + SCHEMA + ".ficha_dnd (ficha_id, edicao, nivel, forca, destreza, constituicao, inteligencia, sabedoria, carisma) "
				+ "values (2, 'D2024', 1, 10, 10, 10, 10, 10, 10)");

		Flyway.configure().dataSource(dataSource).schemas(SCHEMA).target("10").load().migrate();

		// o legado existe, não entra com senha e o e-mail é de um domínio reservado
		Map<String, Object> legado = jdbc.queryForMap("select id, email, nome, senha_hash from " + SCHEMA
				+ ".usuario where email = 'legado@fichas-rpg.invalid'");
		assertThat(legado).containsEntry("nome", "Fichas antigas").containsEntry("senha_hash", null);
		assertThat(jdbc.queryForObject("select count(*) from " + SCHEMA + ".usuario", Integer.class)).isEqualTo(1);

		// todas as fichas ficaram com o legado, com os mesmos ids
		List<Map<String, Object>> fichas = jdbc.queryForList("select id, dono_id, personagem from " + SCHEMA + ".ficha order by id");
		assertThat(fichas).hasSize(2);
		assertThat(fichas.get(0)).containsEntry("id", 1L).containsEntry("personagem", "Lia").containsEntry("dono_id", legado.get("id"));
		assertThat(fichas.get(1)).containsEntry("id", 2L).containsEntry("personagem", "Rex").containsEntry("dono_id", legado.get("id"));

		// dono_id é obrigatório (NOT NULL) e tem chave estrangeira para usuario
		assertThat(jdbc.queryForObject("select is_nullable from information_schema.columns where table_schema = ? "
				+ "and table_name = 'ficha' and column_name = 'dono_id'", String.class, SCHEMA)).isEqualTo("NO");
		assertThatThrownBy(() -> jdbc.update("insert into " + SCHEMA + ".ficha (jogador, personagem, genero, sistema) "
				+ "values ('x', 'y', 'F', 'DND')")).isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update("insert into " + SCHEMA + ".ficha (jogador, personagem, genero, sistema, dono_id) "
				+ "values ('x', 'y', 'F', 'DND', 999999)")).isInstanceOf(DataIntegrityViolationException.class);

		// índices de dono e de família
		List<String> indices = jdbc.queryForList("select indexname from pg_indexes where schemaname = ?", String.class, SCHEMA);
		assertThat(indices).contains("ix_ficha_dono", "ix_refresh_token_familia");

		assertThat(jdbc.queryForList("select version from " + SCHEMA + ".flyway_schema_history where success and version is not null order by installed_rank",
				String.class)).last().isEqualTo("10");
	}

	@Test
	@DisplayName("as restrições do e-mail e do hash: e-mail em minúsculas e único, hash do refresh único e de 64 caracteres")
	void restricoesDasTabelasNovas() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(url, usuario, senha);
		jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("drop schema if exists " + SCHEMA + " cascade");
		Flyway.configure().dataSource(dataSource).schemas(SCHEMA).target("10").load().migrate();

		jdbc.update("insert into " + SCHEMA + ".usuario (email, nome) values ('ana@x.com', 'Ana')");
		assertThatThrownBy(() -> jdbc.update("insert into " + SCHEMA + ".usuario (email, nome) values ('Bia@X.com', 'Bia')"))
				.as("CHECK email = lower(email)").isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update("insert into " + SCHEMA + ".usuario (email, nome) values ('ana@x.com', 'Outra')"))
				.as("e-mail único").isInstanceOf(DataIntegrityViolationException.class);

		Long id = jdbc.queryForObject("select id from " + SCHEMA + ".usuario where email = 'ana@x.com'", Long.class);
		String hash = "a".repeat(64);
		String inserir = "insert into " + SCHEMA + ".refresh_token (usuario_id, familia, hash, criado_em, expira_em) "
				+ "values (?, gen_random_uuid(), ?, now(), now())";
		jdbc.update(inserir, id, hash);
		assertThatThrownBy(() -> jdbc.update(inserir, id, hash)).as("hash único").isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update(inserir, id, "a".repeat(65))).as("hash de até 64").isInstanceOf(DataIntegrityViolationException.class);
	}
}
