package dev.spironello.fichasrpg.exception;

/** Tentativa de tirar o mestre da campanha (sair ou ser removido): o mestre não sai, apaga a campanha. Vira 409. */
public class MestreNaoSaiException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MestreNaoSaiException() {
		super("O mestre não sai da campanha");
	}
}
