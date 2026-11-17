package dev.spironello.fichasrpg;

import org.springframework.jdbc.core.JdbcTemplate;

/** Apoio dos testes da conta demo: apaga tudo o que as contas demo criaram (e as campanhas), na ordem das chaves estrangeiras. */
public final class ContasDemoDeTeste {

	private ContasDemoDeTeste() {
	}

	public static void apagarTodas(JdbcTemplate jdbc) {
		jdbc.update("delete from rolagem where usuario_id in (select id from usuario where demo)");
		jdbc.update("delete from ficha where dono_id in (select id from usuario where demo)");
		jdbc.update("delete from campanha");
		jdbc.update("delete from refresh_token where usuario_id in (select id from usuario where demo)");
		jdbc.update("delete from usuario where demo");
	}
}
