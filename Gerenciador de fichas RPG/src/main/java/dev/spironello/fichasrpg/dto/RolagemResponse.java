package dev.spironello.fichasrpg.dto;

import java.util.List;

/**
 * Uma notação rolada: a expressão canônica, cada dado (lados e valor), o bônus (soma das constantes com sinal) e o
 * total (nunca negativo). Serve à rolagem livre e ao dano de um ataque.
 */
public record RolagemResponse(String expressao, List<DadoRolado> dados, int bonus, int total) {
}
