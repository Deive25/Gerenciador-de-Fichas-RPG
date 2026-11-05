package dev.spironello.fichasrpg.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.dto.FichaDndRequest;
import dev.spironello.fichasrpg.dto.FichaRequest;
import dev.spironello.fichasrpg.dto.FichaResponse;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.dto.TesteRequest;
import dev.spironello.fichasrpg.dto.TesteResponse;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.Sistema;

/**
 * Regras de D&D (5e de 2014 e 2024). Por enquanto só o CRUD: a edição rápida de PV e o teste de d20
 * (com vantagem e desvantagem) entram na próxima tarefa.
 */
@Component
public class SistemaDnd implements SistemaDeFicha {

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
		throw new UnsupportedOperationException("Edição rápida de D&D: Tarefa 5");
	}

	@Override
	public TesteResponse testar(Ficha ficha, TesteRequest pedido, Dado dado) {
		throw new UnsupportedOperationException("Teste de D&D: Tarefa 5");
	}
}
