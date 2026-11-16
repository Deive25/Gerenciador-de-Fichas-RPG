package dev.spironello.fichasrpg.dto;

/** Corpo de PUT /fichas/{id}/campanha: a campanha de destino, ou null para tirar a ficha da campanha. */
public record TrocaDeCampanhaRequest(Long campanhaId) {
}
