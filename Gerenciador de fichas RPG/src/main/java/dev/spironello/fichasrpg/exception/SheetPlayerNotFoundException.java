package dev.spironello.fichasrpg.exception;

/** Lançada pelo serviço quando não existe ficha com o id pedido. Vira 404 no handler de erros. */
public class SheetPlayerNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final Long id;

	public SheetPlayerNotFoundException(Long id) {
		super("Ficha não encontrada: id=" + id);
		this.id = id;
	}

	public Long getId() {
		return id;
	}
}
