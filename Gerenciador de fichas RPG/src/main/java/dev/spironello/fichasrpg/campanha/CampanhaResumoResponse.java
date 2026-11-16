package dev.spironello.fichasrpg.campanha;

import dev.spironello.fichasrpg.dto.Referencia;

/** Item de GET /campanhas. {@code fichas} conta as fichas que quem pede vê na mesa (o jogador não conta as do mestre). */
public record CampanhaResumoResponse(
		Long id,
		String nome,
		Papel papel,
		Referencia mestre,
		int fichas) {
}
