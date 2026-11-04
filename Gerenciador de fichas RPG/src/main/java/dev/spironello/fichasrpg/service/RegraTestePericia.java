package dev.spironello.fichasrpg.service;

import dev.spironello.fichasrpg.model.ResultadoTeste;

/**
 * Regra pura do teste de perícia: sem Spring, sem banco e sem aleatoriedade (a rolagem entra
 * como argumento). Por isso dá para testar todas as fronteiras com valores fixos.
 *
 * <ul>
 * <li>rolagem &lt;= valor / 5 (divisão inteira): sucesso extremo;</li>
 * <li>rolagem &lt;= valor: sucesso;</li>
 * <li>caso contrário: falha.</li>
 * </ul>
 * Não há crítico nem desastre (ADR 0011).
 */
public final class RegraTestePericia {

	private RegraTestePericia() {
	}

	/**
	 * @param valor   valor da perícia na ficha (0 para perícia não treinada)
	 * @param rolagem resultado do d100, de 1 a 100
	 * @throws IllegalArgumentException se a rolagem estiver fora de 1..100
	 */
	public static ResultadoTeste avaliar(int valor, int rolagem) {
		if (rolagem < 1 || rolagem > 100) {
			throw new IllegalArgumentException("A rolagem deve estar entre 1 e 100: " + rolagem);
		}
		if (rolagem <= valor / 5) {
			return ResultadoTeste.SUCESSO_EXTREMO;
		}
		if (rolagem <= valor) {
			return ResultadoTeste.SUCESSO;
		}
		return ResultadoTeste.FALHA;
	}
}
