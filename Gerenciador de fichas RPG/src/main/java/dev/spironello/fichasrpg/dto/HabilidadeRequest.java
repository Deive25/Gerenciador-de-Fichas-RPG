package dev.spironello.fichasrpg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import dev.spironello.fichasrpg.model.TipoHabilidade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST e do PUT de uma habilidade da ficha. O tipo precisa existir no sistema da ficha
 * (conferido no serviço). O id é ignorado se vier no corpo.
 */
@JsonIgnoreProperties("id")
public record HabilidadeRequest(
		@NotBlank @Size(max = 255) String nome,
		@NotNull TipoHabilidade tipo,
		@Size(max = 4000) String descricao) {
}
