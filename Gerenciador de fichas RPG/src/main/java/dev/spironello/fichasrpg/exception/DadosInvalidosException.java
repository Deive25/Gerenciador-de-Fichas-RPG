package dev.spironello.fichasrpg.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Regra de validação que o Bean Validation não expressa (atributo faltando, grau que exige mais NEX).
 * Vira 400 "Dados inválidos" com a propriedade errors (campo -> mensagem), igual ao Bean Validation.
 */
public class DadosInvalidosException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final Map<String, String> erros;

	public DadosInvalidosException(Map<String, String> erros) {
		super("Dados inválidos nos campos " + erros.keySet());
		this.erros = Collections.unmodifiableMap(new LinkedHashMap<>(erros));
	}

	public Map<String, String> getErros() {
		return erros;
	}
}
