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
 * Registra o esquema (ficha, ficha_ordem, ficha_ordem_pericia, ficha_dnd, ficha_dnd_pericia e retrato_ficha), que vem das migrações
 * Flyway e não do Hibernate: com ddl-auto=validate ele só confere.
 * O teste protege os nomes de coluna e a nulabilidade que as entidades esperam, e também mostra que
 * foi o Flyway quem criou o esquema (tabela flyway_schema_history com as versões 1 a 5 aplicadas).
 */
@DisplayName("Esquema do banco (criado pelo Flyway)")
class EsquemaBancoTest extends IntegrationTestBase {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	@DisplayName("as tabelas do esquema são exatamente as esperadas")
	void tabelas() {
		List<Map<String, Object>> tabelas = jdbc.queryForList(
				"select table_name from information_schema.tables where table_schema = current_schema()");
		assertThat(tabelas).extracting(t -> t.get("table_name")).containsExactlyInAnyOrder(
				"ficha", "ficha_ordem", "ficha_ordem_pericia", "ficha_dnd", "ficha_dnd_pericia", "retrato_ficha",
				"flyway_schema_history");
	}

	@Test
	@DisplayName("a tabela ficha tem as colunas comuns, os tipos e a nulabilidade esperados")
	void esquemaDaFicha() {
		Map<String, String> esperado = new LinkedHashMap<>();
		esperado.put("id", "bigint/NO");
		for (String c : List.of("jogador", "personagem", "genero")) {
			esperado.put(c, "character varying/NO");
		}
		esperado.put("idade", "integer/YES");
		esperado.put("sistema", "character varying/NO");

		assertThat(colunas("ficha")).containsExactlyInAnyOrderEntriesOf(esperado);
	}

	@Test
	@DisplayName("a tabela ficha_ordem tem as colunas, os tipos e a nulabilidade esperados")
	void esquemaDaFichaOrdem() {
		Map<String, String> esperado = new LinkedHashMap<>();
		esperado.put("ficha_id", "bigint/NO");
		esperado.put("classe", "character varying/YES");
		esperado.put("origem", "character varying/YES");
		esperado.put("nex", "integer/NO");
		for (String c : List.of("agilidade", "forca", "intelecto", "presenca", "vigor")) {
			esperado.put(c, "integer/NO");
		}
		for (String c : List.of("pv_atual", "pv_max", "san_atual", "san_max", "pe_atual", "pe_max")) {
			esperado.put(c, "integer/YES");
		}

		assertThat(colunas("ficha_ordem")).containsExactlyInAnyOrderEntriesOf(esperado);
	}

	@Test
	@DisplayName("a tabela ficha_ordem_pericia tem as colunas e a nulabilidade esperadas")
	void esquemaDasPericias() {
		Map<String, String> esperado = new LinkedHashMap<>();
		esperado.put("ficha_id", "bigint/NO");
		esperado.put("pericia", "character varying/NO");
		esperado.put("grau", "character varying/NO");

		assertThat(colunas("ficha_ordem_pericia")).containsExactlyInAnyOrderEntriesOf(esperado);
	}

	@Test
	@DisplayName("a tabela ficha_dnd tem as colunas, os tipos e a nulabilidade esperados")
	void esquemaDaFichaDnd() {
		Map<String, String> esperado = new LinkedHashMap<>();
		esperado.put("ficha_id", "bigint/NO");
		esperado.put("edicao", "character varying/NO");
		for (String c : List.of("classe", "especie", "antecedente")) {
			esperado.put(c, "character varying/YES");
		}
		esperado.put("nivel", "integer/NO");
		for (String c : List.of("forca", "destreza", "constituicao", "inteligencia", "sabedoria", "carisma")) {
			esperado.put(c, "integer/NO");
		}
		for (String c : List.of("pv_atual", "pv_max", "pv_temporario", "ca")) {
			esperado.put(c, "integer/YES");
		}
		esperado.put("inspiracao", "boolean/NO");
		for (String c : List.of("salva_forca", "salva_destreza", "salva_constituicao", "salva_inteligencia",
				"salva_sabedoria", "salva_carisma")) {
			esperado.put(c, "boolean/NO");
		}

		assertThat(colunas("ficha_dnd")).containsExactlyInAnyOrderEntriesOf(esperado);
	}

	@Test
	@DisplayName("a tabela ficha_dnd_pericia tem as colunas e a nulabilidade esperadas")
	void esquemaDasPericiasDnd() {
		Map<String, String> esperado = new LinkedHashMap<>();
		esperado.put("ficha_id", "bigint/NO");
		esperado.put("pericia", "character varying/NO");
		esperado.put("grau", "character varying/NO");

		assertThat(colunas("ficha_dnd_pericia")).containsExactlyInAnyOrderEntriesOf(esperado);
	}

	@Test
	@DisplayName("a tabela retrato_ficha tem as colunas, os tipos e a nulabilidade esperados")
	void esquemaDaTabelaDeRetratos() {
		Map<String, String> esperado = new LinkedHashMap<>();
		esperado.put("ficha_id", "bigint/NO");
		esperado.put("conteudo", "bytea/NO");
		esperado.put("atualizado_em", "timestamp with time zone/NO");

		assertThat(colunas("retrato_ficha")).containsExactlyInAnyOrderEntriesOf(esperado);
	}

	@Test
	@DisplayName("o Flyway aplicou as migrações V1, V2, V3, V4 e V5 com sucesso")
	void flywayAplicouAsMigracoes() {
		List<Map<String, Object>> historico = jdbc.queryForList(
				"select version, description, success from flyway_schema_history order by installed_rank");
		assertThat(historico).hasSize(5);
		assertThat(historico.get(0))
				.containsEntry("version", "1")
				.containsEntry("description", "cria tabela sheet player")
				.containsEntry("success", true);
		assertThat(historico.get(1))
				.containsEntry("version", "2")
				.containsEntry("description", "move pericias para tabela propria")
				.containsEntry("success", true);
		assertThat(historico.get(2))
				.containsEntry("version", "3")
				.containsEntry("description", "cria tabela retrato ficha")
				.containsEntry("success", true);
		assertThat(historico.get(3))
				.containsEntry("version", "4")
				.containsEntry("description", "sistemas ordem paranormal")
				.containsEntry("success", true);
		assertThat(historico.get(4))
				.containsEntry("version", "5")
				.containsEntry("description", "cria tabelas dnd")
				.containsEntry("success", true);
	}

	/** Coluna -> "tipo/anulavel" da tabela, no schema atual. */
	private Map<String, String> colunas(String tabela) {
		return jdbc.queryForList(
				"select column_name, data_type, is_nullable from information_schema.columns "
						+ "where table_schema = current_schema() and table_name = ?", tabela)
				.stream()
				.collect(Collectors.toMap(
						linha -> (String) linha.get("column_name"),
						linha -> linha.get("data_type") + "/" + linha.get("is_nullable")));
	}
}
