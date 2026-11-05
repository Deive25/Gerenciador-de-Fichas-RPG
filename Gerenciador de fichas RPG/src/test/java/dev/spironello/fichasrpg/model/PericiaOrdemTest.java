package dev.spironello.fichasrpg.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


/** Fixa o catálogo das perícias de Ordem Paranormal. Unitário: sem Spring e sem Docker. */
@DisplayName("PericiaOrdem (catálogo)")
class PericiaOrdemTest {

	@Test
	@DisplayName("são 28 perícias")
	void sao28() {
		assertThat(PericiaOrdem.values()).hasSize(28);
	}

	@Test
	@DisplayName("a distribuição por atributo é AGI 7, FOR 2, INT 9, PRE 9 e VIG 1")
	void distribuicaoPorAtributo() {
		Map<AtributoOrdem, Long> contagem = Arrays.stream(PericiaOrdem.values())
				.collect(Collectors.groupingBy(PericiaOrdem::getAtributo, Collectors.counting()));

		assertThat(contagem).containsEntry(AtributoOrdem.AGI, 7L).containsEntry(AtributoOrdem.FOR, 2L)
				.containsEntry(AtributoOrdem.INT, 9L).containsEntry(AtributoOrdem.PRE, 9L)
				.containsEntry(AtributoOrdem.VIG, 1L);
	}

	@Test
	@DisplayName("os nomes têm acento e não se repetem")
	void nomesComAcentoESemRepeticao() {
		assertThat(PericiaOrdem.INVESTIGACAO.getNome()).isEqualTo("Investigação");
		assertThat(PericiaOrdem.CIENCIAS.getNome()).isEqualTo("Ciências");
		assertThat(Arrays.stream(PericiaOrdem.values()).map(PericiaOrdem::getNome).collect(Collectors.toSet()))
				.hasSize(28);
	}

	@Test
	@DisplayName("os atributos têm código e nome")
	void atributosComNome() {
		assertThat(AtributoOrdem.AGI.getNome()).isEqualTo("Agilidade");
		assertThat(AtributoOrdem.VIG.getNome()).isEqualTo("Vigor");
		assertThat(PericiaOrdem.FORTITUDE.getAtributo()).isEqualTo(AtributoOrdem.VIG);
	}
}
