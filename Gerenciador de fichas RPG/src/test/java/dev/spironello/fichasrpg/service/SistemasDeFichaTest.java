package dev.spironello.fichasrpg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.spironello.fichasrpg.model.Sistema;

/** Testes unitários do registro dos sistemas: sem Spring e sem Docker. */
@DisplayName("SistemasDeFicha (unitário)")
class SistemasDeFichaTest {

	@Test
	@DisplayName("de(ORDEM_PARANORMAL) devolve o componente de Ordem")
	void devolveOComponenteDoSistema() {
		SistemaOrdem ordem = new SistemaOrdem(new FichaOrdemMapper(), PartesComunsDeTeste.componente());
		SistemasDeFicha sistemas = new SistemasDeFicha(List.of(ordem));

		assertThat(sistemas.de(Sistema.ORDEM_PARANORMAL)).isSameAs(ordem);
	}

	@Test
	@DisplayName("dois componentes do mesmo sistema falham na construção")
	void sistemaDuplicado() {
		SistemaOrdem um = new SistemaOrdem(new FichaOrdemMapper(), PartesComunsDeTeste.componente());
		SistemaOrdem outro = new SistemaOrdem(new FichaOrdemMapper(), PartesComunsDeTeste.componente());

		assertThatThrownBy(() -> new SistemasDeFicha(List.of(um, outro)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("ORDEM_PARANORMAL");
	}

	@Test
	@DisplayName("sistema sem componente lança IllegalStateException")
	void sistemaSemImplementacao() {
		SistemasDeFicha sistemas = new SistemasDeFicha(List.of(new SistemaOrdem(new FichaOrdemMapper(), PartesComunsDeTeste.componente())));

		assertThatThrownBy(() -> sistemas.de(Sistema.DND))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Sistema sem implementação: DND");
	}
}
