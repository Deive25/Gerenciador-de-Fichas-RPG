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
 * Registra o esquema que o Hibernate gera (ddl-auto=update) para a entidade SheetPlayer.
 * Quem já tem dados no banco depende destes nomes: se uma atualização do Hibernate ou do
 * Spring Boot mudar a estratégia de nomes, a aplicação passaria a enxergar colunas novas
 * e "perderia" as antigas. Também serve de linha de base para a migração com Flyway.
 */
@DisplayName("Esquema da tabela da ficha (colunas geradas pelo Hibernate)")
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
			esperado.put(c, "varchar/NO");
		}
		for (String c : List.of("age", "lifea", "lifem", "sana", "sanm",
				"str", "dex", "cons", "int", "car", "sab", "tam", "sor", "pod")) {
			esperado.put(c, "int/YES");
		}
		for (int i = 1; i <= 23; i++) {
			esperado.put("atribute" + i, "int/YES");
		}

		List<Map<String, Object>> tabelas = jdbc.queryForList(
				"select table_name from information_schema.tables where table_schema = database()");
		assertThat(tabelas).extracting(t -> t.get("TABLE_NAME")).containsExactly("sheet_player");

		Map<String, String> real = jdbc.queryForList(
				"select column_name, data_type, is_nullable from information_schema.columns "
						+ "where table_schema = database() and table_name = 'sheet_player'")
				.stream()
				.collect(Collectors.toMap(
						linha -> (String) linha.get("COLUMN_NAME"),
						linha -> linha.get("DATA_TYPE") + "/" + linha.get("IS_NULLABLE")));

		assertThat(real).containsExactlyInAnyOrderEntriesOf(esperado);
	}
}
