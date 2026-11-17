package dev.spironello.fichasrpg.rolagem;

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

import dev.spironello.fichasrpg.IntegrationTestBase;

/**
 * Prova que a V12 só acrescenta a tabela das rolagens: as fichas, as campanhas e as participações da V11 ficam iguais.
 * Mesmo roteiro do MigracaoCampanhasTest (schema próprio no mesmo container).
 */
@DisplayName("Migração V12: rolagens da mesa")
class MigracaoRolagensTest extends IntegrationTestBase {

	private static final String SCHEMA = "migracao_rolagens";

	@Value("${spring.datasource.url}")
	private String url;
	@Value("${spring.datasource.username}")
	private String usuario;
	@Value("${spring.datasource.password}")
	private String senha;

	private JdbcTemplate jdbc;
	private DriverManagerDataSource dataSource;

	@AfterEach
	void limparSchema() {
		if (jdbc != null) {
			jdbc.execute("drop schema if exists " + SCHEMA + " cascade");
		}
	}

	private void migrarAte(String versao) {
		Flyway.configure().dataSource(dataSource).schemas(SCHEMA).target(versao).load().migrate();
	}

	private long id(String sql, Object... args) {
		return jdbc.queryForObject(sql, Long.class, args);
	}

	@Test
	@DisplayName("os dados da V11 ficam iguais depois da V12, e as restrições da tabela nova valem")
	void dadosDaV11FicamIguais() {
		dataSource = new DriverManagerDataSource(url, usuario, senha);
		jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("drop schema if exists " + SCHEMA + " cascade");
		migrarAte("11");
		long m = id("insert into " + SCHEMA + ".usuario (email, nome) values ('m@x.com', 'Mestre') returning id");
		long j = id("insert into " + SCHEMA + ".usuario (email, nome) values ('j@x.com', 'Jogador') returning id");
		long campanha = id("insert into " + SCHEMA + ".campanha (nome, codigo_convite) values ('Mesa', 'CCCCCCCCCCCC') returning id");
		jdbc.update("insert into " + SCHEMA + ".participacao (campanha_id, usuario_id, papel) values (?, ?, 'MESTRE')", campanha, m);
		jdbc.update("insert into " + SCHEMA + ".participacao (campanha_id, usuario_id, papel) values (?, ?, 'JOGADOR')", campanha, j);
		long ficha = id("insert into " + SCHEMA + ".ficha (jogador, personagem, genero, sistema, dono_id, campanha_id) "
				+ "values ('Ana', 'Kai', 'F', 'DND', ?, ?) returning id", j, campanha);
		String fichas = "select id, dono_id, personagem, campanha_id, versao from " + SCHEMA + ".ficha order by id";
		String campanhas = "select id, nome, codigo_convite, convite_ativo from " + SCHEMA + ".campanha order by id";
		String participacoes = "select campanha_id, usuario_id, papel from " + SCHEMA + ".participacao order by usuario_id";
		List<Map<String, Object>> fichasAntes = jdbc.queryForList(fichas);
		List<Map<String, Object>> campanhasAntes = jdbc.queryForList(campanhas);
		List<Map<String, Object>> participacoesAntes = jdbc.queryForList(participacoes);

		migrarAte("12");

		assertThat(jdbc.queryForList(fichas)).isEqualTo(fichasAntes).hasSize(1);
		assertThat(jdbc.queryForList(campanhas)).isEqualTo(campanhasAntes).hasSize(1);
		assertThat(jdbc.queryForList(participacoes)).isEqualTo(participacoesAntes).hasSize(2);
		assertThat(jdbc.queryForObject("select count(*) from " + SCHEMA + ".rolagem", Integer.class)).isZero();
		assertThat(jdbc.queryForList("select version from " + SCHEMA + ".flyway_schema_history where success and version is not null order by installed_rank",
				String.class)).last().isEqualTo("12");

		String inserir = "insert into " + SCHEMA + ".rolagem (campanha_id, ficha_id, usuario_id, personagem, tipo, rotulo, total, detalhe) "
				+ "values (?, ?, ?, 'Kai', ?, 'Furtividade', 25, '{\"total\": 25}'::jsonb)";
		jdbc.update(inserir, campanha, ficha, j, "TESTE");
		assertThatThrownBy(() -> jdbc.update(inserir, campanha, ficha, j, "SECRETA"))
				.as("tipo só TESTE, ATAQUE ou LIVRE").isInstanceOf(DataIntegrityViolationException.class);

		jdbc.update("delete from " + SCHEMA + ".ficha where id = ?", ficha);
		assertThat(jdbc.queryForObject("select ficha_id from " + SCHEMA + ".rolagem", Long.class))
				.as("apagar a ficha mantém a rolagem, com ficha_id nulo").isNull();
		jdbc.update("delete from " + SCHEMA + ".campanha where id = ?", campanha);
		assertThat(jdbc.queryForObject("select count(*) from " + SCHEMA + ".rolagem", Integer.class))
				.as("apagar a campanha leva o histórico junto").isZero();
		assertThat(jdbc.queryForList("select indexname from pg_indexes where schemaname = ?", String.class, SCHEMA))
				.contains("ix_rolagem_campanha");
	}
}
