package dev.spironello.fichasrpg.dto;

import java.util.Map;
import java.util.Set;

import dev.spironello.fichasrpg.model.AtributoDnd;
import dev.spironello.fichasrpg.model.EdicaoDnd;
import dev.spironello.fichasrpg.model.GrauDnd;
import dev.spironello.fichasrpg.model.PericiaDnd;
import dev.spironello.fichasrpg.model.RecursoDnd;
import dev.spironello.fichasrpg.model.Sistema;

/**
 * Ficha de D&D completa. Mapas em EnumMap e salvaguardas em EnumSet (ordem do enum). "recursos" traz sempre
 * PV e PV_TEMP, e o PV_TEMP tem sempre maximo nulo; "pericias" só as com proficiência.
 */
public record FichaDndResponse(
		Long id,
		Sistema sistema,
		String jogador,
		String personagem,
		String genero,
		Integer idade,
		Long retratoVersao,
		EdicaoDnd edicao,
		String classe,
		String especie,
		String antecedente,
		int nivel,
		Map<AtributoDnd, Integer> atributos,
		Map<RecursoDnd, RecursoValor> recursos,
		Integer ca,
		boolean inspiracao,
		Set<AtributoDnd> salvaguardas,
		Map<PericiaDnd, GrauDnd> pericias) implements FichaResponse {
}
