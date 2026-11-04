package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Registra o esquema da tabela sheet_player, que agora vem das migrações Flyway
 * (V1__cria_tabela_sheet_player.sql) e não mais do Hibernate: com ddl-auto=validate ele só confere.
 * O teste protege os nomes de coluna e a nulabilidade que a entidade espera, e também mostra que
 * foi o Flyway quem criou o esquema (tabela flyway_schema_history com a versão 1 aplicada).
 */
@DisplayName("Esquema da tabela da ficha (criado pelo Flyway)")
class EsquemaBancoTest extends IntegrationTestBase {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	@DisplayName("a tabela sheet_player tem exatamente as colunas, tipos e nulabilidade esperados")
	void esquemaDaTabela() {
		// formato: nome da coluna -> "tipo/anulavel"
		Map<String, String> esperado = new LinkedHashMap<>();
		esperado.put("id", "bigint/NO");
		for (String c : List.of("namej", "namec", "gender")) {
			esperado.put(c, "character varying/NO");
		}
		for (String c : List.of("age", "lifea", "lifem", "sana", "sanm",
				"str", "dex", "cons", "int", "car", "sab", "tam", "sor", "pod")) {
			esperado.put(c, "integer/YES");
		}
		for (int i = 1; i <= 23; i++) {
			esperado.put("atribute" + i, "integer/YES");
		}

		List<Map<String, Object>> tabelas = jdbc.queryForList(
				"select table_name from information_schema.tables where table_schema = current_schema()");
		assertThat(tabelas).extracting(t -> t.get("table_name")).containsExactlyInAnyOrder("sheet_player", "flyway_schema_history");

		Map<String, String> real = jdbc.queryForList(
				"select column_name, data_type, is_nullable from information_schema.columns "
						+ "where table_schema = current_schema() and table_name = 'sheet_player'")
				.stream()
				.collect(Collectors.toMap(
						linha -> (String) linha.get("column_name"),
						linha -> linha.get("data_type") + "/" + linha.get("is_nullable")));

		assertThat(real).containsExactlyInAnyOrderEntriesOf(esperado);
	}

	@Test
	@DisplayName("o Flyway aplicou a migração V1 com sucesso")
	void flywayAplicouAV1() {
		List<Map<String, Object>> historico = jdbc.queryForList(
				"select version, description, success from flyway_schema_history");
		assertThat(historico).hasSize(1);
		assertThat(historico.get(0))
				.containsEntry("version", "1")
				.containsEntry("description", "cria tabela sheet player")
				.containsEntry("success", true);
	}
}
