package dev.spironello.fichasrpg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.spironello.fichasrpg.ImagensDeTeste;
import dev.spironello.fichasrpg.exception.RetratoInvalidoException;

/**
 * O servidor não confia no navegador (spec do redesign §4.3): confere os bytes iniciais do JPEG,
 * decodifica a imagem e limita o tamanho em pixels. Sem Spring e sem Docker.
 */
@DisplayName("ValidadorRetrato (unitário)")
class ValidadorRetratoTest {

	private final ValidadorRetrato validador = new ValidadorRetrato();

	@Test
	@DisplayName("aceita um JPEG de 400×500")
	void aceitaJpegValido() {
		assertThatCode(() -> validador.validar(ImagensDeTeste.jpeg(400, 500))).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("aceita exatamente 1000 px de lado")
	void aceitaNoLimite() {
		assertThatCode(() -> validador.validar(ImagensDeTeste.jpeg(1000, 1000))).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("recusa imagem com mais de 1000 px de largura ou de altura")
	void recusaImagemGrande() {
		assertThatThrownBy(() -> validador.validar(ImagensDeTeste.jpeg(1001, 10)))
				.isInstanceOf(RetratoInvalidoException.class)
				.hasMessage("A imagem passa de 1000 px de largura ou altura.");
		assertThatThrownBy(() -> validador.validar(ImagensDeTeste.jpeg(10, 1001)))
				.isInstanceOf(RetratoInvalidoException.class)
				.hasMessage("A imagem passa de 1000 px de largura ou altura.");
	}

	@Test
	@DisplayName("recusa pelo cabeçalho, sem decodificar, um JPEG minúsculo que diz ter 50000×50000 (bomba de decodificação)")
	void recusaBombaDeDecodificacao() {
		byte[] bomba = ImagensDeTeste.jpegComDimensoesFalsas(50_000, 50_000);
		assertThat(bomba.length).isLessThan(2_000);

		// Decodificar pediria 50000 × 50000 × 3 bytes ≈ 7,5 GB: o teste só passa se a conferência vier antes.
		assertThatThrownBy(() -> validador.validar(bomba))
				.isInstanceOf(RetratoInvalidoException.class)
				.hasMessage("A imagem passa de 1000 px de largura ou altura.");
	}

	@Test
	@DisplayName("recusa PNG mesmo que alguém o envie como JPEG")
	void recusaPng() {
		assertThatThrownBy(() -> validador.validar(ImagensDeTeste.png(100, 100)))
				.isInstanceOf(RetratoInvalidoException.class)
				.hasMessage("O arquivo não é um JPEG.");
	}

	@Test
	@DisplayName("recusa arquivo vazio")
	void recusaVazio() {
		assertThatThrownBy(() -> validador.validar(new byte[0]))
				.isInstanceOf(RetratoInvalidoException.class)
				.hasMessage("O arquivo não é um JPEG.");
	}

	@Test
	@DisplayName("recusa bytes que começam como JPEG mas não são uma imagem")
	void recusaJpegCorrompido() {
		byte[] lixo = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3, 4, 5, 6, 7 };
		assertThatThrownBy(() -> validador.validar(lixo))
				.isInstanceOf(RetratoInvalidoException.class)
				.hasMessage("Não foi possível ler a imagem.");
	}
}
