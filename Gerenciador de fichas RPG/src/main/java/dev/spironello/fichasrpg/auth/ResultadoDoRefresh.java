package dev.spironello.fichasrpg.auth;

/**
 * Resultado da renovação. O serviço devolve um resultado em vez de lançar exceção dentro da transação: a exceção
 * desfaria (rollback) a revogação da família que acontece quando um refresh já usado é reapresentado.
 */
public sealed interface ResultadoDoRefresh {

	record Renovado(RespostaAuth resposta) implements ResultadoDoRefresh {
	}

	record Recusado() implements ResultadoDoRefresh {
	}
}
