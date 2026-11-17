package dev.spironello.fichasrpg.rolagem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import dev.spironello.fichasrpg.AutenticacaoDeTesteConfig;
import dev.spironello.fichasrpg.CampanhasDeTeste;
import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.campanha.Papel;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.repository.FichaRepository;

/** A rolagem só vai para o tópico depois do commit: com a transação desfeita, nada é enviado. */
@DisplayName("Publicação da rolagem depois do commit")
class PublicacaoAposCommitTest extends IntegrationTestBase {

	private static final long A = AutenticacaoDeTesteConfig.USUARIO_DE_TESTE_ID;

	@MockitoSpyBean
	private SimpMessagingTemplate mensagens;

	@Autowired
	private RegistroDeRolagens registro;
	@Autowired
	private TransactionTemplate transacao;
	@Autowired
	private FichaRepository fichas;
	@Autowired
	private JdbcTemplate jdbc;

	private long campanha;
	private Ficha ficha;

	@BeforeEach
	void prepararMesa() {
		Mockito.clearInvocations(mensagens);
		campanha = CampanhasDeTeste.campanha(jdbc, "Mesa");
		CampanhasDeTeste.usuario(jdbc, 7_300_001L, "Mestre");
		CampanhasDeTeste.participar(jdbc, campanha, 7_300_001L, Papel.MESTRE);
		CampanhasDeTeste.participar(jdbc, campanha, A, Papel.JOGADOR);
		FichaOrdem nova = FichasDeTeste.ordem("Kai");
		nova.setCampanhaId(campanha);
		ficha = fichas.saveAndFlush(nova);
	}

	@AfterEach
	void limpar() {
		CampanhasDeTeste.limpar(jdbc, fichas, A, 7_300_001L);
	}

	@Test
	@DisplayName("transação desfeita: grava nada e não chama o convertAndSend")
	void desfeitaNaoPublica() {
		transacao.executeWithoutResult(status -> {
			registro.registrar(ficha, TipoRolagem.LIVRE, "1d20", 14, Map.of("total", 14));
			status.setRollbackOnly();
		});

		verify(mensagens, never()).convertAndSend(any(String.class), any(RolagemDaMesaResponse.class));
		assertThat(jdbc.queryForObject("select count(*) from rolagem", Integer.class)).isZero();
	}

	@Test
	@DisplayName("transação confirmada: chama o convertAndSend uma vez, no tópico da campanha")
	void confirmadaPublicaUmaVez() {
		transacao.executeWithoutResult(status -> registro.registrar(ficha, TipoRolagem.LIVRE, "1d20", 14, Map.of("total", 14)));

		verify(mensagens, times(1)).convertAndSend(eq("/topic/campanhas/" + campanha + "/rolagens"), any(RolagemDaMesaResponse.class));
		assertThat(jdbc.queryForObject("select count(*) from rolagem", Integer.class)).isEqualTo(1);
	}
}
