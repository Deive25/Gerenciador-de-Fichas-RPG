package dev.spironello.fichasrpg.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Corpo de saída da API: as mesmas 41 chaves JSON de sempre (contrato com o Angular).
 * "int" é palavra reservada do Java, por isso o componente se chama intel.
 */
public record SheetPlayerResponse(
		Long id,
		String nameJ,
		String nameC,
		String gender,
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
