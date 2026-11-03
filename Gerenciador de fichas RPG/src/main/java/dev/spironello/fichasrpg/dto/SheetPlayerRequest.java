package dev.spironello.fichasrpg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo de entrada do POST e do PUT. Não tem id: o id vem da URL (PUT) ou é gerado (POST).
 * Os nomes dos componentes são as próprias chaves do JSON, para não precisar de anotação.
 * "int" é palavra reservada do Java, por isso o componente se chama intel.
 *
 * O JSON de entrada é estrito: chave desconhecida dá 400. A única exceção é o "id", que o
 * Angular sempre envia e que aqui é aceito e IGNORADO (ADR 0007).
 *
 * Só nameJ, nameC e gender têm regra (iguais às colunas not null varchar(255)). Os números não
 * têm limites de domínio inventados (ADR 0008).
 */
@JsonIgnoreProperties("id")
public record SheetPlayerRequest(
		@NotBlank(message = "é obrigatório") @Size(max = 255, message = "deve ter no máximo 255 caracteres") String nameJ,
		@NotBlank(message = "é obrigatório") @Size(max = 255, message = "deve ter no máximo 255 caracteres") String nameC,
		@NotBlank(message = "é obrigatório") @Size(max = 255, message = "deve ter no máximo 255 caracteres") String gender,
		Integer age,
		Integer lifeA,
		Integer lifeM,
		Integer sanA,
		Integer sanM,
		Integer str,
		Integer dex,
		Integer cons,
		@JsonProperty("int") Integer intel,
		Integer car,
		Integer sab,
		Integer tam,
		Integer sor,
		Integer pod,
		Integer atribute1,
		Integer atribute2,
		Integer atribute3,
		Integer atribute4,
		Integer atribute5,
		Integer atribute6,
		Integer atribute7,
		Integer atribute8,
		Integer atribute9,
		Integer atribute10,
		Integer atribute11,
		Integer atribute12,
		Integer atribute13,
		Integer atribute14,
		Integer atribute15,
		Integer atribute16,
		Integer atribute17,
		Integer atribute18,
		Integer atribute19,
		Integer atribute20,
		Integer atribute21,
		Integer atribute22,
		Integer atribute23) {
}
