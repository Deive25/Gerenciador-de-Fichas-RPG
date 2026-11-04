package dev.spironello.fichasrpg;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

/** Imagens geradas na hora para os testes do retrato (sem arquivos binários no repositório). */
public final class ImagensDeTeste {

	private ImagensDeTeste() {
	}

	public static byte[] jpeg(int largura, int altura) {
		return gerar(largura, altura, "jpg");
	}

	/**
	 * JPEG pequeno de verdade (8×8) com o cabeçalho SOF0 adulterado para dizer outra largura e altura.
	 * É a "bomba de decodificação": poucos bytes que, se decodificados, pediriam gigabytes de memória.
	 */
	public static byte[] jpegComDimensoesFalsas(int largura, int altura) {
		byte[] jpeg = jpeg(8, 8);
		for (int i = 2; i < jpeg.length - 9; i++) {
			if (jpeg[i] == (byte) 0xFF && jpeg[i + 1] == (byte) 0xC0) {
				// FF C0, tamanho (2), precisão (1), altura (2), largura (2)
				jpeg[i + 5] = (byte) (altura >> 8);
				jpeg[i + 6] = (byte) altura;
				jpeg[i + 7] = (byte) (largura >> 8);
				jpeg[i + 8] = (byte) largura;
				return jpeg;
			}
		}
		throw new IllegalStateException("SOF0 não encontrado no JPEG gerado");
	}

	public static byte[] png(int largura, int altura) {
		return gerar(largura, altura, "png");
	}

	private static byte[] gerar(int largura, int altura, String formato) {
		BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = imagem.createGraphics();
		g.setColor(new Color(0x8a6436));
		g.fillRect(0, 0, largura, altura);
		g.dispose();
		ByteArrayOutputStream saida = new ByteArrayOutputStream();
		try {
			ImageIO.write(imagem, formato, saida);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return saida.toByteArray();
	}
}
