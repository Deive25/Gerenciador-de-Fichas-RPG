package dev.spironello.fichasrpg.model;

/**
 * Grau de treinamento de uma perícia. Sem grau (sem entrada na ficha) a perícia é destreinada, com bônus 0.
 * Cada grau guarda o bônus somado ao teste e o NEX mínimo para tê-lo.
 */
public enum GrauOrdem {
	TREINADO(5, 0),
	VETERANO(10, 35),
	EXPERT(15, 70);

	private final int bonus;
	private final int nexMinimo;

	GrauOrdem(int bonus, int nexMinimo) {
		this.bonus = bonus;
		this.nexMinimo = nexMinimo;
	}

	public int getBonus() {
		return bonus;
	}

	public int getNexMinimo() {
		return nexMinimo;
	}
}
