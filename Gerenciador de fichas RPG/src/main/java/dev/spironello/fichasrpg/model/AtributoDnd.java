package dev.spironello.fichasrpg.model;

/** Os seis atributos de D&D (valor de 1 a 30 na ficha). */
public enum AtributoDnd {
	FOR("Força"),
	DES("Destreza"),
	CON("Constituição"),
	INT("Inteligência"),
	SAB("Sabedoria"),
	CAR("Carisma");

	private final String nome;

	AtributoDnd(String nome) {
		this.nome = nome;
	}

	public String getNome() {
		return nome;
	}
}
