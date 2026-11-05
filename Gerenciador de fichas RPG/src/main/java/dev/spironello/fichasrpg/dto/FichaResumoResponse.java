package dev.spironello.fichasrpg.dto;

import java.util.List;

import dev.spironello.fichasrpg.model.Sistema;

/**
 * Item da listagem (GET /fichas): o que o painel mostra, sem atributos nem perícias. Por não tocar nas
 * perícias a listagem continua sendo uma consulta só. Os recursos vêm numa lista (PV, SAN e PE, nessa
 * ordem em Ordem), assim outros sistemas podem ter recursos diferentes.
 */
public record FichaResumoResponse(
		Long id,
		Sistema sistema,
		String jogador,
		String personagem,
		Integer idade,
		Long retratoVersao,
		List<RecursoResumo> recursos) {
}
