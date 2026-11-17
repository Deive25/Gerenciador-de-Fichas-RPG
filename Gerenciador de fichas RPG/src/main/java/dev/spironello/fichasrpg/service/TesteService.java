package dev.spironello.fichasrpg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.dto.TesteRequest;
import dev.spironello.fichasrpg.dto.TesteResponse;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.rolagem.RegistroDeRolagens;
import dev.spironello.fichasrpg.rolagem.TipoRolagem;

/**
 * Teste de perícia ou atributo: busca a ficha, entrega o pedido ao {@link SistemaDeFicha} dela (que valida e
 * rola os dados), devolve o resultado e o entrega ao {@link RegistroDeRolagens} (que só grava se a ficha está numa
 * campanha). O log traz só o id da ficha, o código e o total: a lista de dados e os dados da ficha não vão para o log.
 */
@Service
public class TesteService {

	private static final Logger log = LoggerFactory.getLogger(TesteService.class);

	private final AcessoAFicha fichas;
	private final SistemasDeFicha sistemas;
	private final Dado dado;
	private final RegistroDeRolagens registro;

	public TesteService(AcessoAFicha fichas, SistemasDeFicha sistemas, Dado dado, RegistroDeRolagens registro) {
		this.fichas = fichas;
		this.sistemas = sistemas;
		this.dado = dado;
		this.registro = registro;
	}

	// Não é mais readOnly: o registro grava a rolagem da mesa (e publica o evento depois do commit).
	@Transactional
	public TesteResponse testar(Long fichaId, TesteRequest request) {
		Ficha ficha = fichas.buscar(fichaId);
		TesteResponse resposta = sistemas.de(ficha.getSistema()).testar(ficha, request, dado);
		registro.registrar(ficha, TipoRolagem.TESTE, resposta.nome(), resposta.total(), resposta);
		log.info("Teste rolado ficha={} codigo={} total={}", fichaId, request.codigo(), resposta.total());
		return resposta;
	}
}
