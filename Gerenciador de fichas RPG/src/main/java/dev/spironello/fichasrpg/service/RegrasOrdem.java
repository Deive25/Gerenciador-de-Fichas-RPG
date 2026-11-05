package dev.spironello.fichasrpg.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.spironello.fichasrpg.model.GrauOrdem;

/**
 * Regra do teste de Ordem: rola tantos d20 quanto o atributo (com atributo 0, rola 2) e fica com o
 * maior (com atributo 0, o menor). O total soma o bônus do grau da perícia. Classe sem estado e sem Spring.
 */
public final class RegrasOrdem {

	private static final int FACES = 20;

	private RegrasOrdem() {
	}

	public static int quantidadeDeDados(int atributo) {
		return atributo == 0 ? 2 : atributo;
	}

	public static boolean mantemMaior(int atributo) {
		return atributo > 0;
	}

	/** Bônus do grau; grau nulo é destreinado, bônus 0. */
	public static int bonus(GrauOrdem grauOuNulo) {
		return grauOuNulo == null ? 0 : grauOuNulo.getBonus();
	}

	public static TesteRolado rolar(int atributo, GrauOrdem grauOuNulo, Dado dado) {
		int quantidade = quantidadeDeDados(atributo);
		List<Integer> dados = new ArrayList<>(quantidade);
		for (int i = 0; i < quantidade; i++) {
			dados.add(dado.rolar(FACES));
		}
		int mantido = mantemMaior(atributo) ? Collections.max(dados) : Collections.min(dados);
		int bonus = bonus(grauOuNulo);
		return new TesteRolado(List.copyOf(dados), mantido, bonus, mantido + bonus);
	}
}
