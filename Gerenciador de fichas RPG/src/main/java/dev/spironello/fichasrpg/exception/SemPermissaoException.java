package dev.spironello.fichasrpg.exception;

/** O usuário é membro da campanha mas o papel dele não permite a ação (ou a ficha é de outro jogador). Vira 403. */
public class SemPermissaoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SemPermissaoException() {
		super("Sem permissão");
	}
}
