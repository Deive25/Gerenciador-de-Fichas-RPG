package dev.spironello.fichasrpg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.dto.TesteRequest;
import dev.spironello.fichasrpg.dto.TesteResponse;
import dev.spironello.fichasrpg.exception.FichaNaoEncontradaException;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.repository.FichaRepository;

/**
 * Teste de perícia ou atributo: busca a ficha, entrega o pedido ao {@link SistemaDeFicha} dela (que valida e
 * rola os dados) e devolve o resultado. Não grava nada. O log traz só o id da ficha, o código e o total: a
 * lista de dados e os dados da ficha não vão para o log.
 */
@Service
public class TesteService {

	private static final Logger log = LoggerFactory.getLogger(TesteService.class);

	private final FichaRepository repository;
	private final SistemasDeFicha sistemas;
	private final Dado dado;

	public TesteService(FichaRepository repository, SistemasDeFicha sistemas, Dado dado) {
		this.repository = repository;
		this.sistemas = sistemas;
		this.dado = dado;
	}

	@Transactional(readOnly = true)
	public TesteResponse testar(Long fichaId, TesteRequest request) {
		Ficha ficha = repository.findById(fichaId).orElseThrow(() -> new FichaNaoEncontradaException(fichaId));
		TesteResponse resposta = sistemas.de(ficha.getSistema()).testar(ficha, request, dado);
		log.info("Teste rolado ficha={} codigo={} total={}", fichaId, request.codigo(), resposta.total());
		return resposta;
	}
}
