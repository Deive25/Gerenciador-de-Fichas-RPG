package dev.spironello.fichasrpg.exception;

/** A ficha existe, mas não tem retrato. Vira 404 "Retrato não encontrado" no handler de erros. */
public class RetratoNaoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final Long fichaId;

	public RetratoNaoEncontradoException(Long fichaId) {
		super("Retrato não encontrado: fichaId=" + fichaId);
		this.fichaId = fichaId;
	}

	public Long getFichaId() {
		return fichaId;
	}
}
