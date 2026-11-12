package dev.spironello.fichasrpg.dto;

/** Corpo opcional da rolagem de um ataque: o modo do d20 (só em D&D); modo nulo vale NORMAL. */
public record RolagemAtaqueRequest(ModoTeste modo) {
}
