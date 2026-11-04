package dev.spironello.fichasrpg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.dto.TestePericiaResponse;
import dev.spironello.fichasrpg.exception.SheetPlayerNotFoundException;
import dev.spironello.fichasrpg.model.Pericia;
import dev.spironello.fichasrpg.model.ResultadoTeste;
import dev.spironello.fichasrpg.model.SheetPlayer;
import dev.spironello.fichasrpg.repository.SheetPlayerRepository;

/**
 * Teste de perícia: busca a ficha, rola o dado e aplica a regra. Não grava nada (ADR 0011).
 * O log traz só ids e o resultado, sem nomes do jogador ou do personagem.
 */
@Service
public class TestePericiaService {

	private static final Logger log = LoggerFactory.getLogger(TestePericiaService.class);

	private final SheetPlayerRepository repository;
	private final Dado dado;

	public TestePericiaService(SheetPlayerRepository repository, Dado dado) {
		this.repository = repository;
		this.dado = dado;
	}

	@Transactional(readOnly = true)
	public TestePericiaResponse testar(Long fichaId, Pericia pericia) {
		SheetPlayer ficha = repository.findById(fichaId)
				.orElseThrow(() -> new SheetPlayerNotFoundException(fichaId));
		// Perícia ausente do mapa = não treinada = valor 0, que sempre falha (ADR 0011).
		int valor = ficha.getPericias().getOrDefault(pericia, 0);
		int rolagem = dado.rolarD100();
		ResultadoTeste resultado = RegraTestePericia.avaliar(valor, rolagem);
		log.info("Teste de perícia ficha={} pericia={} resultado={}", fichaId, pericia, resultado);
		return new TestePericiaResponse(pericia, pericia.getNome(), valor, rolagem, resultado);
	}
}
