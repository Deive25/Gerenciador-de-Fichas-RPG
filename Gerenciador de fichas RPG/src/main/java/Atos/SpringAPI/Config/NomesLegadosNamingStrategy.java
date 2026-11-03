package Atos.SpringAPI.Config;

import java.util.Locale;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Reproduz a estratégia de nomes que o Spring Boot 2/3 aplicava por padrão
 * (CamelCaseToUnderscoresNamingStrategy): SheetPlayer vira sheet_player, nameJ vira namej
 * e STR vira str (só entra "_" entre minúscula, MAIÚSCULA e minúscula).
 *
 * Por que existe: o Boot 4 deixou de aplicar essa conversão e a estratégia equivalente do
 * Hibernate não altera identificadores entre aspas (o application.properties usa
 * globally_quoted_identifiers). Sem esta classe, o Hibernate criaria uma tabela nova
 * ("SheetPlayer") e não enxergaria os dados de quem já usa a aplicação. O teste
 * EsquemaBancoTest garante que os nomes continuam os mesmos.
 */
public class NomesLegadosNamingStrategy implements PhysicalNamingStrategy {

	@Override
	public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		return apply(name);
	}

	@Override
	public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		return apply(name);
	}

	@Override
	public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		return apply(name);
	}

	@Override
	public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		return apply(name);
	}

	@Override
	public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		return apply(name);
	}

	private Identifier apply(Identifier name) {
		if (name == null) {
			return null;
		}
		StringBuilder nome = new StringBuilder(name.getText().replace('.', '_'));
		for (int i = 1; i < nome.length() - 1; i++) {
			if (precisaDeUnderscore(nome.charAt(i - 1), nome.charAt(i), nome.charAt(i + 1))) {
				nome.insert(i++, '_');
			}
		}
		return new Identifier(nome.toString().toLowerCase(Locale.ROOT), name.isQuoted());
	}

	private boolean precisaDeUnderscore(char antes, char atual, char depois) {
		return Character.isLowerCase(antes) && Character.isUpperCase(atual) && Character.isLowerCase(depois);
	}
}
