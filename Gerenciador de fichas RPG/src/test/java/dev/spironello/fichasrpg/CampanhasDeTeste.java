package dev.spironello.fichasrpg;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import dev.spironello.fichasrpg.campanha.Papel;
import dev.spironello.fichasrpg.repository.FichaRepository;

/**
 * Apoio dos testes de campanha: usuários, campanhas e participações criados direto pelo JDBC, com ids fixos, e a
 * limpeza (as fichas dos usuários, depois as campanhas, depois os usuários, nessa ordem por causa das chaves
 * estrangeiras). Quem não é o usuário de teste padrão age com {@link #como(long)}.
 */
public final class CampanhasDeTeste {

	private static final AtomicLong CONTADOR = new AtomicLong();

	private CampanhasDeTeste() {
	}

	/** O pedido feito por este usuário (sobrepõe o usuário de teste padrão da base). */
	public static RequestPostProcessor como(long usuarioId) {
		return jwt().jwt(j -> j.subject(String.valueOf(usuarioId)));
	}

	public static void usuario(JdbcTemplate jdbc, long id, String nome) {
		jdbc.update("insert into usuario (id, email, nome) values (?, ?, ?) on conflict do nothing",
				id, "u" + id + "@fichas-rpg.invalid", nome);
	}

	/** Cria a campanha com um código de convite único (12 caracteres) e devolve o id. */
	public static long campanha(JdbcTemplate jdbc, String nome) {
		String codigo = String.format("T%011d", CONTADOR.incrementAndGet());
		return jdbc.queryForObject("insert into campanha (nome, codigo_convite) values (?, ?) returning id", Long.class, nome, codigo);
	}

	public static void participar(JdbcTemplate jdbc, long campanhaId, long usuarioId, Papel papel) {
		jdbc.update("insert into participacao (campanha_id, usuario_id, papel) values (?, ?, ?)", campanhaId, usuarioId, papel.name());
	}

	/** Apaga as fichas dos usuários, todas as campanhas (e as participações) e os usuários criados pelo teste. */
	public static void limpar(JdbcTemplate jdbc, FichaRepository fichas, long... usuarios) {
		for (long usuario : usuarios) {
			fichas.deleteAll(fichas.findAllByDonoId(usuario));
		}
		jdbc.update("delete from campanha");
		for (long usuario : usuarios) {
			jdbc.update("delete from usuario where id = ?", usuario);
		}
	}
}
