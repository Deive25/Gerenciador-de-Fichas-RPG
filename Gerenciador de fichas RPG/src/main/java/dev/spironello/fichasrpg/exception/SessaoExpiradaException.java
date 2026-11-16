package dev.spironello.fichasrpg.exception;

/** Refresh token desconhecido, vencido, já usado ou revogado. Vira 401 "Sessão expirada. Entre de novo.". */
public class SessaoExpiradaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SessaoExpiradaException() {
		super("Sessão expirada. Entre de novo.");
	}
}
