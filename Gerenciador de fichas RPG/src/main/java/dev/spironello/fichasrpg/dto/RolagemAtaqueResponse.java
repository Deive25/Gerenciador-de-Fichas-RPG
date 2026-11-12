package dev.spironello.fichasrpg.dto;

import java.util.List;

/**
 * Resultado da rolagem de um ataque: o d20 do ataque (todos os dados, o mantido, o bônus total e o total), o dano
 * rolado (já com o crítico) e se foi crítico.
 */
public record RolagemAtaqueResponse(Long ataqueId, String nome, Teste ataque, RolagemResponse dano, boolean critico) {

	/** O d20 do ataque: como o teste de perícia, com o bônus do ataque já somado ao bônus. */
	public record Teste(List<Integer> dados, int mantido, int bonus, int total) {
	}
}
