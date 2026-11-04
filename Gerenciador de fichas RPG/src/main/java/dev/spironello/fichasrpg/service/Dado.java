package dev.spironello.fichasrpg.service;

/**
 * Fonte de aleatoriedade do teste de perícia. É uma interface para que a regra e o serviço
 * possam ser testados com uma rolagem fixa, sem depender de sorte.
 */
@FunctionalInterface
public interface Dado {

	/** Rola um dado de cem faces: devolve um inteiro de 1 a 100. */
	int rolarD100();
}
