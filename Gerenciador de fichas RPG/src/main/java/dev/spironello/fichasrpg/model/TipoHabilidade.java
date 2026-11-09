package dev.spironello.fichasrpg.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Tipo de uma habilidade da ficha. Cada sistema aceita um subconjunto; CLASSE e OUTRA valem nos dois.
 * A coluna ficha_habilidade.tipo aceita todos; quem confere o sistema é {@link #permitidoEm(Sistema)}.
 */
public enum TipoHabilidade {
	CLASSE(EnumSet.of(Sistema.ORDEM_PARANORMAL, Sistema.DND)),
	TRILHA(EnumSet.of(Sistema.ORDEM_PARANORMAL)),
	ORIGEM(EnumSet.of(Sistema.ORDEM_PARANORMAL)),
	PARANORMAL(EnumSet.of(Sistema.ORDEM_PARANORMAL)),
	SUBCLASSE(EnumSet.of(Sistema.DND)),
	ESPECIE(EnumSet.of(Sistema.DND)),
	ANTECEDENTE(EnumSet.of(Sistema.DND)),
	TALENTO(EnumSet.of(Sistema.DND)),
	OUTRA(EnumSet.of(Sistema.ORDEM_PARANORMAL, Sistema.DND));

	private final Set<Sistema> sistemas;

	TipoHabilidade(Set<Sistema> sistemas) {
		this.sistemas = sistemas;
	}

	/** Diz se o tipo existe no sistema dado. */
	public boolean permitidoEm(Sistema sistema) {
		return sistemas.contains(sistema);
	}
}
