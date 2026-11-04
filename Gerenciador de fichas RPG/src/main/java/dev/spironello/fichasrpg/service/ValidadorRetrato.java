package dev.spironello.fichasrpg.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.exception.RetratoInvalidoException;

/**
 * Regras do arquivo de retrato (spec do redesign §4.3). O navegador já recorta e reduz a imagem,
 * mas o servidor não confia nele: só aceita JPEG de verdade (bytes iniciais FF D8 FF), que
 * decodifique e tenha no máximo 1000 px de lado. SVG e outros formatos nunca passam, porque podem
 * carregar script.
 *
 * A largura e a altura são lidas só do CABEÇALHO, antes de decodificar: um JPEG de poucos bytes
 * pode dizer que tem 50000×50000 ("bomba de decodificação") e, decodificado, pediria gigabytes de
 * memória. O limite de 200 KB não protege contra isso; a conferência do cabeçalho, sim.
 */
@Component
public class ValidadorRetrato {

	/** Tamanho máximo do arquivo: 200 KB. O recorte do navegador gera em torno de 10–60 KB. */
	public static final int LIMITE_BYTES = 200 * 1024;

	/** Largura e altura máximas, em pixels. O recorte do navegador gera 400×500. */
	public static final int LADO_MAXIMO = 1000;

	private static final String ILEGIVEL = "Não foi possível ler a imagem.";

	public void validar(byte[] conteudo) {
		if (!comecaComoJpeg(conteudo)) {
			throw new RetratoInvalidoException("O arquivo não é um JPEG.");
		}
		try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(conteudo))) {
			Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);
			if (!leitores.hasNext()) {
				throw new RetratoInvalidoException(ILEGIVEL);
			}
			ImageReader leitor = leitores.next();
			try {
				leitor.setInput(entrada, true, true);
				// 1º o cabeçalho (barato), 2º a decodificação (cara), só se o tamanho couber.
				if (leitor.getWidth(0) > LADO_MAXIMO || leitor.getHeight(0) > LADO_MAXIMO) {
					throw new RetratoInvalidoException("A imagem passa de 1000 px de largura ou altura.");
				}
				leitor.read(0);
			} finally {
				leitor.dispose();
			}
		} catch (RetratoInvalidoException e) {
			throw e;
		} catch (IOException | RuntimeException e) {
			throw new RetratoInvalidoException(ILEGIVEL);
		}
	}

	private static boolean comecaComoJpeg(byte[] conteudo) {
		return conteudo.length >= 3
				&& conteudo[0] == (byte) 0xFF && conteudo[1] == (byte) 0xD8 && conteudo[2] == (byte) 0xFF;
	}
}
