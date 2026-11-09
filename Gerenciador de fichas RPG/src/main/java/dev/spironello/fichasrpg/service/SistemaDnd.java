package dev.spironello.fichasrpg.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.dto.AlvoTeste;
import dev.spironello.fichasrpg.dto.FichaDndRequest;
import dev.spironello.fichasrpg.dto.FichaRequest;
import dev.spironello.fichasrpg.dto.FichaResponse;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.dto.ModoTeste;
import dev.spironello.fichasrpg.dto.TesteRequest;
import dev.spironello.fichasrpg.dto.TesteResponse;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.model.AtributoDnd;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.PericiaDnd;
import dev.spironello.fichasrpg.model.RecursoDnd;
import dev.spironello.fichasrpg.model.Sistema;

/**
 * Regras de D&D (5e de 2014 e 2024): CRUD, edição rápida de PV e PV temporário e teste de d20
 * (perícia, atributo ou salvaguarda, com vantagem e desvantagem) pela {@link RegrasDnd}.
 */
@Component
public class SistemaDnd implements SistemaDeFicha {

	private static final String NAO_EXISTE = "não existe em D&D";

	private final FichaDndMapper mapper;

	public SistemaDnd(FichaDndMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public Sistema sistema() {
		return Sistema.DND;
	}

	@Override
	public Ficha criar(FichaRequest pedido) {
		FichaDndRequest dnd = (FichaDndRequest) pedido;
		ValidacaoFichaDnd.validar(dnd);
		return mapper.criar(dnd);
	}

	@Override
	public void copiar(FichaRequest pedido, Ficha ficha) {
		FichaDndRequest dnd = (FichaDndRequest) pedido;
		ValidacaoFichaDnd.validar(dnd);
		mapper.copiar(dnd, (FichaDnd) ficha);
	}

	@Override
	public FichaResponse resposta(Ficha ficha) {
		return mapper.resposta((FichaDnd) ficha);
	}

	@Override
	public FichaResumoResponse resumo(Ficha ficha) {
		return mapper.resumo((FichaDnd) ficha);
	}

	@Override
	public void atualizarRecursos(Ficha ficha, Map<String, Integer> atuais) {
		FichaDnd dnd = (FichaDnd) ficha;
		Map<String, String> erros = new LinkedHashMap<>();
		Map<RecursoDnd, Integer> validos = new LinkedHashMap<>();
		atuais.forEach((codigo, valor) -> {
			RecursoDnd recurso = converter(RecursoDnd.class, codigo);
			if (recurso == null) {
				erros.put(codigo, NAO_EXISTE);
			} else if (valor == null || valor < 0) {
				erros.put(codigo, "deve ser um número maior ou igual a 0");
			} else {
				validos.put(recurso, valor);
			}
		});
		if (!erros.isEmpty()) {
			throw new DadosInvalidosException(erros);
		}
		validos.forEach((recurso, valor) -> {
			switch (recurso) {
				case PV -> dnd.setPvAtual(valor);
				case PV_TEMP -> dnd.setPvTemporario(valor);
			}
		});
	}

	@Override
	public TesteResponse testar(Ficha ficha, TesteRequest pedido, Dado dado) {
		FichaDnd dnd = (FichaDnd) ficha;
		ModoTeste modo = pedido.modo();
		Map<String, String> erros = new LinkedHashMap<>();
		TesteResponse resposta = null;
		if (pedido.alvo() == AlvoTeste.PERICIA) {
			PericiaDnd pericia = converter(PericiaDnd.class, pedido.codigo());
			if (pericia == null) {
				erros.put("codigo", NAO_EXISTE);
			} else {
				int bonus = RegrasDnd.bonusPericia(dnd.getAtributo(pericia.getAtributo()), dnd.getNivel(),
						dnd.getPericias().get(pericia));
				resposta = resposta(pedido, pericia.getNome(), RegrasDnd.rolar(bonus, modo, dado));
			}
		} else {
			AtributoDnd atributo = converter(AtributoDnd.class, pedido.codigo());
			if (atributo == null) {
				erros.put("codigo", NAO_EXISTE);
			} else if (pedido.alvo() == AlvoTeste.ATRIBUTO) {
				int bonus = RegrasDnd.modificador(dnd.getAtributo(atributo));
				resposta = resposta(pedido, atributo.getNome(), RegrasDnd.rolar(bonus, modo, dado));
			} else {
				int bonus = RegrasDnd.bonusSalvaguarda(dnd.getAtributo(atributo), dnd.getNivel(),
						dnd.isProficienteEmSalvaguarda(atributo));
				resposta = resposta(pedido, "Salvaguarda de " + atributo.getNome(),
						RegrasDnd.rolar(bonus, modo, dado));
			}
		}
		if (!erros.isEmpty()) {
			throw new DadosInvalidosException(erros);
		}
		return resposta;
	}

	private static TesteResponse resposta(TesteRequest pedido, String nome, TesteRolado rolado) {
		return new TesteResponse(pedido.alvo(), pedido.codigo(), nome, rolado.dados(), rolado.mantido(),
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
