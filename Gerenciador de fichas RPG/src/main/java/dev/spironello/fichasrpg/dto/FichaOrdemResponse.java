package dev.spironello.fichasrpg.dto;

import java.util.List;
import java.util.Map;

import dev.spironello.fichasrpg.model.AtributoOrdem;
import dev.spironello.fichasrpg.model.ClasseOrdem;
import dev.spironello.fichasrpg.model.GrauOrdem;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.model.RecursoOrdem;
import dev.spironello.fichasrpg.model.Sistema;

/**
 * Ficha de Ordem Paranormal completa. Os mapas são EnumMap (ordem estável, a do enum); "recursos" traz
 * sempre PV, SAN e PE (com valores nulos quando não preenchidos) e "pericias" só as treinadas.
 * As chaves "habilidades" e "itens" são listas à parte da ficha (não vêm no PUT, ADR 0018).
 * Não leva anotação de tipo: "sistema" já é um componente do record e sai no JSON como os outros.
 */
public record FichaOrdemResponse(
		Long id,
		Sistema sistema,
		String jogador,
		String personagem,
		String genero,
		Integer idade,
		Long retratoVersao,
		DescricaoFicha descricao,
		List<HabilidadeResponse> habilidades,
		List<ItemOrdemResponse> itens,
		List<RitualResponse> rituais,
		ClasseOrdem classe,
		String origem,
		int nex,
		Map<AtributoOrdem, Integer> atributos,
		Map<RecursoOrdem, RecursoValor> recursos,
		Map<PericiaOrdem, GrauOrdem> pericias) implements FichaResponse {
}
