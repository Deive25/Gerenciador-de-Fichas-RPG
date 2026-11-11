package dev.spironello.fichasrpg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST e do PUT de uma magia de D&D. Textos vazios viram nulo e os marcadores nulos viram false
 * (quem converte é o componente da lista). O id é ignorado se vier no corpo.
 */
@JsonIgnoreProperties("id")
public record MagiaRequest(
		@NotBlank(message = "é obrigatório") @Size(max = 255, message = "deve ter no máximo 255 caracteres") String nome,
		@NotNull(message = "é obrigatório") @Min(value = 0, message = "deve estar entre 0 e 9") @Max(value = 9, message = "deve estar entre 0 e 9") Integer nivel,
		@Size(max = 255, message = "deve ter no máximo 255 caracteres") String escola,
		@Size(max = 255, message = "deve ter no máximo 255 caracteres") String tempo,
		@Size(max = 255, message = "deve ter no máximo 255 caracteres") String alcance,
		@Size(max = 255, message = "deve ter no máximo 255 caracteres") String componentes,
		@Size(max = 255, message = "deve ter no máximo 255 caracteres") String duracao,
		Boolean concentracao,
		Boolean ritual,
		Boolean preparada,
		@Size(max = 4000, message = "deve ter no máximo 4000 caracteres") String descricao) {
}
