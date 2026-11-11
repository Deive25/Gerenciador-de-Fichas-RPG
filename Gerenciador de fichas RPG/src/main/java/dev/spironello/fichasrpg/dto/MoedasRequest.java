package dev.spironello.fichasrpg.dto;

import jakarta.validation.constraints.Min;

/** Mudança parcial das moedas de D&D: um campo nulo ou ausente não muda a moeda. */
public record MoedasRequest(
		@Min(0) Integer pc,
		@Min(0) Integer pp,
		@Min(0) Integer pe,
		@Min(0) Integer po,
		@Min(0) Integer pl) {
}
