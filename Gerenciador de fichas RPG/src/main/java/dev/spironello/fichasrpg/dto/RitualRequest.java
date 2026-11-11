package dev.spironello.fichasrpg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import dev.spironello.fichasrpg.model.ElementoRitual;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST e do PUT de um ritual de Ordem Paranormal. Textos vazios viram nulo (quem converte é o
 * componente da lista). O id é ignorado se vier no corpo.
 */
@JsonIgnoreProperties("id")
public record RitualRequest(
		@NotBlank(message = "é obrigatório") @Size(max = 255, message = "deve ter no máximo 255 caracteres") String nome,
		@NotNull(message = "é obrigatório") @Min(value = 1, message = "deve estar entre 1 e 4") @Max(value = 4, message = "deve estar entre 1 e 4") Integer circulo,
		@NotNull(message = "é obrigatório") ElementoRitual elemento,
		@Size(max = 255, message = "deve ter no máximo 255 caracteres") String execucao,
		@Size(max = 255, message = "deve ter no máximo 255 caracteres") String alcance,
		@Size(max = 255, message = "deve ter no máximo 255 caracteres") String alvo,
		@Size(max = 255, message = "deve ter no máximo 255 caracteres") String duracao,
		@Size(max = 255, message = "deve ter no máximo 255 caracteres") String resistencia,
		@Size(max = 4000, message = "deve ter no máximo 4000 caracteres") String descricao) {
}
