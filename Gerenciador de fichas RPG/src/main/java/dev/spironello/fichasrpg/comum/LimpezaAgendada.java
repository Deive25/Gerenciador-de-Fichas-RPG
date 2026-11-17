package dev.spironello.fichasrpg.comum;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Limpeza do que cresce sozinho (2.6, spec §5.5; fecha um risco do ADR 0022): as contas demo com mais de 24 h, com tudo o
 * que é delas, e os refresh tokens vencidos. A API dorme no plano gratuito, então um agendamento diário poderia nunca
 * rodar: a limpeza roda toda vez que a aplicação fica pronta (a cada vez que acorda) e a cada 6 h enquanto está acordada.
 *
 * Só apaga o que é do demo: a conta real nunca é tocada, nem a que está na mesma campanha que um demo (a ficha dela só
 * perde a campanha, pela chave estrangeira da V11). Os refresh já usados mas ainda válidos ficam: a detecção de reuso
 * depende deles. Tudo numa transação, na ordem das chaves estrangeiras. O log traz só contagens.
 */
@Component
public class LimpezaAgendada {

	private static final Logger log = LoggerFactory.getLogger(LimpezaAgendada.class);

	/** A conta demo vive 24 h. */
	static final Duration IDADE_MAXIMA_DA_CONTA_DEMO = Duration.ofHours(24);

	/** As contas demo velhas, como subconsulta (o mesmo corte em todas as instruções da rodada). */
	private static final String DEMO_VELHOS = "(select id from usuario where demo and criado_em < ?)";

	/** As campanhas em que um demo velho é o mestre. */
	private static final String MESAS_DOS_DEMO_VELHOS =
			"(select campanha_id from participacao where papel = 'MESTRE' and usuario_id in " + DEMO_VELHOS + ")";

	private final JdbcTemplate jdbc;
	private final TransactionTemplate transacao;
	private final Clock relogio;

	public LimpezaAgendada(JdbcTemplate jdbc, TransactionTemplate transacao, Clock relogio) {
		this.jdbc = jdbc;
		this.transacao = transacao;
		this.relogio = relogio;
	}

	/** Cada vez que a API acorda. */
	@EventListener(ApplicationReadyEvent.class)
	public void aoFicarPronta() {
		rodar();
	}

	/** Enquanto está acordada (o primeiro disparo só vem 6 h depois: ao subir, quem roda é o ApplicationReadyEvent). */
	@Scheduled(fixedDelay = 6, initialDelay = 6, timeUnit = TimeUnit.HOURS)
	public void aCadaSeisHoras() {
		rodar();
	}

	/** Uma falha aqui não pode derrubar a subida (o ApplicationReadyEvent) nem parar o agendamento: fica no log e a próxima rodada tenta de novo. */
	private void rodar() {
		try {
			ResultadoDaLimpeza resultado = executar();
			log.info("Limpeza: contasDemoApagadas={} refreshVencidosApagados={}", resultado.contasDemoApagadas(),
					resultado.refreshVencidosApagados());
		} catch (RuntimeException e) {
			log.warn("A limpeza falhou; tenta de novo na próxima rodada", e);
		}
	}

	/** Uma rodada, numa transação. Público para o teste chamar com o relógio dele. */
	public ResultadoDaLimpeza executar() {
		Instant agora = relogio.instant();
		Timestamp corte = Timestamp.from(agora.minus(IDADE_MAXIMA_DA_CONTA_DEMO));
		return transacao.execute(status -> {
			// rolagens dos demo velhos (a de quem é de fora, numa mesa demo, sai junto com a mesa, em cascata)
			jdbc.update("delete from rolagem where usuario_id in " + DEMO_VELHOS, corte);
			// fichas deles: as tabelas filhas e o retrato saem em cascata
			jdbc.update("delete from ficha where dono_id in " + DEMO_VELHOS, corte);
			// as mesas deles: participações e rolagens saem em cascata; a ficha de um usuário real nelas só perde a campanha
			jdbc.update("delete from campanha where id in " + MESAS_DOS_DEMO_VELHOS, corte);
			// o que sobrou: participação de jogador demo em mesa de outro demo, e os refresh
			jdbc.update("delete from participacao where usuario_id in " + DEMO_VELHOS, corte);
			jdbc.update("delete from refresh_token where usuario_id in " + DEMO_VELHOS, corte);
			int contas = jdbc.update("delete from usuario where id in " + DEMO_VELHOS, corte);
			// refresh vencido, de qualquer conta (os usados e ainda válidos ficam, para a detecção de reuso)
			int vencidos = jdbc.update("delete from refresh_token where expira_em < ?", Timestamp.from(agora));
			return new ResultadoDaLimpeza(contas, vencidos);
		});
	}
}
