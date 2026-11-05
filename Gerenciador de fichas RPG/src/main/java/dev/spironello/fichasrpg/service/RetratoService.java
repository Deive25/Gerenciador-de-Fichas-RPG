package dev.spironello.fichasrpg.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.exception.FichaNaoEncontradaException;
import dev.spironello.fichasrpg.exception.RetratoGrandeDemaisException;
import dev.spironello.fichasrpg.exception.RetratoNaoEncontradoException;
import dev.spironello.fichasrpg.model.RetratoFicha;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.repository.RetratoFichaRepository;

/**
 * Retrato do personagem (spec do redesign §4). Um retrato por ficha: enviar de novo substitui.
 * A "versão" que o JSON da ficha expõe é o atualizadoEm em milissegundos, e nunca se repete:
 * se o relógio não andou desde o último envio, a versão nova é a anterior + 1 ms.
 */
@Service
public class RetratoService {

	private static final Logger log = LoggerFactory.getLogger(RetratoService.class);

	private final RetratoFichaRepository retratos;
	private final FichaRepository fichas;
	private final ValidadorRetrato validador;
	private final Clock relogio;

	public RetratoService(RetratoFichaRepository retratos, FichaRepository fichas, ValidadorRetrato validador,
			Clock relogio) {
		this.retratos = retratos;
		this.fichas = fichas;
		this.validador = validador;
		this.relogio = relogio;
	}

	/**
	 * Lê no máximo LIMITE_BYTES + 1 bytes do corpo: um corpo enorme nunca vai inteiro para a memória.
	 * O tamanho declarado (Content-Length, ou -1 se ausente) permite recusar antes de ler.
	 */
	@Transactional
	public void enviar(Long fichaId, InputStream corpo, long tamanhoDeclarado) {
		exigirFicha(fichaId);
		if (tamanhoDeclarado > ValidadorRetrato.LIMITE_BYTES) {
			throw new RetratoGrandeDemaisException();
		}
		byte[] conteudo = lerComLimite(corpo);
		validador.validar(conteudo);

		Instant versao = relogio.instant().truncatedTo(ChronoUnit.MILLIS);
		Instant anterior = retratos.findById(fichaId).map(RetratoFicha::getAtualizadoEm).orElse(null);
		if (anterior != null && !versao.isAfter(anterior)) {
			versao = anterior.plusMillis(1);
		}
		retratos.save(new RetratoFicha(fichaId, conteudo, versao));
		log.info("Retrato salvo fichaId={} bytes={}", fichaId, conteudo.length);
	}

	@Transactional(readOnly = true)
	public RetratoFicha buscar(Long fichaId) {
		exigirFicha(fichaId);
		return retratos.findById(fichaId).orElseThrow(() -> new RetratoNaoEncontradoException(fichaId));
	}

	@Transactional
	public void remover(Long fichaId) {
		exigirFicha(fichaId);
		if (retratos.existsById(fichaId)) {
			retratos.deleteById(fichaId);
			log.info("Retrato removido fichaId={}", fichaId);
		}
	}

	private void exigirFicha(Long fichaId) {
		if (!fichas.existsById(fichaId)) {
			throw new FichaNaoEncontradaException(fichaId);
		}
	}

	private static byte[] lerComLimite(InputStream corpo) {
		try {
			byte[] conteudo = corpo.readNBytes(ValidadorRetrato.LIMITE_BYTES + 1);
			if (conteudo.length > ValidadorRetrato.LIMITE_BYTES) {
				throw new RetratoGrandeDemaisException();
			}
			return conteudo;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
