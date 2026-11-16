package dev.spironello.fichasrpg.exception;

/** Código de convite que não existe, foi trocado (renovado) ou está desativado. Vira 404. */
public class ConviteInvalidoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ConviteInvalidoException() {
		super("Convite inválido ou desativado");
	}
}
