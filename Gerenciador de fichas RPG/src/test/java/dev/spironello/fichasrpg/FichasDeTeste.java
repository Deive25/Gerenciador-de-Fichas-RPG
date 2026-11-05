package dev.spironello.fichasrpg;

import dev.spironello.fichasrpg.model.AtributoOrdem;
import dev.spironello.fichasrpg.model.FichaOrdem;

/** Fichas válidas para os testes que só precisam de "uma ficha" no banco. */
public final class FichasDeTeste {

	private FichasDeTeste() {
	}

	/** Ficha de Ordem Paranormal válida: NEX 5, os cinco atributos em 1, sem perícias nem recursos. */
	public static FichaOrdem ordem(String personagem) {
		FichaOrdem ficha = new FichaOrdem();
		ficha.setJogador("Ana");
		ficha.setPersonagem(personagem);
		ficha.setGenero("F");
		ficha.setNex(5);
		for (AtributoOrdem atributo : AtributoOrdem.values()) {
			ficha.setAtributo(atributo, 1);
		}
		return ficha;
	}
}
