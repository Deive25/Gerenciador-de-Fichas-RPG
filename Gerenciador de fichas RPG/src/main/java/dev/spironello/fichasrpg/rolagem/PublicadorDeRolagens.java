package dev.spironello.fichasrpg.rolagem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Leva a rolagem gravada ao tópico da campanha, mas só DEPOIS do commit: se a transação for desfeita, a mesa não vê
 * uma rolagem que não existe. Uma falha no envio vai só para o log (com os ids): a rolagem já está gravada e aparece
 * no histórico.
 */
@Component
public class PublicadorDeRolagens {

	private static final Logger log = LoggerFactory.getLogger(PublicadorDeRolagens.class);

	private final SimpMessagingTemplate mensagens;

	public PublicadorDeRolagens(SimpMessagingTemplate mensagens) {
		this.mensagens = mensagens;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void publicar(RolagemRegistrada evento) {
		RolagemDaMesaResponse rolagem = evento.rolagem();
		try {
			mensagens.convertAndSend("/topic/campanhas/" + rolagem.campanhaId() + "/rolagens", rolagem);
		} catch (RuntimeException e) {
			log.warn("Falha ao publicar a rolagem id={} campanhaId={} ({})", rolagem.id(), rolagem.campanhaId(),
					e.getClass().getSimpleName());
		}
	}
}
