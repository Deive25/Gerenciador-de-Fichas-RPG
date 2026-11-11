package dev.spironello.fichasrpg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.spironello.fichasrpg.dto.DadoRolado;
import dev.spironello.fichasrpg.exception.NotacaoInvalidaException;

/** Unitário: a notação de dados (leitura, forma canônica e rolagem com o multiplicador do crítico). */
@DisplayName("NotacaoDeDados (2d6+1d4+3)")
class NotacaoDeDadosTest {

	/** Dado que devolve os valores na ordem; falha se rolarem mais dados do que o esperado. */
	private static Dado sequencia(Integer... valores) {
		Iterator<Integer> it = List.of(valores).iterator();
		return faces -> it.next();
	}

	/** Dado que devolve sempre o mesmo valor. */
	private static Dado fixo(int valor) {
		return faces -> valor;
	}

	@ParameterizedTest(name = "\"{0}\" vira \"{1}\"")
	@CsvSource(delimiter = '|', value = {
			"1d8+3|1d8+3",
			"2d6+1d4+2|2d6+1d4+2",
			"1d10-1|1d10-1",
			"' 2D6 + 3 '|2d6+3",
			"1d100|1d100",
			"5|5",
			"20d6|20d6",
			"3+2d6|3+2d6",
			"0|0",
			"99|99" })
	@DisplayName("expressões válidas viram a forma canônica (sem espaços, em minúsculas)")
	void validas(String texto, String canonica) {
		assertThat(NotacaoDeDados.ler(texto).canonica()).isEqualTo(canonica);
	}

	@Test
	@DisplayName("8 termos passam")
	void oitoTermos() {
		assertThat(NotacaoDeDados.ler("1d4+1d6+1d8+1d10+1d12+1d20+1d100+3").canonica())
				.isEqualTo("1d4+1d6+1d8+1d10+1d12+1d20+1d100+3");
	}

