package dev.spironello.fichasrpg.dto;

import java.math.BigDecimal;

/** Um item de D&D: peso de uma unidade, em kg, e se está equipado. */
public record ItemDndResponse(Long id, String nome, int quantidade, BigDecimal peso, boolean equipado,
		String descricao) implements ItemResponse {
}
