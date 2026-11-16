package dev.spironello.fichasrpg.campanha;

import java.security.SecureRandom;

/**
 * Gerador do código do convite: 12 caracteres de A-Z, a-z e 0-9 (62 símbolos, cerca de 71 bits), sorteados por um
 * {@link SecureRandom} só. Não é adivinhável: o link do convite é a única "senha" de quem entra na campanha.
 */
public final class CodigoDeConvite {

	public static final int TAMANHO = 12;

	private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final SecureRandom ALEATORIO = new SecureRandom();

	private CodigoDeConvite() {
	}

	public static String gerar() {
		StringBuilder codigo = new StringBuilder(TAMANHO);
		for (int i = 0; i < TAMANHO; i++) {
			codigo.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
		}
		return codigo.toString();
	}
}
