package dev.spironello.fichasrpg.exception;

/**
 * Lançada quando um item de uma lista da ficha (habilidade, ataque, item...) não existe ou é de outra ficha.
 * Vira 404 "Item não encontrado" no handler de erros. Serve a todas as listas da ficha.
 */
public class ItemDaFichaNaoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final String lista;
	private final Long id;

	public ItemDaFichaNaoEncontradoException(String lista, Long id) {
		super("Item não encontrado: " + lista + " id=" + id);
		this.lista = lista;
		this.id = id;
	}

	public String getLista() {
		return lista;
	}

	public Long getId() {
		return id;
	}
}
