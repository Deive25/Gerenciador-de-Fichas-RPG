package dev.spironello.fichasrpg.dto;

import dev.spironello.fichasrpg.model.Pericia;
import jakarta.validation.constraints.NotNull;

/** Pedido de teste de perícia: o código do enum (ex.: "ACROBACIA"). */
public record TestePericiaRequest(@NotNull(message = "é obrigatório") Pericia pericia) {
}
