package dev.spironello.fichasrpg.exception;

/** Login com Google recusado (token inválido, e-mail não verificado, e-mail ligado a outra conta). Vira 401 com a mensagem. */
public class LoginGoogleRecusadoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public static final String TOKEN_INVALIDO = "Não foi possível entrar com o Google. Tente de novo.";
	public static final String EMAIL_NAO_VERIFICADO = "Confirme o seu e-mail no Google antes de entrar.";
	public static final String EMAIL_DE_OUTRA_CONTA = "Este e-mail já está ligado a outra conta do Google.";

	public LoginGoogleRecusadoException(String mensagem) {
		super(mensagem);
	}
}
