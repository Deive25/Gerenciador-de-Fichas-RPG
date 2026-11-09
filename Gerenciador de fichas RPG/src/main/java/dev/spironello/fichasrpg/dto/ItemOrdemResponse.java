package dev.spironello.fichasrpg.dto;

/** Um item de Ordem Paranormal: categoria de 0 a 4 e espaços por unidade. */
public record ItemOrdemResponse(Long id, String nome, int categoria, int espacos, int quantidade,
		String descricao) implements ItemResponse {
}
