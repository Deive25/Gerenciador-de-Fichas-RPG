package dev.spironello.fichasrpg.exception;

/** Login recusado. A mesma exceção (e a mesma mensagem) vale para e-mail inexistente, senha errada e conta sem senha. */
public class CredenciaisInvalidasException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CredenciaisInvalidasException() {
		super("E-mail ou senha incorretos");
	}
}
