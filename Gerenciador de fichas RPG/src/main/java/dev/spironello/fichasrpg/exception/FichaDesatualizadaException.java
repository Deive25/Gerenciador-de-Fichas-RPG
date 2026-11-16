package dev.spironello.fichasrpg.exception;

/** O PUT trouxe uma versão da ficha diferente da do banco: alguém gravou antes (ADR 0025). Vira 409. */
public class FichaDesatualizadaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public FichaDesatualizadaException() {
		super("Ficha desatualizada");
	}
}
