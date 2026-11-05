package dev.spironello.fichasrpg.exception;

/** O PUT trouxe um sistema diferente do da ficha. Vira 409 no handler de erros. */
public class SistemaImutavelException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SistemaImutavelException() {
		super("O sistema de uma ficha não muda.");
	}
}
