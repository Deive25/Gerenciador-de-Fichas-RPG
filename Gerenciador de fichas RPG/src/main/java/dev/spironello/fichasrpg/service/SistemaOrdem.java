package dev.spironello.fichasrpg.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.dto.AlvoTeste;
import dev.spironello.fichasrpg.dto.FichaOrdemRequest;
import dev.spironello.fichasrpg.dto.FichaRequest;
import dev.spironello.fichasrpg.dto.FichaResponse;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.dto.ModoTeste;
import dev.spironello.fichasrpg.dto.TesteRequest;
import dev.spironello.fichasrpg.dto.TesteResponse;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.model.AtributoOrdem;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.model.RecursoOrdem;
import dev.spironello.fichasrpg.model.Sistema;

/**
 * Regras de Ordem Paranormal: validação e conversão (mapper), edição rápida de PV/SAN/PE e teste de d20
 * (perícia ou atributo) pela {@link RegrasOrdem}. O código veio do 2.1d, só mudou de lugar.
 */
@Component
public class SistemaOrdem implements SistemaDeFicha {

	private static final String NAO_EXISTE = "não existe em Ordem Paranormal";

	private final FichaOrdemMapper mapper;
	private final PartesComunsDaFicha partesComuns;
	private final AtaquesOrdem ataques;
	private final ItensOrdem itens;
	private final RituaisOrdem rituais;

	public SistemaOrdem(FichaOrdemMapper mapper, PartesComunsDaFicha partesComuns, AtaquesOrdem ataques, ItensOrdem itens,
			RituaisOrdem rituais) {
		this.mapper = mapper;
		this.partesComuns = partesComuns;
		this.ataques = ataques;
		this.itens = itens;
		this.rituais = rituais;
	}

	@Override
	public Sistema sistema() {
		return Sistema.ORDEM_PARANORMAL;
	}

	@Override
	public Ficha criar(FichaRequest pedido) {
		FichaOrdemRequest ordem = (FichaOrdemRequest) pedido;
		ValidacaoFichaOrdem.validar(ordem);
		return mapper.criar(ordem);
	}

	@Override
	public void copiar(FichaRequest pedido, Ficha ficha) {
		FichaOrdemRequest ordem = (FichaOrdemRequest) pedido;
		ValidacaoFichaOrdem.validar(ordem);
		mapper.copiar(ordem, (FichaOrdem) ficha);
	}

	@Override
	public FichaResponse resposta(Ficha ficha) {
		return mapper.resposta((FichaOrdem) ficha, partesComuns.de(ficha),
				new ListasOrdem(ataques.listar(ficha.getId()), itens.listar(ficha.getId()), rituais.listar(ficha.getId())));
	}

	@Override
	public FichaResumoResponse resumo(Ficha ficha) {
		return mapper.resumo((FichaOrdem) ficha);
	}

	@Override
	public void atualizarRecursos(Ficha ficha, Map<String, Integer> atuais) {
		FichaOrdem ordem = (FichaOrdem) ficha;
		Map<String, String> erros = new LinkedHashMap<>();
		Map<RecursoOrdem, Integer> validos = new LinkedHashMap<>();
		atuais.forEach((codigo, valor) -> {
			RecursoOrdem recurso = converter(RecursoOrdem.class, codigo);
			if (recurso == null) {
				erros.put(codigo, "não existe em Ordem Paranormal");
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
				case PV -> ordem.setPvAtual(valor);
				case SAN -> ordem.setSanAtual(valor);
				case PE -> ordem.setPeAtual(valor);
			}
		});
	}

	@Override
	public TesteResponse testar(Ficha ficha, TesteRequest pedido, Dado dado) {
		FichaOrdem ordem = (FichaOrdem) ficha;
		Map<String, String> erros = new LinkedHashMap<>();
		if (pedido.alvo() == AlvoTeste.SALVAGUARDA) {
			erros.put("alvo", "SALVAGUARDA não existe em Ordem Paranormal");
		}
		if (pedido.modo() != null && pedido.modo() != ModoTeste.NORMAL) {
			erros.put("modo", "só NORMAL existe em Ordem Paranormal");
		}
		TesteResponse resposta = null;
		if (pedido.alvo() == AlvoTeste.PERICIA) {
			PericiaOrdem pericia = converter(PericiaOrdem.class, pedido.codigo());
			if (pericia == null) {
				erros.put("codigo", NAO_EXISTE);
			} else if (erros.isEmpty()) {
				TesteRolado rolado = RegrasOrdem.rolar(ordem.getAtributo(pericia.getAtributo()),
						ordem.getPericias().get(pericia), dado);
				resposta = resposta(pedido, pericia.getNome(), rolado);
			}
		} else if (pedido.alvo() == AlvoTeste.ATRIBUTO) {
			AtributoOrdem atributo = converter(AtributoOrdem.class, pedido.codigo());
			if (atributo == null) {
				erros.put("codigo", NAO_EXISTE);
			} else if (erros.isEmpty()) {
				TesteRolado rolado = RegrasOrdem.rolar(ordem.getAtributo(atributo), null, dado);
				resposta = resposta(pedido, atributo.getNome(), rolado);
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
