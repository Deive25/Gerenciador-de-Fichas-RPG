package dev.spironello.fichasrpg.exception;

/** O corpo enviado como retrato passa de 200 KB. Vira 413 no handler de erros. */
public class RetratoGrandeDemaisException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RetratoGrandeDemaisException() {
		super("Retrato acima do limite de tamanho");
	}
}
