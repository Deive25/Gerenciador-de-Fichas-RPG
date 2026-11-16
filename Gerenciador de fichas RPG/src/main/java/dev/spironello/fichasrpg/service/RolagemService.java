package dev.spironello.fichasrpg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.dto.RolagemRequest;
import dev.spironello.fichasrpg.dto.RolagemResponse;

/**
 * Rolagem livre de uma notação de dados (aba Dados). Vale nos dois sistemas e não grava nada: só confere que a ficha
 * existe (a aba está na ficha, e no 2.4 cada rolagem vai pertencer a um personagem). O log traz só o id da ficha e o
 * total, nunca o texto digitado.
 */
@Service
public class RolagemService {

	private static final Logger log = LoggerFactory.getLogger(RolagemService.class);

	private final AcessoAFicha fichas;
	private final Dado dado;

	public RolagemService(AcessoAFicha fichas, Dado dado) {
		this.fichas = fichas;
		this.dado = dado;
	}

	@Transactional(readOnly = true)
	public RolagemResponse rolar(Long fichaId, RolagemRequest request) {
		fichas.exigir(fichaId);
		NotacaoDeDados.Expressao expressao = NotacaoDeDados.lerNoCampo(request.expressao(), "expressao");
		Rolagem rolagem = NotacaoDeDados.rolar(expressao, 1, dado);
		log.info("Rolagem livre fichaId={} total={}", fichaId, rolagem.total());
		return new RolagemResponse(expressao.canonica(), rolagem.dados(), rolagem.bonus(), rolagem.total());
	}
}
