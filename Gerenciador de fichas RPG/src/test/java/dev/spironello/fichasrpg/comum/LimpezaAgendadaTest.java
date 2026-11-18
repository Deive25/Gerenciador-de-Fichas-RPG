package dev.spironello.fichasrpg.comum;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.ActiveProfiles;

import dev.spironello.fichasrpg.AutenticacaoDeTesteConfig;
import dev.spironello.fichasrpg.CampanhasDeTeste;
import dev.spironello.fichasrpg.ContasDemoDeTeste;
import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.RelogioDeTesteConfig.RelogioAjustavel;
import dev.spironello.fichasrpg.auth.DemoService;
import dev.spironello.fichasrpg.auth.RespostaDemo;
import dev.spironello.fichasrpg.campanha.Papel;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.repository.FichaRepository;

/**
 * A limpeza das contas demo velhas e dos refresh vencidos (2.6, spec §5.5). O foco é o que NÃO pode apagar: conta real,
 * inclusive a que está na mesma campanha que um demo. O relógio é o do teste: a conta "velha" nasce em INICIO e a
 * limpeza roda 25 h depois; a "recente" nasce 2 h depois de INICIO e tem 23 h na hora da limpeza.
 */
@ActiveProfiles("demo")
@DisplayName("Limpeza agendada (contas demo velhas e refresh vencidos)")
class LimpezaAgendadaTest extends IntegrationTestBase {

	private static final Instant INICIO = Instant.parse("2026-01-01T00:00:00Z");
	private static final long A = AutenticacaoDeTesteConfig.USUARIO_DE_TESTE_ID;
	private static final long RM = 3_800_001L;

	@Autowired
	private LimpezaAgendada limpeza;
	@Autowired
	private DemoService demo;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private FichaRepository fichas;
	@Autowired
	private RelogioAjustavel relogio;
	@Autowired
	private ConfigurableApplicationContext contexto;

	@BeforeEach
	void zerar() {
		relogio.definir(INICIO);
		limparTudo();
	}

	@AfterEach
	void limpar() {
		limparTudo();
		relogio.definir(INICIO);
	}

	private void limparTudo() {
		ContasDemoDeTeste.apagarTodas(jdbc);
		jdbc.update("delete from refresh_token where usuario_id in (?, ?)", A, RM);
		fichas.deleteAll(fichas.findAllByDonoId(A));
		fichas.deleteAll(fichas.findAllByDonoId(RM));
		jdbc.update("delete from usuario where id = ?", RM);
	}

