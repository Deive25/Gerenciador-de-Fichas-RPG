package dev.spironello.fichasrpg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.dto.RolagemRequest;
import dev.spironello.fichasrpg.dto.RolagemResponse;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.rolagem.RegistroDeRolagens;
import dev.spironello.fichasrpg.rolagem.TipoRolagem;

/**
 * Rolagem livre de uma notação de dados (aba Dados). Vale nos dois sistemas: confere o acesso à ficha (a aba está na
 * ficha e a rolagem pertence a um personagem), rola e entrega o resultado ao {@link RegistroDeRolagens}, que só grava
 * se a ficha está numa campanha. O log traz só o id da ficha e o total, nunca o texto digitado.
 */
@Service
public class RolagemService {

	private static final Logger log = LoggerFactory.getLogger(RolagemService.class);

	private final AcessoAFicha fichas;
	private final Dado dado;
	private final RegistroDeRolagens registro;

	public RolagemService(AcessoAFicha fichas, Dado dado, RegistroDeRolagens registro) {
		this.fichas = fichas;
		this.dado = dado;
		this.registro = registro;
	}

	// Não é mais readOnly: o registro grava a rolagem da mesa (e publica o evento depois do commit).
	@Transactional
	public RolagemResponse rolar(Long fichaId, RolagemRequest request) {
		Ficha ficha = fichas.buscar(fichaId);
		NotacaoDeDados.Expressao expressao = NotacaoDeDados.lerNoCampo(request.expressao(), "expressao");
		Rolagem rolagem = NotacaoDeDados.rolar(expressao, 1, dado);
		log.info("Rolagem livre fichaId={} total={}", fichaId, rolagem.total());
		RolagemResponse resposta = new RolagemResponse(expressao.canonica(), rolagem.dados(), rolagem.bonus(), rolagem.total());
		registro.registrar(ficha, TipoRolagem.LIVRE, resposta.expressao(), resposta.total(), resposta);
		return resposta;
	}
}
