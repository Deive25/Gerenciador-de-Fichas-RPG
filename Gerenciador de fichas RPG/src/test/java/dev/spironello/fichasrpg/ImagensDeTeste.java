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
