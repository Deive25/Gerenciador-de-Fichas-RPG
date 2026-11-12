package dev.spironello.fichasrpg.service;

import java.util.List;

import dev.spironello.fichasrpg.dto.DadoRolado;

/** Resultado de rolar uma notação: os dados, o bônus (soma das constantes com sinal) e o total (nunca negativo). */
public record Rolagem(List<DadoRolado> dados, int bonus, int total) {
}
