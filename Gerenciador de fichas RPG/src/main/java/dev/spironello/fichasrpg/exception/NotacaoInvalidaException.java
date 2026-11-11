package dev.spironello.fichasrpg.exception;

/**
 * A notação de dados (ex.: 2d6+3) está fora da gramática ou dos limites. A mensagem já é a que a pessoa lê
 * (em português, por caso); quem chama a põe no campo certo (errors.dano, errors.expressao).
 */
public class NotacaoInvalidaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NotacaoInvalidaException(String mensagem) {
		super(mensagem);
	}
}
