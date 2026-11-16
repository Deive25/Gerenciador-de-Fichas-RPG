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
 * Prova que a migração V11 não mexe nas fichas que já existiam (campanha nula, versão 0, mesmos donos) e que as
 * restrições novas valem no banco. Mesmo roteiro do MigracaoLoginTest: schema próprio no mesmo container, migra até a
 * V10, insere dados no formato antigo, migra até a V11 e confere.
 */
@DisplayName("Migração V11: campanhas, participações e versão da ficha")
class MigracaoCampanhasTest extends IntegrationTestBase {

	private static final String SCHEMA = "migracao_campanhas";

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

	private void prepararSchema() {
		dataSource = new DriverManagerDataSource(url, usuario, senha);
		jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("drop schema if exists " + SCHEMA + " cascade");
	}

	private long novoUsuario(String email, String nome) {
		return jdbc.queryForObject("insert into " + SCHEMA + ".usuario (email, nome) values (?, ?) returning id", Long.class, email, nome);
	}

	private long novaFicha(long donoId, String personagem, Long campanhaId) {
		return jdbc.queryForObject("insert into " + SCHEMA + ".ficha (jogador, personagem, genero, sistema, dono_id, campanha_id) "
				+ "values ('Ana', ?, 'F', 'DND', ?, ?) returning id", Long.class, personagem, donoId, campanhaId);
	}

	private long novaCampanha(String nome, String codigo) {
		return jdbc.queryForObject("insert into " + SCHEMA + ".campanha (nome, codigo_convite) values (?, ?) returning id", Long.class, nome, codigo);
	}

	@Test
	@DisplayName("as fichas da V10 ficam com campanha nula, versão 0 e os mesmos donos; nenhum dado muda")
	void fichasAntigasFicamSemCampanha() {
		prepararSchema();
		migrarAte("10");
		long arthur = novoUsuario("a@x.com", "Arthur");
		long bia = novoUsuario("b@x.com", "Bia");
		long f1 = jdbc.queryForObject("insert into " + SCHEMA + ".ficha (jogador, personagem, genero, sistema, dono_id) "
				+ "values ('Ana', 'Lia', 'F', 'ORDEM_PARANORMAL', ?) returning id", Long.class, arthur);
		jdbc.update("insert into " + SCHEMA + ".ficha_ordem (ficha_id, nex, agilidade, forca, intelecto, presenca, vigor) values (?, 5, 1, 1, 1, 1, 1)", f1);
		long f2 = jdbc.queryForObject("insert into " + SCHEMA + ".ficha (jogador, personagem, genero, sistema, dono_id) "
				+ "values ('Bia', 'Rex', 'M', 'DND', ?) returning id", Long.class, bia);
		jdbc.update("insert into " + SCHEMA + ".ficha_dnd (ficha_id, edicao, nivel, forca, destreza, constituicao, inteligencia, sabedoria, carisma) "
				+ "values (?, 'D2024', 1, 10, 10, 10, 10, 10, 10)", f2);

		migrarAte("11");

		List<Map<String, Object>> fichas = jdbc.queryForList("select id, dono_id, personagem, campanha_id, versao from " + SCHEMA + ".ficha order by id");
		assertThat(fichas).hasSize(2);
		assertThat(fichas.get(0)).containsEntry("id", f1).containsEntry("personagem", "Lia").containsEntry("dono_id", arthur)
				.containsEntry("campanha_id", null).containsEntry("versao", 0L);
		assertThat(fichas.get(1)).containsEntry("id", f2).containsEntry("personagem", "Rex").containsEntry("dono_id", bia)
				.containsEntry("campanha_id", null).containsEntry("versao", 0L);
		assertThat(jdbc.queryForObject("select count(*) from " + SCHEMA + ".campanha", Integer.class)).isZero();
		assertThat(jdbc.queryForObject("select count(*) from " + SCHEMA + ".participacao", Integer.class)).isZero();

		assertThat(jdbc.queryForList("select version from " + SCHEMA + ".flyway_schema_history where success and version is not null order by installed_rank",
				String.class)).last().isEqualTo("11");
	}

