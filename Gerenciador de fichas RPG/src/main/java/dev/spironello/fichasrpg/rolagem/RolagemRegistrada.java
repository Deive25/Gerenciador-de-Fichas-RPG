package dev.spironello.fichasrpg.rolagem;

/**
 * Evento de domínio: uma rolagem da mesa acabou de ser gravada. O {@link PublicadorDeRolagens} só o entrega ao
 * tópico depois do commit da transação que gravou.
 */
public record RolagemRegistrada(RolagemDaMesaResponse rolagem) {
}
