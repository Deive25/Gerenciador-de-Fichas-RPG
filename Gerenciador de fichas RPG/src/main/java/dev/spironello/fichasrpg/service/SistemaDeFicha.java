package dev.spironello.fichasrpg.service;

import java.util.Map;

import dev.spironello.fichasrpg.dto.FichaRequest;
import dev.spironello.fichasrpg.dto.FichaResponse;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.dto.TesteRequest;
import dev.spironello.fichasrpg.dto.TesteResponse;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.Sistema;

/**
 * As regras de um sistema de RPG num lugar só: validar e converter o pedido, montar as respostas, a edição
 * rápida dos recursos e a rolagem de testes. O {@code FichaService} e o {@code TesteService} não conhecem
 * nenhum sistema: pedem ao {@link SistemasDeFicha} o que atende a ficha e delegam. Sistema novo é uma
 * implementação nova desta interface, sem mexer nesses serviços.
 */
public interface SistemaDeFicha {

	/** O sistema que esta implementação atende. */
	Sistema sistema();

	/** Valida e converte o pedido (que é deste sistema) numa entidade nova. */
	Ficha criar(FichaRequest pedido);

	/** Valida o pedido e o copia sobre a ficha, inclusive os nulos (PUT). */
	void copiar(FichaRequest pedido, Ficha ficha);

	FichaResponse resposta(Ficha ficha);

	FichaResumoResponse resumo(Ficha ficha);

	/** Edição rápida (PATCH): muda só os valores atuais. Se algo for inválido, nada muda. */
	void atualizarRecursos(Ficha ficha, Map<String, Integer> atuais) throws DadosInvalidosException;

	/** Rola um teste na ficha, sem gravar nada. */
	TesteResponse testar(Ficha ficha, TesteRequest pedido, Dado dado) throws DadosInvalidosException;
}
