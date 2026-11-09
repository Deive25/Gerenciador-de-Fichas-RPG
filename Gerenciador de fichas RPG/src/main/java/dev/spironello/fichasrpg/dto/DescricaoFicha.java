package dev.spironello.fichasrpg.dto;

import jakarta.validation.constraints.Size;

/**
 * Descrição do personagem (aba Descrição): aparência, história e anotações, todas opcionais, até 4000
 * caracteres. Serve de pedido (PUT /fichas/{id}/descricao) e de resposta (dentro da ficha completa).
 */
public record DescricaoFicha(
		@Size(max = 4000) String aparencia,
		@Size(max = 4000) String historia,
		@Size(max = 4000) String anotacoes) {
}
