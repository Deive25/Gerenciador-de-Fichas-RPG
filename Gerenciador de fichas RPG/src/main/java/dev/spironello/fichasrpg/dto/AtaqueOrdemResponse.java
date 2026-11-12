package dev.spironello.fichasrpg.dto;

import dev.spironello.fichasrpg.model.PericiaOrdem;

/** Um ataque de Ordem Paranormal: a perícia (Luta ou Pontaria), a margem de ameaça e o multiplicador do crítico. */
public record AtaqueOrdemResponse(Long id, String nome, PericiaOrdem pericia, int bonus, String dano,
		String tipoDano, String alcance, int margem, int multiplicador) implements AtaqueResponse {
}
