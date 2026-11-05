package dev.spironello.fichasrpg.dto;

/** Ficha completa devolvida pela API; o campo "sistema" do JSON diz qual é o tipo. */
public sealed interface FichaResponse permits FichaOrdemResponse {

	Long id();
}
