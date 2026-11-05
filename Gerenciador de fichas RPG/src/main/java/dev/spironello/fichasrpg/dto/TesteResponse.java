package dev.spironello.fichasrpg.dto;

import java.util.List;

/** Resultado da rolagem: todos os dados, o dado mantido, o bônus do grau e o total (mantido + bônus). */
public record TesteResponse(AlvoTeste alvo, String codigo, String nome, List<Integer> dados, int mantido, int bonus,
		int total) {
}
