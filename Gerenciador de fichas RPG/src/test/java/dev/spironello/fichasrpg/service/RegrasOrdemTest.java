package dev.spironello.fichasrpg.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import dev.spironello.fichasrpg.model.GrauOrdem;

/** Unitário: a regra de rolagem de Ordem com um {@link Dado} de sequência fixa. */
@DisplayName("RegrasOrdem (teste com d20)")
class RegrasOrdemTest {

	private static Dado sequencia(Integer... valores) {
		Iterator<Integer> it = List.of(valores).iterator();
		return faces -> it.next();
	}

	@Test
	@DisplayName("atributo 3 rola 3 dados e mantém o maior")
	void atributoTresMantemMaior() {
		TesteRolado r = RegrasOrdem.rolar(3, null, sequencia(4, 17, 9));

		assertThat(r.dados()).containsExactly(4, 17, 9);
		assertThat(r.mantido()).isEqualTo(17);
		assertThat(r.bonus()).isZero();
		assertThat(r.total()).isEqualTo(17);
	}

	@Test
	@DisplayName("atributo 0 rola 2 dados e mantém o menor")
	void atributoZeroMantemMenor() {
		TesteRolado r = RegrasOrdem.rolar(0, null, sequencia(12, 5));

		assertThat(r.dados()).containsExactly(12, 5);
		assertThat(r.mantido()).isEqualTo(5);
	}

	@Test
	@DisplayName("Expert com atributo 0 soma 15 ao menor dado")
	void expertComAtributoZero() {
		TesteRolado r = RegrasOrdem.rolar(0, GrauOrdem.EXPERT, sequencia(12, 5));

		assertThat(r.bonus()).isEqualTo(15);
		assertThat(r.total()).isEqualTo(20);
	}

	@ParameterizedTest(name = "atributo {0} rola {1} dado(s)")
	@CsvSource({ "0,2", "1,1", "2,2", "3,3", "4,4", "5,5" })
	@DisplayName("quantidade de dados")
	void quantidadeDeDados(int atributo, int esperado) {
		assertThat(RegrasOrdem.quantidadeDeDados(atributo)).isEqualTo(esperado);
		assertThat(RegrasOrdem.rolar(atributo, null, faces -> 10).dados()).hasSize(esperado);
	}

	@Test
	@DisplayName("só o atributo 0 mantém o menor")
	void mantemMaior() {
		assertThat(RegrasOrdem.mantemMaior(0)).isFalse();
		assertThat(RegrasOrdem.mantemMaior(1)).isTrue();
		assertThat(RegrasOrdem.mantemMaior(5)).isTrue();
	}

	@ParameterizedTest(name = "{0} dá bônus {1}")
	@CsvSource({ "TREINADO,5", "VETERANO,10", "EXPERT,15" })
	@DisplayName("bônus do grau")
	void bonusDoGrau(GrauOrdem grau, int esperado) {
		assertThat(RegrasOrdem.bonus(grau)).isEqualTo(esperado);
	}

	@Test
	@DisplayName("grau nulo (destreinado) dá bônus 0 e o NEX mínimo dos graus é fixo")
	void grauNuloENexMinimo() {
		assertThat(RegrasOrdem.bonus(null)).isZero();
		assertThat(GrauOrdem.TREINADO.getNexMinimo()).isZero();
		assertThat(GrauOrdem.VETERANO.getNexMinimo()).isEqualTo(35);
		assertThat(GrauOrdem.EXPERT.getNexMinimo()).isEqualTo(70);
	}

	@Test
	@DisplayName("o dado é rolado com 20 faces")
	void rolaD20() {
		RegrasOrdem.rolar(1, null, faces -> {
			assertThat(faces).isEqualTo(20);
			return 1;
		});
	}
}
