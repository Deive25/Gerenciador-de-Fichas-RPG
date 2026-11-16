package dev.spironello.fichasrpg.dto;

import dev.spironello.fichasrpg.model.Ficha;

/** Referência curta a outra coisa, só com o id e o nome: o {@code dono}, a {@code campanha} e o {@code mestre} nas respostas. */
public record Referencia(Long id, String nome) {

	/** O dono da ficha. */
	public static Referencia doDono(Ficha ficha) {
		return new Referencia(ficha.getDonoId(), ficha.getDonoNome());
	}

	/** A campanha da ficha, ou null quando ela está sem campanha. */
	public static Referencia daCampanha(Ficha ficha) {
		return ficha.getCampanhaId() == null ? null : new Referencia(ficha.getCampanhaId(), ficha.getCampanhaNome());
	}
}
