package dev.spironello.fichasrpg.exception;

/**
 * O arquivo enviado como retrato não é um JPEG válido dentro dos limites. Vira 422 no handler de
 * erros, com a mensagem (já em português, sem dado pessoal) como detalhe.
 */
public class RetratoInvalidoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RetratoInvalidoException(String motivo) {
		super(motivo);
	}
}
