package dev.spironello.fichasrpg.exception;

/** Cadastro com um e-mail que já tem conta. Vira 409 em errors.email (o ADR registra que isso revela a conta). */
public class EmailJaCadastradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public EmailJaCadastradoException() {
		super("E-mail já cadastrado");
	}
}
