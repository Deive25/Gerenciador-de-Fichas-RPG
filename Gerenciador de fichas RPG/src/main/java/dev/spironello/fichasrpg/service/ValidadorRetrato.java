package dev.spironello.fichasrpg.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.exception.RetratoInvalidoException;

/**
 * Regras do arquivo de retrato (spec do redesign §4.3). O navegador já recorta e reduz a imagem,
 * mas o servidor não confia nele: só aceita JPEG de verdade (bytes iniciais FF D8 FF), que
 * decodifique e tenha no máximo 1000 px de lado. SVG e outros formatos nunca passam, porque podem
 * carregar script.
 */
@Component
public class ValidadorRetrato {

	/** Tamanho máximo do arquivo: 200 KB. O recorte do navegador gera em torno de 30–60 KB. */
	public static final int LIMITE_BYTES = 200 * 1024;

	/** Largura e altura máximas, em pixels. O recorte do navegador gera 400×500. */
	public static final int LADO_MAXIMO = 1000;

	public void validar(byte[] conteudo) {
		if (!comecaComoJpeg(conteudo)) {
			throw new RetratoInvalidoException("O arquivo não é um JPEG.");
		}
		BufferedImage imagem = ler(conteudo);
		if (imagem.getWidth() > LADO_MAXIMO || imagem.getHeight() > LADO_MAXIMO) {
			throw new RetratoInvalidoException("A imagem passa de 1000 px de largura ou altura.");
		}
	}

	private static boolean comecaComoJpeg(byte[] conteudo) {
		return conteudo.length >= 3
				&& conteudo[0] == (byte) 0xFF && conteudo[1] == (byte) 0xD8 && conteudo[2] == (byte) 0xFF;
	}

	private static BufferedImage ler(byte[] conteudo) {
		try {
			BufferedImage imagem = ImageIO.read(new ByteArrayInputStream(conteudo));
			if (imagem == null) {
				throw new RetratoInvalidoException("Não foi possível ler a imagem.");
			}
			return imagem;
		} catch (IOException | RuntimeException e) {
			if (e instanceof RetratoInvalidoException invalido) {
				throw invalido;
			}
			throw new RetratoInvalidoException("Não foi possível ler a imagem.");
		}
	}
}
