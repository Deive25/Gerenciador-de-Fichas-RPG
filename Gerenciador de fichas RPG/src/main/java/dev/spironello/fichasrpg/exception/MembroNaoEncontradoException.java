package dev.spironello.fichasrpg.exception;

/** O usuário que o mestre quer remover (ou que sai) não participa da campanha. Vira 404. */
public class MembroNaoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MembroNaoEncontradoException(Long usuarioId) {
		super("Membro não encontrado: usuarioId=" + usuarioId);
	}
}