	@Test
	@DisplayName("apaga a conta demo com mais de 24 h e tudo o que é dela; mantém a recente e todas as reais")
	void apagaSoODemoVelho() {
		// 1) dois demo velhos (o mestre e um jogador), com mesa, fichas, retrato, rolagem e refresh
		RespostaDemo velho = demo.entrar(null);
		RespostaDemo velhoJogador = demo.entrar(codigoDa(velho.campanhaId()));
		long mesaVelha = velho.campanhaId();
		List<Long> fichasVelhas = jdbc.queryForList("select id from ficha where campanha_id = ?", Long.class, mesaVelha);
		assertThat(fichasVelhas).hasSize(3);
		jdbc.update("insert into retrato_ficha (ficha_id, conteudo, atualizado_em) values (?, ?, now())", fichasVelhas.get(0), new byte[] { 1, 2, 3 });
		rolagem(mesaVelha, fichasVelhas.get(0), velho.usuario().id());
		rolagem(mesaVelha, fichasVelhas.get(2), velhoJogador.usuario().id());
		// um refresh já trocado, que aponta para o filho (a chave estrangeira circular da própria tabela)
		long pai = jdbc.queryForObject("select id from refresh_token where usuario_id = ?", Long.class, velho.usuario().id());
		long filho = jdbc.queryForObject("insert into refresh_token (usuario_id, familia, hash, criado_em, expira_em) "
				+ "select usuario_id, familia, 'f' || repeat('0', 63), criado_em, expira_em from refresh_token where id = ? returning id", Long.class, pai);
		jdbc.update("update refresh_token set revogado_em = now(), substituido_por = ? where id = ?", filho, pai);

		// 2) uma conta REAL na mesma mesa do demo velho, com ficha e rolagem lá, e fora dela: ficha solta, campanha própria
		CampanhasDeTeste.participar(jdbc, mesaVelha, A, Papel.JOGADOR);
		Ficha fichaDeA = FichasDeTeste.ordem("Ana na mesa demo");
		fichaDeA.setCampanhaId(mesaVelha);
		long fichaDeAId = fichas.save(fichaDeA).getId();
		rolagem(mesaVelha, fichaDeAId, A);
		long fichaSolta = fichas.save(FichasDeTeste.dnd("Ana sem mesa")).getId();
		CampanhasDeTeste.usuario(jdbc, RM, "Mestre real");
		long mesaReal = CampanhasDeTeste.campanha(jdbc, "Mesa real");
		CampanhasDeTeste.participar(jdbc, mesaReal, RM, Papel.MESTRE);
		Ficha fichaDeRm = FichasDeTeste.ordem("Do mestre real");
		fichaDeRm.setDonoId(RM);
		fichaDeRm.setCampanhaId(mesaReal);
		long fichaDeRmId = fichas.save(fichaDeRm).getId();
		long rolagemReal = rolagem(mesaReal, fichaDeRmId, RM);

		// 3) refresh da conta real: um vencido (some), um válido e um já usado mas ainda válido (ficam, p/ detectar reuso)
		long vencido = refresh(A, "a1", INICIO.plus(Duration.ofDays(1)));
		long valido = refresh(A, "a2", INICIO.plus(Duration.ofDays(40)));
		long usadoAindaValido = refresh(A, "a3", INICIO.plus(Duration.ofDays(30)));
		jdbc.update("update refresh_token set revogado_em = now() where id = ?", usadoAindaValido);
		long deRm = refresh(RM, "r1", INICIO.plus(Duration.ofDays(40)));

		// 4) o demo recente nasce 2 h depois; a limpeza roda 25 h depois do início
		relogio.definir(INICIO.plus(Duration.ofHours(2)));
		RespostaDemo recente = demo.entrar(null);
		relogio.definir(INICIO.plus(Duration.ofHours(25)));

		ResultadoDaLimpeza resultado = limpeza.executar();

		assertThat(resultado.contasDemoApagadas()).isEqualTo(2);
		assertThat(resultado.refreshVencidosApagados()).isEqualTo(1);
		// demo velho: conta, mesa, fichas (e filhas, retrato), rolagens e refresh sumiram
		assertThat(contar("usuario where id in (?, ?)", velho.usuario().id(), velhoJogador.usuario().id())).isZero();
		assertThat(contar("campanha where id = ?", mesaVelha)).isZero();
		for (long id : fichasVelhas) {
			assertThat(contar("ficha where id = ?", id)).as("ficha %d", id).isZero();
			assertThat(contar("ficha_habilidade where ficha_id = ?", id)).isZero();
			assertThat(contar("retrato_ficha where ficha_id = ?", id)).isZero();
		}
		assertThat(contar("refresh_token where usuario_id in (?, ?)", velho.usuario().id(), velhoJogador.usuario().id())).isZero();
		assertThat(contar("rolagem where campanha_id = ?", mesaVelha)).isZero();
		// demo recente: tudo de pé
		assertThat(contar("usuario where id = ?", recente.usuario().id())).isEqualTo(1);
		assertThat(contar("campanha where id = ?", recente.campanhaId())).isEqualTo(1);
		assertThat(contar("ficha where dono_id = ?", recente.usuario().id())).isEqualTo(2);
		assertThat(contar("refresh_token where usuario_id = ?", recente.usuario().id())).isEqualTo(1);
		// conta real que estava na mesa do demo: continua, com a ficha dela (agora sem campanha) e os refresh válidos
		assertThat(contar("usuario where id = ?", A)).isEqualTo(1);
		assertThat(contar("ficha where id = ? and campanha_id is null", fichaDeAId)).isEqualTo(1);
		assertThat(contar("ficha where id = ?", fichaSolta)).isEqualTo(1);
		assertThat(contar("participacao where usuario_id = ?", A)).isZero();
		assertThat(contar("refresh_token where id in (?, ?)", valido, usadoAindaValido)).isEqualTo(2);
		assertThat(contar("refresh_token where id = ?", vencido)).isZero();
		// a mesa e as coisas das contas reais nem foram tocadas
		assertThat(contar("usuario where id = ?", RM)).isEqualTo(1);
		assertThat(contar("campanha where id = ?", mesaReal)).isEqualTo(1);
		assertThat(contar("participacao where campanha_id = ? and usuario_id = ?", mesaReal, RM)).isEqualTo(1);
		assertThat(contar("ficha where id = ? and campanha_id = ?", fichaDeRmId, mesaReal)).isEqualTo(1);
		assertThat(contar("rolagem where id = ?", rolagemReal)).isEqualTo(1);
		assertThat(contar("refresh_token where id = ?", deRm)).isEqualTo(1);
	}

