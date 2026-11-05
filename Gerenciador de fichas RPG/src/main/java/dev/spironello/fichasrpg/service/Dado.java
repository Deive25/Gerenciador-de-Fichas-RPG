package dev.spironello.fichasrpg.service;

/**
 * Fonte de aleatoriedade dos testes. É uma interface para que a regra e o serviço
 * possam ser testados com uma rolagem fixa, sem depender de sorte.
 */
@FunctionalInterface
public interface Dado {

	/** Rola um dado de {@code faces} faces: devolve um inteiro de 1 a {@code faces}. */
	int rolar(int faces);

	/** Dado de cem faces, usado pela regra antiga (d100) até ela sair na Tarefa 2. */
	default int rolarD100() {
		return rolar(100);
	}
}
