package dev.spironello.fichasrpg.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Fixa o catálogo das perícias de D&D (iguais nas duas edições). Unitário: sem Spring e sem Docker. */
@DisplayName("PericiaDnd (catálogo)")
class PericiaDndTest {

	@Test
	@DisplayName("são 18 perícias")
	void sao18() {
		assertThat(PericiaDnd.values()).hasSize(18);
	}

	@Test
	@DisplayName("a distribuição por atributo é FOR 1, DES 3, CON 0, INT 5, SAB 5 e CAR 4")
	void distribuicaoPorAtributo() {
		Map<AtributoDnd, Long> contagem = Arrays.stream(PericiaDnd.values())
				.collect(Collectors.groupingBy(PericiaDnd::getAtributo, Collectors.counting()));

		assertThat(contagem).containsEntry(AtributoDnd.FOR, 1L).containsEntry(AtributoDnd.DES, 3L)
				.containsEntry(AtributoDnd.INT, 5L).containsEntry(AtributoDnd.SAB, 5L)
				.containsEntry(AtributoDnd.CAR, 4L).doesNotContainKey(AtributoDnd.CON);
	}

	@Test
	@DisplayName("os nomes têm acento e não se repetem")
	void nomes() {
		assertThat(PericiaDnd.PRESTIDIGITACAO.getNome()).isEqualTo("Prestidigitação");
		assertThat(PericiaDnd.LIDAR_COM_ANIMAIS.getNome()).isEqualTo("Lidar com Animais");
		assertThat(Arrays.stream(PericiaDnd.values()).map(PericiaDnd::getNome).distinct()).hasSize(18);
	}

	@Test
	@DisplayName("os atributos têm nome e as edições e recursos existem")
	void atributosEOutros() {
		assertThat(AtributoDnd.values()).hasSize(6);
		assertThat(AtributoDnd.CON.getNome()).isEqualTo("Constituição");
		assertThat(EdicaoDnd.values()).containsExactly(EdicaoDnd.D2014, EdicaoDnd.D2024);
		assertThat(GrauDnd.values()).containsExactly(GrauDnd.PROFICIENTE, GrauDnd.ESPECIALISTA);
		assertThat(RecursoDnd.values()).containsExactly(RecursoDnd.PV, RecursoDnd.PV_TEMP);
	}
}
