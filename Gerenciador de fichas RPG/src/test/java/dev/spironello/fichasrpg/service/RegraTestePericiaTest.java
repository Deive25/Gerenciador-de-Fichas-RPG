package dev.spironello.fichasrpg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.spironello.fichasrpg.model.ResultadoTeste;

/**
 * Regras do teste de perícia nas fronteiras. Sem Spring e sem Docker: a rolagem entra como
 * argumento, então o resultado é sempre o mesmo.
 */
@DisplayName("RegraTestePericia (unitário)")
class RegraTestePericiaTest {

	@ParameterizedTest(name = "valor {0}, rolagem {1} -> {2}")
	@DisplayName("avaliar classifica a rolagem nas fronteiras")
	@CsvSource({
			// valor 50: extremo até 10 (50 / 5), sucesso até 50, falha depois
			"50, 1, SUCESSO_EXTREMO",
			"50, 10, SUCESSO_EXTREMO",
			"50, 11, SUCESSO",
			"50, 50, SUCESSO",
			"50, 51, FALHA",
			"50, 100, FALHA",
			// valor 49: 49 / 5 = 9 na divisão inteira, então 10 já é só sucesso
			"49, 9, SUCESSO_EXTREMO",
			"49, 10, SUCESSO",
			// valor 0 (perícia não treinada): sempre falha
			"0, 1, FALHA",
			// valor 100: extremo até 20
			"100, 20, SUCESSO_EXTREMO",
			"100, 21, SUCESSO",
			"100, 100, SUCESSO",
			// valor acima de 100 não quebra: continua valendo a mesma regra
			"150, 100, SUCESSO"
	})
	void avaliarNasFronteiras(int valor, int rolagem, ResultadoTeste esperado) {
		assertThat(RegraTestePericia.avaliar(valor, rolagem)).isEqualTo(esperado);
	}

	@ParameterizedTest(name = "rolagem {0} é inválida")
	@DisplayName("avaliar rejeita rolagem fora de 1..100")
	@ValueSource(ints = { 0, 101, -1 })
	void rolagemForaDoIntervalo(int rolagem) {
		assertThatThrownBy(() -> RegraTestePericia.avaliar(50, rolagem))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("as duas pontas do dado (1 e 100) são aceitas")
	void pontasDoDadoSaoValidas() {
		assertThat(RegraTestePericia.avaliar(50, 1)).isNotNull();
		assertThat(RegraTestePericia.avaliar(50, 100)).isNotNull();
	}
}
