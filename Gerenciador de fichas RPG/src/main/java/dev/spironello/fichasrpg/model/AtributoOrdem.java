package dev.spironello.fichasrpg.model;

/** Os cinco atributos de Ordem Paranormal (valor de 0 a 5 na ficha). */
public enum AtributoOrdem {
	AGI("Agilidade"),
	FOR("Força"),
	INT("Intelecto"),
	PRE("Presença"),
	VIG("Vigor");

	private final String nome;

	AtributoOrdem(String nome) {
		this.nome = nome;
	}

	public String getNome() {
		return nome;
	}
}
