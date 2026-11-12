package dev.spironello.fichasrpg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import dev.spironello.fichasrpg.model.AtributoDnd;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST e do PUT de um ataque, igual para os dois sistemas. Os campos de um sistema só valem nele
 * (pericia, margem e multiplicador em Ordem; atributo e proficiente em D&D): quem confere é o serviço de cada
 * sistema, e também se o dano é uma notação de dados válida. Bônus, margem e multiplicador ausentes valem 0, 20 e 2,
 * e proficiente ausente vale false. O id é ignorado se vier no corpo.
 */
@JsonIgnoreProperties("id")
public record AtaqueRequest(
		@NotBlank(message = "é obrigatório") @Size(max = 255, message = "deve ter no máximo 255 caracteres") String nome,
		@Min(value = -99, message = "deve estar entre -99 e 99") @Max(value = 99, message = "deve estar entre -99 e 99") Integer bonus,
		@NotBlank(message = "é obrigatório") @Size(max = 60, message = "deve ter no máximo 60 caracteres") String dano,
		@Size(max = 255, message = "deve ter no máximo 255 caracteres") String tipoDano,
		@Size(max = 255, message = "deve ter no máximo 255 caracteres") String alcance,
		PericiaOrdem pericia,
		@Min(value = 2, message = "deve estar entre 2 e 20") @Max(value = 20, message = "deve estar entre 2 e 20") Integer margem,
		@Min(value = 2, message = "deve estar entre 2 e 5") @Max(value = 5, message = "deve estar entre 2 e 5") Integer multiplicador,
		AtributoDnd atributo,
		Boolean proficiente) {
}
