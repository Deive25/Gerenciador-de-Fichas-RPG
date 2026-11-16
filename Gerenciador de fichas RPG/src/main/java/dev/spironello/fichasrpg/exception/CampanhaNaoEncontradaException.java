package dev.spironello.fichasrpg.exception;

/**
 * A campanha não existe ou quem pede não participa dela: para quem está fora ela não existe (404, nunca 403), e a API
 * não revela quais ids estão em uso. Vira 404.
 */
public class CampanhaNaoEncontradaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final Long id;

	public CampanhaNaoEncontradaException(Long id) {
		super("Campanha não encontrada: id=" + id);
		this.id = id;
	}

	public Long getId() {
		return id;
	}
}
