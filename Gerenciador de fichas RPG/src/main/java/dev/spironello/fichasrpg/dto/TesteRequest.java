package dev.spironello.fichasrpg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Pedido de rolagem: o que rolar (alvo e código) e o modo; modo nulo vale NORMAL. */
public record TesteRequest(@NotNull AlvoTeste alvo, @NotBlank String codigo, ModoTeste modo) {
}