	@Test
	@DisplayName("as restrições: um mestre por campanha, papel válido, código único e a ficha só numa campanha de que o dono participa")
	void restricoesDasTabelasNovas() {
		prepararSchema();
		migrarAte("11");
		long m = novoUsuario("m@x.com", "Mestre");
		long j = novoUsuario("j@x.com", "Jogador");
		long outro = novoUsuario("o@x.com", "Outro");
		long campanha = novaCampanha("Mesa", "AAAAAAAAAAAA");
		String inserirParticipacao = "insert into " + SCHEMA + ".participacao (campanha_id, usuario_id, papel) values (?, ?, ?)";
		jdbc.update(inserirParticipacao, campanha, m, "MESTRE");
		jdbc.update(inserirParticipacao, campanha, j, "JOGADOR");

		assertThatThrownBy(() -> jdbc.update(inserirParticipacao, campanha, outro, "MESTRE"))
				.as("no máximo um mestre").isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update(inserirParticipacao, campanha, outro, "DONO"))
				.as("papel só MESTRE ou JOGADOR").isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> novaCampanha("Outra", "AAAAAAAAAAAA"))
				.as("código de convite único").isInstanceOf(DataIntegrityViolationException.class);

		assertThat(jdbc.queryForMap("select convite_ativo, descricao from " + SCHEMA + ".campanha where id = ?", campanha))
				.containsEntry("convite_ativo", true).containsEntry("descricao", null);

		novaFicha(j, "Dentro", campanha);
		assertThatThrownBy(() -> novaFicha(outro, "Fora", campanha))
				.as("ficha numa campanha de que o dono não participa").isInstanceOf(DataIntegrityViolationException.class);

		List<String> indices = jdbc.queryForList("select indexname from pg_indexes where schemaname = ?", String.class, SCHEMA);
		assertThat(indices).contains("ux_participacao_um_mestre", "ix_participacao_usuario", "ix_ficha_campanha");
	}

	@Test
	@DisplayName("sair da campanha (apagar a participação) devolve a ficha a 'sem campanha' sozinho, pela chave estrangeira composta")
	void saidaDevolveAFicha() {
		prepararSchema();
		migrarAte("11");
		long m = novoUsuario("m@x.com", "Mestre");
		long j = novoUsuario("j@x.com", "Jogador");
		long campanha = novaCampanha("Mesa", "BBBBBBBBBBBB");
		jdbc.update("insert into " + SCHEMA + ".participacao (campanha_id, usuario_id, papel) values (?, ?, 'MESTRE')", campanha, m);
		jdbc.update("insert into " + SCHEMA + ".participacao (campanha_id, usuario_id, papel) values (?, ?, 'JOGADOR')", campanha, j);
		long fichaDoJogador = novaFicha(j, "Do jogador", campanha);
		long fichaDoMestre = novaFicha(m, "Do mestre", campanha);

		jdbc.update("delete from " + SCHEMA + ".participacao where campanha_id = ? and usuario_id = ?", campanha, j);
		assertThat(jdbc.queryForObject("select campanha_id from " + SCHEMA + ".ficha where id = ?", Long.class, fichaDoJogador)).isNull();
		assertThat(jdbc.queryForObject("select campanha_id from " + SCHEMA + ".ficha where id = ?", Long.class, fichaDoMestre)).isEqualTo(campanha);

		jdbc.update("delete from " + SCHEMA + ".campanha where id = ?", campanha);
		assertThat(jdbc.queryForObject("select campanha_id from " + SCHEMA + ".ficha where id = ?", Long.class, fichaDoMestre)).isNull();
		assertThat(jdbc.queryForObject("select count(*) from " + SCHEMA + ".participacao", Integer.class)).isZero();
	}
}
