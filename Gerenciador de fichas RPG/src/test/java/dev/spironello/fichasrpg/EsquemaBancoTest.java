package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import dev.spironello.fichasrpg.repository.FichaRepository;

/**
 * Registra o esquema (ficha, ficha_ordem, ficha_ordem_pericia, ficha_dnd, ficha_dnd_pericia, retrato_ficha, ficha_habilidade, ficha_ordem_item, ficha_dnd_item, ficha_dnd_magia, ficha_dnd_espaco e ficha_ordem_ritual), que vem das migrações
 * Flyway e não do Hibernate: com ddl-auto=validate ele só confere.
 * O teste protege os nomes de coluna e a nulabilidade que as entidades esperam, e também mostra que
 * foi o Flyway quem criou o esquema (tabela flyway_schema_history com as versões 1 a 8 aplicadas).
 */
@DisplayName("Esquema do banco (criado pelo Flyway)")
class EsquemaBancoTest extends IntegrationTestBase {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private FichaRepository fichas;

	@Test
	@DisplayName("as tabelas do esquema são exatamente as esperadas")
	void tabelas() {
		List<Map<String, Object>> tabelas = jdbc.queryForList(
				"select table_name from information_schema.tables where table_schema = current_schema()");
		assertThat(tabelas).extracting(t -> t.get("table_name")).containsExactlyInAnyOrder(
				"ficha", "ficha_ordem", "ficha_ordem_pericia", "ficha_dnd", "ficha_dnd_pericia", "retrato_ficha",
				"ficha_habilidade", "ficha_ordem_item", "ficha_dnd_item", "ficha_dnd_magia", "ficha_dnd_espaco",
				"ficha_ordem_ritual", "flyway_schema_history");
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
		for (String c : List.of("aparencia", "historia", "anotacoes")) {
			esperado.put(c, "character varying/YES");
		}

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
		for (String c : List.of("pc", "pp", "pe", "po", "pl")) {
			esperado.put(c, "integer/NO");
		}
		esperado.put("atributo_conjuracao", "character varying/YES");

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
	@DisplayName("a tabela ficha_habilidade tem as colunas, os tipos e a nulabilidade esperados")
	void esquemaDasHabilidades() {
		Map<String, String> esperado = new LinkedHashMap<>();
		esperado.put("id", "bigint/NO");
		esperado.put("ficha_id", "bigint/NO");
		esperado.put("nome", "character varying/NO");
		esperado.put("tipo", "character varying/NO");
		esperado.put("descricao", "character varying/YES");

		assertThat(colunas("ficha_habilidade")).containsExactlyInAnyOrderEntriesOf(esperado);
	}

	@Test
	@DisplayName("a tabela ficha_ordem_item tem as colunas, os tipos e a nulabilidade esperados")
	void esquemaDosItensDeOrdem() {
		Map<String, String> esperado = new LinkedHashMap<>();
		esperado.put("id", "bigint/NO");
		esperado.put("ficha_id", "bigint/NO");
		esperado.put("nome", "character varying/NO");
		for (String c : List.of("categoria", "espacos", "quantidade")) {
			esperado.put(c, "integer/NO");
		}
		esperado.put("descricao", "character varying/YES");

		assertThat(colunas("ficha_ordem_item")).containsExactlyInAnyOrderEntriesOf(esperado);
	}

	@Test
	@DisplayName("a tabela ficha_dnd_item tem as colunas, os tipos e a nulabilidade esperados")
	void esquemaDosItensDnd() {
		Map<String, String> esperado = new LinkedHashMap<>();
		esperado.put("id", "bigint/NO");
		esperado.put("ficha_id", "bigint/NO");
		esperado.put("nome", "character varying/NO");
		esperado.put("quantidade", "integer/NO");
		esperado.put("peso", "numeric/NO");
		esperado.put("equipado", "boolean/NO");
		esperado.put("descricao", "character varying/YES");

		assertThat(colunas("ficha_dnd_item")).containsExactlyInAnyOrderEntriesOf(esperado);
	}

	@Test
	@DisplayName("a tabela ficha_dnd_magia tem as colunas, os tipos e a nulabilidade esperados")
	void esquemaDasMagias() {
		Map<String, String> esperado = new LinkedHashMap<>();
		esperado.put("id", "bigint/NO");
		esperado.put("ficha_id", "bigint/NO");
		esperado.put("nome", "character varying/NO");
		esperado.put("nivel", "integer/NO");
		for (String c : List.of("escola", "tempo", "alcance", "componentes", "duracao", "descricao")) {
			esperado.put(c, "character varying/YES");
		}
		for (String c : List.of("concentracao", "ritual", "preparada")) {
			esperado.put(c, "boolean/NO");
		}

		assertThat(colunas("ficha_dnd_magia")).containsExactlyInAnyOrderEntriesOf(esperado);
	}

	@Test
	@DisplayName("a tabela ficha_dnd_espaco tem as colunas e a nulabilidade esperadas")
	void esquemaDosEspacos() {
		Map<String, String> esperado = new LinkedHashMap<>();
		for (String c : List.of("ficha_id", "nivel", "total", "usados")) {
			esperado.put(c, c.equals("ficha_id") ? "bigint/NO" : "integer/NO");
		}

		assertThat(colunas("ficha_dnd_espaco")).containsExactlyInAnyOrderEntriesOf(esperado);
	}

	@Test
	@DisplayName("a tabela ficha_ordem_ritual tem as colunas, os tipos e a nulabilidade esperados")
	void esquemaDosRituais() {
		Map<String, String> esperado = new LinkedHashMap<>();
		esperado.put("id", "bigint/NO");
		esperado.put("ficha_id", "bigint/NO");
		esperado.put("nome", "character varying/NO");
		esperado.put("circulo", "integer/NO");
		esperado.put("elemento", "character varying/NO");
		for (String c : List.of("execucao", "alcance", "alvo", "duracao", "resistencia", "descricao")) {
			esperado.put(c, "character varying/YES");
		}

		assertThat(colunas("ficha_ordem_ritual")).containsExactlyInAnyOrderEntriesOf(esperado);
	}

	@Test
	@DisplayName("os CHECKs da V8 recusam no banco o que a API também recusa")
	void checksDaV8() {
		Long dnd = fichas.save(FichasDeTeste.dnd("Bram")).getId();
		Long ordem = fichas.save(FichasDeTeste.ordem("Lia")).getId();
		try {
			assertThatThrownBy(() -> jdbc.update("update ficha_dnd set atributo_conjuracao = 'FOR' where ficha_id = ?", dnd))
					.isInstanceOf(DataIntegrityViolationException.class);
			assertThatThrownBy(() -> jdbc.update(
					"insert into ficha_dnd_magia (ficha_id, nome, nivel) values (?, 'X', 10)", dnd))
					.isInstanceOf(DataIntegrityViolationException.class);
			assertThatThrownBy(() -> jdbc.update(
					"insert into ficha_dnd_espaco (ficha_id, nivel, total, usados) values (?, 0, 1, 0)", dnd))
					.isInstanceOf(DataIntegrityViolationException.class);
			assertThatThrownBy(() -> jdbc.update(
					"insert into ficha_dnd_espaco (ficha_id, nivel, total, usados) values (?, 3, 2, 3)", dnd))
					.isInstanceOf(DataIntegrityViolationException.class);
			assertThatThrownBy(() -> jdbc.update(
					"insert into ficha_dnd_espaco (ficha_id, nivel, total, usados) values (?, 3, -1, 0)", dnd))
					.isInstanceOf(DataIntegrityViolationException.class);
			assertThatThrownBy(() -> jdbc.update(
					"insert into ficha_ordem_ritual (ficha_id, nome, circulo, elemento) values (?, 'X', 5, 'SANGUE')", ordem))
					.isInstanceOf(DataIntegrityViolationException.class);
			assertThatThrownBy(() -> jdbc.update(
					"insert into ficha_ordem_ritual (ficha_id, nome, circulo, elemento) values (?, 'X', 1, 'FOGO')", ordem))
					.isInstanceOf(DataIntegrityViolationException.class);
			// Os valores limite passam: usados igual ao total, truque (nível 0) e círculo 4.
			jdbc.update("insert into ficha_dnd_espaco (ficha_id, nivel, total, usados) values (?, 3, 2, 2)", dnd);
			jdbc.update("insert into ficha_dnd_magia (ficha_id, nome, nivel) values (?, 'Luz', 0)", dnd);
			jdbc.update("insert into ficha_ordem_ritual (ficha_id, nome, circulo, elemento) values (?, 'X', 4, 'MEDO')", ordem);
		} finally {
			fichas.deleteAll();
		}
		// A FK com on delete cascade levou as linhas junto com a ficha.
		assertThat(jdbc.queryForObject("select count(*) from ficha_dnd_espaco", Integer.class)).isZero();
		assertThat(jdbc.queryForObject("select count(*) from ficha_dnd_magia", Integer.class)).isZero();
		assertThat(jdbc.queryForObject("select count(*) from ficha_ordem_ritual", Integer.class)).isZero();
	}

	@Test
	@DisplayName("o Flyway aplicou as migrações V1, V2, V3, V4, V5, V6, V7 e V8 com sucesso")
	void flywayAplicouAsMigracoes() {
		List<Map<String, Object>> historico = jdbc.queryForList(
				"select version, description, success from flyway_schema_history order by installed_rank");
		assertThat(historico).hasSize(8);
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
		assertThat(historico.get(5))
				.containsEntry("version", "6")
				.containsEntry("description", "descricao e habilidades da ficha")
				.containsEntry("success", true);
		assertThat(historico.get(6))
				.containsEntry("version", "7")
				.containsEntry("description", "inventario da ficha")
				.containsEntry("success", true);
		assertThat(historico.get(7))
				.containsEntry("version", "8")
				.containsEntry("description", "magias e rituais")
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
