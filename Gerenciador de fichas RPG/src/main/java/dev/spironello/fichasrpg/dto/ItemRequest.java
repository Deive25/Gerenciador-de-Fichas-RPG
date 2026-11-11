package dev.spironello.fichasrpg.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST e do PUT de um item do inventário, igual para os dois sistemas. Os campos de um sistema só valem
 * nele (categoria e espaços em Ordem; peso e equipado em D&D): quem confere é o serviço de cada sistema.
 * O id é ignorado se vier no corpo.
 */
@JsonIgnoreProperties("id")
public record ItemRequest(
		@NotBlank @Size(max = 255) String nome,
		@NotNull @Min(1) Integer quantidade,
		@Size(max = 4000) String descricao,
		@Min(0) @Max(4) Integer categoria,
		@Min(0) Integer espacos,
		@DecimalMin("0") @Digits(integer = 5, fraction = 2) BigDecimal peso,
		Boolean equipado) {
}
