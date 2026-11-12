package dev.spironello.fichasrpg.dto;

import jakarta.validation.constraints.NotBlank;

/** Pedido da rolagem livre: a notação de dados (ex.: 2d6+1d4+3). Quem confere a notação é a NotacaoDeDados. */
public record RolagemRequest(@NotBlank(message = "é obrigatório") String expressao) {
}
