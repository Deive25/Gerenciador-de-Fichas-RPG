package dev.spironello.fichasrpg.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TipoHabilidade")
class TipoHabilidadeTest {

	@Test
	@DisplayName("TRILHA vale só em Ordem Paranormal")
	void trilha() {
		assertThat(TipoHabilidade.TRILHA.permitidoEm(Sistema.ORDEM_PARANORMAL)).isTrue();
		assertThat(TipoHabilidade.TRILHA.permitidoEm(Sistema.DND)).isFalse();
	}

	@Test
	@DisplayName("ESPECIE vale só em D&D")
	void especie() {
		assertThat(TipoHabilidade.ESPECIE.permitidoEm(Sistema.DND)).isTrue();
		assertThat(TipoHabilidade.ESPECIE.permitidoEm(Sistema.ORDEM_PARANORMAL)).isFalse();
	}

	@Test
	@DisplayName("CLASSE e OUTRA valem nos dois sistemas")
	void comuns() {
		for (Sistema sistema : Sistema.values()) {
			assertThat(TipoHabilidade.CLASSE.permitidoEm(sistema)).isTrue();
			assertThat(TipoHabilidade.OUTRA.permitidoEm(sistema)).isTrue();
		}
	}

	@Test
	@DisplayName("Ordem tem 5 tipos e D&D tem 6")
	void quantidades() {
		assertThat(Arrays.stream(TipoHabilidade.values()).filter(t -> t.permitidoEm(Sistema.ORDEM_PARANORMAL)))
				.containsExactly(TipoHabilidade.CLASSE, TipoHabilidade.TRILHA, TipoHabilidade.ORIGEM,
						TipoHabilidade.PARANORMAL, TipoHabilidade.OUTRA);
		assertThat(Arrays.stream(TipoHabilidade.values()).filter(t -> t.permitidoEm(Sistema.DND)))
				.containsExactly(TipoHabilidade.CLASSE, TipoHabilidade.SUBCLASSE, TipoHabilidade.ESPECIE,
						TipoHabilidade.ANTECEDENTE, TipoHabilidade.TALENTO, TipoHabilidade.OUTRA);
	}
}
