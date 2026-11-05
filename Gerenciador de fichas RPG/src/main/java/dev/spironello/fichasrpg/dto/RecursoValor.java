package dev.spironello.fichasrpg.dto;

import jakarta.validation.constraints.Min;

/** Valor atual e máximo de um recurso (PV, SAN ou PE). Os dois são opcionais; quando vêm, não são negativos. */
public record RecursoValor(
		@Min(value = 0, message = "deve ser 0 ou mais") Integer atual,
		@Min(value = 0, message = "deve ser 0 ou mais") Integer maximo) {
}
