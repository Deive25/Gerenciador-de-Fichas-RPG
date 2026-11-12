package dev.spironello.fichasrpg.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.dto.AtaqueDndResponse;
import dev.spironello.fichasrpg.dto.AtaqueRequest;
import dev.spironello.fichasrpg.dto.ModoTeste;
import dev.spironello.fichasrpg.dto.RolagemAtaqueResponse;
import dev.spironello.fichasrpg.dto.RolagemResponse;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.exception.ItemDaFichaNaoEncontradoException;
import dev.spironello.fichasrpg.model.AtaqueDnd;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.repository.AtaqueDndRepository;

/** Os ataques das fichas de D&D: CRUD e rolagem. Chame dentro da transação. */
@Component
public class AtaquesDnd {

	private static final String NAO_EXISTE = "não existe em D&D";
	/** No crítico de D&D, os dados do dano dobram. */
	private static final int MULTIPLICADOR_DO_CRITICO = 2;

	private final AtaqueDndRepository ataques;

	public AtaquesDnd(AtaqueDndRepository ataques) {
		this.ataques = ataques;
	}

	/** Os ataques da ficha pela ordem de criação. */
	public List<AtaqueDndResponse> listar(Long fichaId) {
		return ataques.findByFichaIdOrderById(fichaId).stream().map(AtaquesDnd::paraResponse).toList();
	}

	public AtaqueDndResponse criar(FichaDnd ficha, AtaqueRequest request) {
		String dano = conferir(request);
		AtaqueDnd ataque = new AtaqueDnd();
		ataque.setFicha(ficha);
		preencher(ataque, request, dano);
		return paraResponse(ataques.save(ataque));
	}

	public AtaqueDndResponse atualizar(FichaDnd ficha, Long ataqueId, AtaqueRequest request) {
		AtaqueDnd ataque = buscar(ficha, ataqueId);
		String dano = conferir(request);
		preencher(ataque, request, dano);
		return paraResponse(ataque);
	}

	public void excluir(FichaDnd ficha, Long ataqueId) {
		ataques.delete(buscar(ficha, ataqueId));
	}

	/**
	 * Rola o ataque: o d20 (com vantagem ou desvantagem) mais o modificador do atributo, a proficiência (se o ataque a
	 * soma, pelo nível da ficha) e o bônus do ataque. É crítico só no 20 natural mantido; aí os dados do dano dobram.
	 */
	public RolagemAtaqueResponse rolar(FichaDnd ficha, Long ataqueId, ModoTeste modo, Dado dado) {
		AtaqueDnd ataque = buscar(ficha, ataqueId);
		int bonus = RegrasDnd.modificador(ficha.getAtributo(ataque.getAtributo()))
				+ (ataque.isProficiente() ? RegrasDnd.proficiencia(ficha.getNivel()) : 0)
				+ ataque.getBonus();
		TesteRolado d20 = RegrasDnd.rolar(bonus, modo, dado);
		boolean critico = RegrasDnd.critico(d20.mantido());
		NotacaoDeDados.Expressao expressao = NotacaoDeDados.ler(ataque.getDano());
		Rolagem dano = NotacaoDeDados.rolar(expressao, critico ? MULTIPLICADOR_DO_CRITICO : 1, dado);
		return new RolagemAtaqueResponse(ataque.getId(), ataque.getNome(),
				new RolagemAtaqueResponse.Teste(d20.dados(), d20.mantido(), d20.bonus(), d20.total()),
				new RolagemResponse(expressao.canonica(), dano.dados(), dano.bonus(), dano.total()), critico);
	}

	/** findByIdAndFichaId também cobre o ataque de outra ficha: para esta ficha, ele não existe. */
	private AtaqueDnd buscar(FichaDnd ficha, Long ataqueId) {
		return ataques.findByIdAndFichaId(ataqueId, ficha.getId())
				.orElseThrow(() -> new ItemDaFichaNaoEncontradoException("ataque", ataqueId));
	}

	/** Confere os campos do sistema e o dano; devolve todos os erros de uma vez, ou o dano em forma canônica. */
	private String conferir(AtaqueRequest request) {
		Map<String, String> erros = new LinkedHashMap<>();
		if (request.atributo() == null) {
			erros.put("atributo", "é obrigatório");
		}
		if (request.pericia() != null) {
			erros.put("pericia", NAO_EXISTE);
		}
		if (request.margem() != null) {
			erros.put("margem", NAO_EXISTE);
		}
		if (request.multiplicador() != null) {
			erros.put("multiplicador", NAO_EXISTE);
		}
		NotacaoDeDados.Expressao dano = NotacaoDeDados.lerAnotando(request.dano(), "dano", erros);
		if (!erros.isEmpty()) {
			throw new DadosInvalidosException(erros);
		}
		return dano.canonica();
	}

	private void preencher(AtaqueDnd ataque, AtaqueRequest request, String dano) {
		ataque.setNome(request.nome());
		ataque.setAtributo(request.atributo());
		ataque.setProficiente(Boolean.TRUE.equals(request.proficiente()));
		ataque.setBonus(request.bonus() == null ? 0 : request.bonus());
		ataque.setDano(dano);
		ataque.setTipoDano(Textos.vazioParaNulo(request.tipoDano()));
		ataque.setAlcance(Textos.vazioParaNulo(request.alcance()));
	}

	private static AtaqueDndResponse paraResponse(AtaqueDnd a) {
		return new AtaqueDndResponse(a.getId(), a.getNome(), a.getAtributo(), a.isProficiente(), a.getBonus(),
				a.getDano(), a.getTipoDano(), a.getAlcance());
	}
}
