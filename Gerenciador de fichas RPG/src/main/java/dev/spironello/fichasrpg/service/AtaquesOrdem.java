package dev.spironello.fichasrpg.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.dto.AtaqueOrdemResponse;
import dev.spironello.fichasrpg.dto.AtaqueRequest;
import dev.spironello.fichasrpg.dto.ModoTeste;
import dev.spironello.fichasrpg.dto.RolagemAtaqueResponse;
import dev.spironello.fichasrpg.dto.RolagemResponse;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.exception.ItemDaFichaNaoEncontradoException;
import dev.spironello.fichasrpg.model.AtaqueOrdem;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.repository.AtaqueOrdemRepository;

/** Os ataques das fichas de Ordem Paranormal: CRUD e rolagem. Chame dentro da transação. */
@Component
public class AtaquesOrdem {

	private static final int MARGEM_PADRAO = 20;
	private static final int MULTIPLICADOR_PADRAO = 2;
	private static final String NAO_EXISTE = "não existe em Ordem Paranormal";

	private final AtaqueOrdemRepository ataques;

	public AtaquesOrdem(AtaqueOrdemRepository ataques) {
		this.ataques = ataques;
	}

	/** Os ataques da ficha pela ordem de criação. */
	public List<AtaqueOrdemResponse> listar(Long fichaId) {
		return ataques.findByFichaIdOrderById(fichaId).stream().map(AtaquesOrdem::paraResponse).toList();
	}

	public AtaqueOrdemResponse criar(FichaOrdem ficha, AtaqueRequest request) {
		String dano = conferir(request);
		AtaqueOrdem ataque = new AtaqueOrdem();
		ataque.setFicha(ficha);
		preencher(ataque, request, dano);
		return paraResponse(ataques.save(ataque));
	}

	public AtaqueOrdemResponse atualizar(FichaOrdem ficha, Long ataqueId, AtaqueRequest request) {
		AtaqueOrdem ataque = buscar(ficha, ataqueId);
		String dano = conferir(request);
		preencher(ataque, request, dano);
		return paraResponse(ataque);
	}

	public void excluir(FichaOrdem ficha, Long ataqueId) {
		ataques.delete(buscar(ficha, ataqueId));
	}

	/**
	 * Rola o ataque: o d20 da perícia (atributo da perícia, grau da ficha) mais o bônus do ataque e, depois, o dano.
	 * É crítico se o d20 mantido chegou à margem; aí os dados do dano valem o multiplicador do ataque. Só o modo
	 * NORMAL existe em Ordem.
	 */
	public RolagemAtaqueResponse rolar(FichaOrdem ficha, Long ataqueId, ModoTeste modo, Dado dado) {
		AtaqueOrdem ataque = buscar(ficha, ataqueId);
		if (modo != null && modo != ModoTeste.NORMAL) {
			throw new DadosInvalidosException(Map.of("modo", "só NORMAL existe em Ordem Paranormal"));
		}
		PericiaOrdem pericia = ataque.getPericia();
		TesteRolado d20 = RegrasOrdem.rolar(ficha.getAtributo(pericia.getAtributo()),
				ficha.getPericias().get(pericia), dado);
		int bonus = d20.bonus() + ataque.getBonus();
		boolean critico = RegrasOrdem.critico(d20.mantido(), ataque.getMargem());
		NotacaoDeDados.Expressao expressao = NotacaoDeDados.ler(ataque.getDano());
		Rolagem dano = NotacaoDeDados.rolar(expressao, critico ? ataque.getMultiplicador() : 1, dado);
		return new RolagemAtaqueResponse(ataque.getId(), ataque.getNome(),
				new RolagemAtaqueResponse.Teste(d20.dados(), d20.mantido(), bonus, d20.mantido() + bonus),
				new RolagemResponse(expressao.canonica(), dano.dados(), dano.bonus(), dano.total()), critico);
	}

	/** findByIdAndFichaId também cobre o ataque de outra ficha: para esta ficha, ele não existe. */
	private AtaqueOrdem buscar(FichaOrdem ficha, Long ataqueId) {
		return ataques.findByIdAndFichaId(ataqueId, ficha.getId())
				.orElseThrow(() -> new ItemDaFichaNaoEncontradoException("ataque", ataqueId));
	}

	/** Confere os campos do sistema e o dano; devolve todos os erros de uma vez, ou o dano em forma canônica. */
	private String conferir(AtaqueRequest request) {
		Map<String, String> erros = new LinkedHashMap<>();
		if (request.pericia() == null) {
			erros.put("pericia", "é obrigatório");
		} else if (request.pericia() != PericiaOrdem.LUTA && request.pericia() != PericiaOrdem.PONTARIA) {
			erros.put("pericia", "use LUTA ou PONTARIA");
		}
		if (request.atributo() != null) {
			erros.put("atributo", NAO_EXISTE);
		}
		if (request.proficiente() != null) {
			erros.put("proficiente", NAO_EXISTE);
		}
		NotacaoDeDados.Expressao dano = NotacaoDeDados.lerAnotando(request.dano(), "dano", erros);
		if (!erros.isEmpty()) {
			throw new DadosInvalidosException(erros);
		}
		return dano.canonica();
	}

	private void preencher(AtaqueOrdem ataque, AtaqueRequest request, String dano) {
		ataque.setNome(request.nome());
		ataque.setPericia(request.pericia());
		ataque.setBonus(request.bonus() == null ? 0 : request.bonus());
		ataque.setDano(dano);
		ataque.setTipoDano(Textos.vazioParaNulo(request.tipoDano()));
		ataque.setAlcance(Textos.vazioParaNulo(request.alcance()));
		ataque.setMargem(request.margem() == null ? MARGEM_PADRAO : request.margem());
		ataque.setMultiplicador(request.multiplicador() == null ? MULTIPLICADOR_PADRAO : request.multiplicador());
	}

	private static AtaqueOrdemResponse paraResponse(AtaqueOrdem a) {
		return new AtaqueOrdemResponse(a.getId(), a.getNome(), a.getPericia(), a.getBonus(), a.getDano(),
				a.getTipoDano(), a.getAlcance(), a.getMargem(), a.getMultiplicador());
	}
}
