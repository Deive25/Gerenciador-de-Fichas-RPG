package dev.spironello.fichasrpg.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.dto.AlvoTeste;
import dev.spironello.fichasrpg.dto.ModoTeste;
import dev.spironello.fichasrpg.dto.TesteRequest;
import dev.spironello.fichasrpg.dto.TesteResponse;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.exception.FichaNaoEncontradaException;
import dev.spironello.fichasrpg.model.AtributoOrdem;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.repository.FichaRepository;

/**
 * Teste de Ordem: busca a ficha, valida o pedido, rola os d20 pela {@link RegrasOrdem} e devolve o resultado.
 * Não grava nada. O log traz só o id da ficha, o código e o total: a lista de dados e os dados da ficha
 * não vão para o log.
 */
@Service
public class TesteService {

	private static final Logger log = LoggerFactory.getLogger(TesteService.class);
	private static final String NAO_EXISTE = "não existe em Ordem Paranormal";

	private final FichaRepository repository;
	private final Dado dado;

	public TesteService(FichaRepository repository, Dado dado) {
		this.repository = repository;
		this.dado = dado;
	}

	@Transactional(readOnly = true)
	public TesteResponse testar(Long fichaId, TesteRequest request) {
		Ficha ficha = repository.findById(fichaId).orElseThrow(() -> new FichaNaoEncontradaException(fichaId));
		if (!(ficha instanceof FichaOrdem ordem)) {
			throw new IllegalStateException("Sistema sem rolagem: " + ficha.getSistema());
		}
		Map<String, String> erros = new LinkedHashMap<>();
		if (request.alvo() == AlvoTeste.SALVAGUARDA) {
			erros.put("alvo", "SALVAGUARDA não existe em Ordem Paranormal");
		}
		if (request.modo() != null && request.modo() != ModoTeste.NORMAL) {
			erros.put("modo", "só NORMAL existe em Ordem Paranormal");
		}
		TesteResponse resposta = null;
		if (request.alvo() == AlvoTeste.PERICIA) {
			PericiaOrdem pericia = converter(PericiaOrdem.class, request.codigo());
			if (pericia == null) {
				erros.put("codigo", NAO_EXISTE);
			} else if (erros.isEmpty()) {
				TesteRolado rolado = RegrasOrdem.rolar(ordem.getAtributo(pericia.getAtributo()),
						ordem.getPericias().get(pericia), dado);
				resposta = resposta(request, pericia.getNome(), rolado);
			}
		} else if (request.alvo() == AlvoTeste.ATRIBUTO) {
			AtributoOrdem atributo = converter(AtributoOrdem.class, request.codigo());
			if (atributo == null) {
				erros.put("codigo", NAO_EXISTE);
			} else if (erros.isEmpty()) {
				TesteRolado rolado = RegrasOrdem.rolar(ordem.getAtributo(atributo), null, dado);
				resposta = resposta(request, atributo.getNome(), rolado);
			}
		}
		if (!erros.isEmpty()) {
			throw new DadosInvalidosException(erros);
		}
		log.info("Teste rolado ficha={} codigo={} total={}", fichaId, request.codigo(), resposta.total());
		return resposta;
	}

	private static TesteResponse resposta(TesteRequest request, String nome, TesteRolado rolado) {
		return new TesteResponse(request.alvo(), request.codigo(), nome, rolado.dados(), rolado.mantido(),
				rolado.bonus(), rolado.total());
	}

	/** O código tem de ser exatamente o nome da constante; fora disso devolve null. */
	private static <E extends Enum<E>> E converter(Class<E> tipo, String codigo) {
		try {
			return Enum.valueOf(tipo, codigo);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
