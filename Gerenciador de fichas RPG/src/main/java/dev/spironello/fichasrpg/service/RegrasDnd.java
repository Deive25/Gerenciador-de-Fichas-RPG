package dev.spironello.fichasrpg.service;

import java.util.Collections;
import java.util.List;

import dev.spironello.fichasrpg.dto.ModoTeste;
import dev.spironello.fichasrpg.model.GrauDnd;

/**
 * Regras de D&D: modificador, proficiência e a rolagem do d20 com vantagem e desvantagem.
 * Classe sem estado e sem Spring. As duas edições (2014 e 2024) usam as mesmas fórmulas.
 */
public final class RegrasDnd {

	private static final int FACES = 20;

	private RegrasDnd() {
	}

	/** (valor - 10) / 2 arredondado para baixo; o {@code floorDiv} evita que 9 dê 0 em vez de -1. */
	public static int modificador(int valor) {
		return Math.floorDiv(valor - 10, 2);
	}

	public static int proficiencia(int nivel) {
		return 2 + (nivel - 1) / 4;
	}

	/** Grau nulo é sem proficiência: só o modificador. Especialista soma a proficiência duas vezes. */
	public static int bonusPericia(int valor, int nivel, GrauDnd grauOuNulo) {
		int bonus = modificador(valor);
		if (grauOuNulo == GrauDnd.PROFICIENTE) {
			return bonus + proficiencia(nivel);
		}
		if (grauOuNulo == GrauDnd.ESPECIALISTA) {
			return bonus + 2 * proficiencia(nivel);
		}
		return bonus;
	}

	public static int bonusSalvaguarda(int valor, int nivel, boolean proficiente) {
		return modificador(valor) + (proficiente ? proficiencia(nivel) : 0);
	}

	/** Modo nulo é NORMAL (1d20). Vantagem e desvantagem rolam 2d20 e ficam com o maior e o menor. */
	public static TesteRolado rolar(int bonus, ModoTeste modoOuNulo, Dado dado) {
		ModoTeste modo = modoOuNulo == null ? ModoTeste.NORMAL : modoOuNulo;
		List<Integer> dados = modo == ModoTeste.NORMAL ? List.of(dado.rolar(FACES))
				: List.of(dado.rolar(FACES), dado.rolar(FACES));
		int mantido = modo == ModoTeste.DESVANTAGEM ? Collections.min(dados) : Collections.max(dados);
		return new TesteRolado(dados, mantido, bonus, mantido + bonus);
	}

	/** Ataque crítico: só o 20 natural do d20 mantido (com desvantagem, o 20 no outro dado não conta). */
	public static boolean critico(int mantido) {
		return mantido == FACES;
	}
}
