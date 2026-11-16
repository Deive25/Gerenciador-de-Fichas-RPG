package dev.spironello.fichasrpg.campanha;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo de POST e PUT /campanhas: nome (1 a 100, obrigatório) e descrição (opcional, até 1000). O construtor tira os
 * espaços das pontas e a descrição vazia vira null, antes de a validação olhar o tamanho. JSON estrito, como no resto.
 */
public record CampanhaRequest(
		@NotBlank(message = "é obrigatório") @Size(max = 100, message = "deve ter no máximo 100 caracteres") String nome,
		@Size(max = 1000, message = "deve ter no máximo 1000 caracteres") String descricao) {

	public CampanhaRequest {
		nome = nome == null ? null : nome.strip();
		descricao = descricao == null || descricao.isBlank() ? null : descricao.strip();
	}
}
