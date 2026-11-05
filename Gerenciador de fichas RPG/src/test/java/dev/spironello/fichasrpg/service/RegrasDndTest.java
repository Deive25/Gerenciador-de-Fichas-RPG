package dev.spironello.fichasrpg.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import dev.spironello.fichasrpg.dto.ModoTeste;
import dev.spironello.fichasrpg.model.GrauDnd;

/** Unitário: as regras de D&D (modificador, proficiência e rolagem) com um {@link Dado} de sequência fixa. */
@DisplayName("RegrasDnd (d20, proficiência e vantagem)")
class RegrasDndTest {

	private static Dado sequencia(Integer... valores) {
		Iterator<Integer> it = List.of(valores).iterator();
		return faces -> it.next();
	}

	@ParameterizedTest(name = "valor {0} dá modificador {1}")
	@CsvSource({ "1,-5", "8,-1", "9,-1", "10,0", "11,0", "12,1", "30,10" })
	@DisplayName("modificador arredonda para baixo, também nos negativos")
	void modificador(int valor, int esperado) {
		assertThat(RegrasDnd.modificador(valor)).isEqualTo(esperado);
	}

	@ParameterizedTest(name = "nível {0} dá proficiência {1}")
	@CsvSource({ "1,2", "4,2", "5,3", "8,3", "9,4", "13,5", "17,6", "20,6" })
	@DisplayName("proficiência por nível")
	void proficiencia(int nivel, int esperado) {
		assertThat(RegrasDnd.proficiencia(nivel)).isEqualTo(esperado);
	}

	@Test
	@DisplayName("bônus de perícia: proficiente, especialista e sem proficiência")
	void bonusPericia() {
		assertThat(RegrasDnd.bonusPericia(14, 5, GrauDnd.PROFICIENTE)).isEqualTo(5);
		assertThat(RegrasDnd.bonusPericia(14, 5, GrauDnd.ESPECIALISTA)).isEqualTo(8);
		assertThat(RegrasDnd.bonusPericia(14, 5, null)).isEqualTo(2);
	}

	@Test
	@DisplayName("bônus de salvaguarda: com e sem proficiência")
	void bonusSalvaguarda() {
		assertThat(RegrasDnd.bonusSalvaguarda(8, 1, true)).isEqualTo(1);
		assertThat(RegrasDnd.bonusSalvaguarda(8, 1, false)).isEqualTo(-1);
	}

	@Test
	@DisplayName("NORMAL rola 1d20 e soma o bônus")
	void normal() {
		TesteRolado r = RegrasDnd.rolar(3, ModoTeste.NORMAL, sequencia(15));

		assertThat(r.dados()).containsExactly(15);
		assertThat(r.mantido()).isEqualTo(15);
		assertThat(r.bonus()).isEqualTo(3);
		assertThat(r.total()).isEqualTo(18);
	}

	@Test
	@DisplayName("modo nulo é NORMAL")
	void modoNulo() {
		TesteRolado r = RegrasDnd.rolar(3, null, sequencia(15));

		assertThat(r.dados()).containsExactly(15);
		assertThat(r.total()).isEqualTo(18);
	}

	@Test
	@DisplayName("VANTAGEM rola 2d20 e mantém o maior")
	void vantagem() {
		TesteRolado r = RegrasDnd.rolar(0, ModoTeste.VANTAGEM, sequencia(4, 17));

		assertThat(r.dados()).containsExactly(4, 17);
		assertThat(r.mantido()).isEqualTo(17);
	}

	@Test
	@DisplayName("DESVANTAGEM rola 2d20 e mantém o menor, com bônus negativo")
	void desvantagem() {
		TesteRolado r = RegrasDnd.rolar(-1, ModoTeste.DESVANTAGEM, sequencia(4, 17));

		assertThat(r.dados()).containsExactly(4, 17);
		assertThat(r.mantido()).isEqualTo(4);
		assertThat(r.bonus()).isEqualTo(-1);
		assertThat(r.total()).isEqualTo(3);
	}

	@Test
	@DisplayName("o Dado é chamado com 20 faces")
	void vinteFaces() {
		List<Integer> faces = new ArrayList<>();
		RegrasDnd.rolar(0, ModoTeste.VANTAGEM, f -> {
			faces.add(f);
			return 10;
		});

		assertThat(faces).containsExactly(20, 20);
	}
}
