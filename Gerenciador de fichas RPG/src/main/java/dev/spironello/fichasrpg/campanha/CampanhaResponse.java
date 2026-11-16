package dev.spironello.fichasrpg.campanha;

import dev.spironello.fichasrpg.dto.Referencia;

/** Uma campanha por inteiro, vista por quem participa. {@code convite} só vem para o mestre (para o jogador é null). */
public record CampanhaResponse(
		Long id,
		String nome,
		String descricao,
		Papel meuPapel,
		Referencia mestre,
		ConviteResponse convite) {
}