	@Test
	@DisplayName("a conta demo com menos de 24 h fica; só passando das 24 h ela vai embora")
	void limiteDe24Horas() {
		RespostaDemo conta = demo.entrar(null);

		relogio.definir(INICIO.plus(Duration.ofHours(24)));
		assertThat(limpeza.executar().contasDemoApagadas()).isZero();
		assertThat(contar("usuario where id = ?", conta.usuario().id())).isEqualTo(1);

		relogio.definir(INICIO.plus(Duration.ofHours(24)).plusSeconds(1));
		assertThat(limpeza.executar().contasDemoApagadas()).isEqualTo(1);
		assertThat(contar("usuario where id = ?", conta.usuario().id())).isZero();
	}

	@Test
	@DisplayName("conta que não é demo nunca é apagada, por mais velha que seja")
	void contaRealVelhaFica() {
		CampanhasDeTeste.usuario(jdbc, RM, "Mestre real");
		jdbc.update("update usuario set criado_em = ? where id = ?", java.sql.Timestamp.from(INICIO.minus(Duration.ofDays(900))), RM);

		relogio.definir(INICIO.plus(Duration.ofDays(30)));
		limpeza.executar();

		assertThat(contar("usuario where id = ?", RM)).isEqualTo(1);
		assertThat(contar("usuario where id = ?", A)).isEqualTo(1);
	}

	@Test
	@DisplayName("roda sozinha quando a aplicação fica pronta (ApplicationReadyEvent)")
	void rodaAoFicarPronta() {
		RespostaDemo conta = demo.entrar(null);
		relogio.definir(INICIO.plus(Duration.ofHours(30)));

		contexto.publishEvent(new ApplicationReadyEvent(new SpringApplication(), new String[0], contexto, Duration.ZERO));

		assertThat(contar("usuario where id = ?", conta.usuario().id())).isZero();
	}

	@Test
	@DisplayName("e se repete a cada 6 h enquanto a API está acordada")
	void repeteACadaSeisHoras() throws Exception {
		Method agendado = null;
		for (Method metodo : LimpezaAgendada.class.getDeclaredMethods()) {
			if (metodo.isAnnotationPresent(Scheduled.class)) {
				agendado = metodo;
			}
		}
		assertThat(agendado).as("um método @Scheduled").isNotNull();
		Scheduled agenda = agendado.getAnnotation(Scheduled.class);
		assertThat(Duration.of(agenda.fixedDelay(), agenda.timeUnit().toChronoUnit())).isEqualTo(Duration.ofHours(6));
		// e o gancho do ApplicationReadyEvent existe
		boolean aoFicarPronta = false;
		for (Method metodo : LimpezaAgendada.class.getDeclaredMethods()) {
			EventListener ouvinte = metodo.getAnnotation(EventListener.class);
			if (ouvinte != null && List.of(ouvinte.value()).contains(ApplicationReadyEvent.class)) {
				aoFicarPronta = true;
			}
		}
		assertThat(aoFicarPronta).isTrue();
	}

	private String codigoDa(long campanhaId) {
		return jdbc.queryForObject("select codigo_convite from campanha where id = ?", String.class, campanhaId);
	}

	private int contar(String deOnde, Object... argumentos) {
		return jdbc.queryForObject("select count(*) from " + deOnde, Integer.class, argumentos);
	}

	private long rolagem(long campanhaId, long fichaId, long usuarioId) {
		return jdbc.queryForObject("insert into rolagem (campanha_id, ficha_id, usuario_id, personagem, tipo, rotulo, total, detalhe) "
				+ "values (?, ?, ?, 'x', 'LIVRE', '1d20', 10, cast('{}' as jsonb)) returning id", Long.class, campanhaId, fichaId, usuarioId);
	}

	private long refresh(long usuarioId, String hash, Instant expiraEm) {
		return jdbc.queryForObject("insert into refresh_token (usuario_id, familia, hash, criado_em, expira_em) "
				+ "values (?, gen_random_uuid(), ?, ?, ?) returning id", Long.class, usuarioId, hash + "0".repeat(62),
				java.sql.Timestamp.from(INICIO), java.sql.Timestamp.from(expiraEm));
	}
}
