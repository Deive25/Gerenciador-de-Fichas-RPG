package dev.spironello.fichasrpg.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.spironello.fichasrpg.dto.ModoTeste;

/** Unitário: quando um ataque é crítico (Ordem pela margem, D&D só no 20 natural), nas bordas. */
@DisplayName("Crítico do ataque")
class CriticoTest {

	private static Dado sequencia(Integer... valores) {
		Iterator<Integer> it = List.of(valores).iterator();
		return faces -> it.next();
	}

	@Test
	@DisplayName("Ordem com margem 19: mantido 19 é crítico, 18 não é")
	void ordemNaMargem() {
		assertThat(RegrasOrdem.critico(19, 19)).isTrue();
		assertThat(RegrasOrdem.critico(18, 19)).isFalse();
		assertThat(RegrasOrdem.critico(20, 19)).isTrue();
	}

	@Test
	@DisplayName("Ordem com a margem padrão 20: só o 20 é crítico")
	void ordemMargemPadrao() {
		assertThat(RegrasOrdem.critico(20, 20)).isTrue();
		assertThat(RegrasOrdem.critico(19, 20)).isFalse();
	}

	@Test
	@DisplayName("D&D: 20 natural é crítico, 19 não")
	void dnd() {
		assertThat(RegrasDnd.critico(20)).isTrue();
		assertThat(RegrasDnd.critico(19)).isFalse();
	}

	@Test
	@DisplayName("D&D com desvantagem [20, 3]: o mantido é 3 e não é crítico")
	void desvantagemComVinteNoOutroDado() {
		TesteRolado r = RegrasDnd.rolar(5, ModoTeste.DESVANTAGEM, sequencia(20, 3));

		assertThat(r.mantido()).isEqualTo(3);
		assertThat(RegrasDnd.critico(r.mantido())).isFalse();
	}

	@Test
	@DisplayName("D&D com vantagem [20, 3]: o mantido é 20 e é crítico")
	void vantagemComVinte() {
		TesteRolado r = RegrasDnd.rolar(5, ModoTeste.VANTAGEM, sequencia(20, 3));

		assertThat(RegrasDnd.critico(r.mantido())).isTrue();
	}
}