	@ParameterizedTest(name = "\"{0}\" -> {1}")
	@CsvSource(delimiter = '|', value = {
			"d6|notação inválida (ex.: 2d6+3)",
			"abc|notação inválida (ex.: 2d6+3)",
			"1d6+|notação inválida (ex.: 2d6+3)",
			"+1d6|notação inválida (ex.: 2d6+3)",
			"1d6++2|notação inválida (ex.: 2d6+3)",
			"1d|notação inválida (ex.: 2d6+3)",
			"0d6|use de 1 a 20 dados em cada termo",
			"1d7|use d4, d6, d8, d10, d12, d20 ou d100",
			"1d0|use d4, d6, d8, d10, d12, d20 ou d100",
			"21d6|no máximo 20 dados",
			"11d6+10d6|no máximo 20 dados",
			"1d8-1d4|dados não podem ser subtraídos",
			"100|a constante deve ir de 0 a 99",
			"1d6+100|a constante deve ir de 0 a 99" })
	@DisplayName("expressões inválidas dão a mensagem do caso")
	void invalidas(String texto, String mensagem) {
		assertThatThrownBy(() -> NotacaoDeDados.ler(texto))
				.isInstanceOf(NotacaoInvalidaException.class)
				.hasMessage(mensagem);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   " })
	@DisplayName("vazio ou só espaços é inválido")
	void vazio(String texto) {
		assertThatThrownBy(() -> NotacaoDeDados.ler(texto))
				.isInstanceOf(NotacaoInvalidaException.class)
				.hasMessage("notação inválida (ex.: 2d6+3)");
	}

	@Test
	@DisplayName("nulo é inválido")
	void nulo() {
		assertThatThrownBy(() -> NotacaoDeDados.ler(null)).isInstanceOf(NotacaoInvalidaException.class);
	}

	@Test
	@DisplayName("9 termos dão erro")
	void noveTermos() {
		assertThatThrownBy(() -> NotacaoDeDados.ler("1+1+1+1+1+1+1+1+1"))
				.isInstanceOf(NotacaoInvalidaException.class)
				.hasMessage("no máximo 8 termos");
	}

	@Test
	@DisplayName("número gigante não estoura: vira erro de limite")
	void numeroGigante() {
		assertThatThrownBy(() -> NotacaoDeDados.ler("99999999999999999999d6"))
				.isInstanceOf(NotacaoInvalidaException.class)
				.hasMessage("no máximo 20 dados");
		assertThatThrownBy(() -> NotacaoDeDados.ler("99999999999999999999"))
				.isInstanceOf(NotacaoInvalidaException.class)
				.hasMessage("a constante deve ir de 0 a 99");
	}

	@Test
	@DisplayName("rola 2d6+1d4+3: dados na ordem da expressão, bônus 3 e total somado")
	void rolaNormal() {
		Rolagem r = NotacaoDeDados.rolar(NotacaoDeDados.ler("2d6+1d4+3"), 1, sequencia(4, 1, 3));

		assertThat(r.dados()).containsExactly(new DadoRolado(6, 4), new DadoRolado(6, 1), new DadoRolado(4, 3));
		assertThat(r.bonus()).isEqualTo(3);
		assertThat(r.total()).isEqualTo(11);
	}

	@Test
	@DisplayName("crítico x2 dobra os dados e não a constante")
	void criticoDobra() {
		Rolagem r = NotacaoDeDados.rolar(NotacaoDeDados.ler("1d8+3"), 2, sequencia(6, 5));

		assertThat(r.dados()).containsExactly(new DadoRolado(8, 6), new DadoRolado(8, 5));
		assertThat(r.bonus()).isEqualTo(3);
		assertThat(r.total()).isEqualTo(14);
	}

	@Test
	@DisplayName("crítico x3 triplica os dados de cada termo, em sequência, e não a constante")
	void criticoTriplica() {
		Rolagem r = NotacaoDeDados.rolar(NotacaoDeDados.ler("1d10+1d4+2"), 3, sequencia(1, 2, 3, 4, 5, 6));

		assertThat(r.dados()).extracting(DadoRolado::lados).containsExactly(10, 10, 10, 4, 4, 4);
		assertThat(r.bonus()).isEqualTo(2);
		assertThat(r.total()).isEqualTo(1 + 2 + 3 + 4 + 5 + 6 + 2);
	}

	@Test
	@DisplayName("as constantes somam com sinal: 1d6+5-2 tem bônus 3")
	void constantesComSinal() {
		Rolagem r = NotacaoDeDados.rolar(NotacaoDeDados.ler("1d6+5-2"), 1, fixo(4));

		assertThat(r.bonus()).isEqualTo(3);
		assertThat(r.total()).isEqualTo(7);
	}

	@Test
	@DisplayName("total negativo vira 0 (1d4-5), e o bônus continua -5")
	void totalNegativoVira0() {
		Rolagem r = NotacaoDeDados.rolar(NotacaoDeDados.ler("1d4-5"), 1, fixo(2));

		assertThat(r.bonus()).isEqualTo(-5);
		assertThat(r.total()).isZero();
	}

	@Test
	@DisplayName("só constante: nenhum dado, total igual à constante")
	void soConstante() {
		List<Integer> chamadas = new ArrayList<>();
		Rolagem r = NotacaoDeDados.rolar(NotacaoDeDados.ler("7"), 2, faces -> {
			chamadas.add(faces);
			return 1;
		});

		assertThat(r.dados()).isEmpty();
		assertThat(chamadas).isEmpty();
		assertThat(r.total()).isEqualTo(7);
	}

	@Test
	@DisplayName("o d100 rola com 100 faces")
	void d100() {
		List<Integer> faces = new ArrayList<>();
		NotacaoDeDados.rolar(NotacaoDeDados.ler("1d100"), 1, f -> {
			faces.add(f);
			return 50;
		});

		assertThat(faces).containsExactly(100);
	}
}
