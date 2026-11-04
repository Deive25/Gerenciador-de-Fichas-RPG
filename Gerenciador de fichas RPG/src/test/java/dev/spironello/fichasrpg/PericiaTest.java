package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.spironello.fichasrpg.model.Pericia;

/** Fixa o mapeamento perícia -> chave legada do JSON (atributeN). Unitário: sem Spring e sem Docker. */
@DisplayName("Pericia (mapeamento para o JSON legado)")
class PericiaTest {

	@Test
	@DisplayName("são 23 perícias, com campos legados únicos, de atribute1 a atribute23")
	void camposLegadosCobremDe1a23() {
		assertThat(Pericia.values()).hasSize(23);
		assertThat(Arrays.stream(Pericia.values()).map(Pericia::getCampoLegado).collect(Collectors.toSet()))
				.containsExactlyInAnyOrderElementsOf(
						IntStream.rangeClosed(1, 23).mapToObj(i -> "atribute" + i).toList());
	}

	@Test
	@DisplayName("cada constante continua com o nome e o campo legado combinados com o formulário do Angular")
	void mapeamentoFixado() {
		String[][] esperado = {
				{"ACROBACIA", "Acrobacia", "atribute1"}, {"ATLETISMO", "Atletismo", "atribute2"},
				{"ADESTRAR_ANIMAIS", "Adestrar animais", "atribute3"}, {"ATUACAO_LABIA", "Atuação/Lábia", "atribute4"},
				{"CHARME", "Charme", "atribute5"}, {"ESQUIVA", "Esquiva", "atribute6"},
				{"FURTIVIDADE", "Furtividade", "atribute7"}, {"LUTA_CORPO_A_CORPO", "Luta corpo a corpo", "atribute8"},
				{"LUTA_ARMA_BRANCA", "Luta com arma branca", "atribute9"}, {"INTIMIDACAO", "Intimidação", "atribute10"},
				{"INTUICAO", "Intuição", "atribute11"}, {"INVESTIGACAO", "Investigação", "atribute12"},
				{"ARMAS_DE_FOGO_LEVES", "Armas de fogo leves", "atribute13"},
				{"ARMAS_DE_FOGO_MEDIAS", "Armas de fogo médias", "atribute14"},
				{"ARMAS_DE_FOGO_PESADAS", "Armas de fogo pesadas", "atribute15"},
				{"PERCEPCAO", "Percepção", "atribute16"}, {"USAR_BIBLIOTECAS", "Usar bibliotecas", "atribute17"},
				{"HISTORIA", "História", "atribute18"}, {"CURA", "Cura", "atribute19"},
				{"MALANDRAGEM", "Malandragem", "atribute20"}, {"OCULTISMO", "Ocultismo", "atribute21"},
				{"PSICOLOGIA", "Psicologia", "atribute22"}, {"ARREMESSAR", "Arremessar", "atribute23"}};
		for (String[] linha : esperado) {
			Pericia p = Pericia.valueOf(linha[0]);
			assertThat(p.getNome()).as(linha[0]).isEqualTo(linha[1]);
			assertThat(p.getCampoLegado()).as(linha[0]).isEqualTo(linha[2]);
		}
		assertThat(esperado.length).isEqualTo(23);
	}
}
