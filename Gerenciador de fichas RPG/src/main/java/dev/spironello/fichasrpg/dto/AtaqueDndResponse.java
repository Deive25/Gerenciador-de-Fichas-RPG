package dev.spironello.fichasrpg.dto;

import dev.spironello.fichasrpg.model.AtributoDnd;

/** Um ataque de D&D: o atributo e se soma a proficiência. */
public record AtaqueDndResponse(Long id, String nome, AtributoDnd atributo, boolean proficiente, int bonus,
		String dano, String tipoDano, String alcance) implements AtaqueResponse {
}
