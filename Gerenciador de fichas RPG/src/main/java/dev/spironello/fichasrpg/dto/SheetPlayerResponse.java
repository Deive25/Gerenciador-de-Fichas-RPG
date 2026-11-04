package dev.spironello.fichasrpg.dto;

import java.util.Map;

import dev.spironello.fichasrpg.model.Pericia;

/**
 * Corpo de saída da API (contrato do incremento 1.6, ADR 0012): campos escalares planos e o
 * objeto "pericias", com só as perícias preenchidas, chaveado pelo código do enum.
 */
public record SheetPlayerResponse(
		Long id,
		String jogador,
		String personagem,
		String genero,
		Integer idade,
		Integer vidaAtual,
		Integer vidaMaxima,
		Integer sanidadeAtual,
		Integer sanidadeMaxima,
		Integer forca,
		Integer destreza,
		Integer constituicao,
		Integer inteligencia,
		Integer carisma,
		Integer sabedoria,
		Integer tamanho,
		Integer sorte,
		Integer poder,
		Map<Pericia, Integer> pericias) {
}
