package dev.spironello.fichasrpg.service;

/** Pequenos ajustes de texto que as listas da ficha repetem. */
final class Textos {

	private Textos() {
	}

	/** Texto opcional: vazio ou só espaços é "sem valor", e o banco guarda nulo. */
	static String vazioParaNulo(String texto) {
		return texto == null || texto.isBlank() ? null : texto;
	}
}
