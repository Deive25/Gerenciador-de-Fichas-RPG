package dev.spironello.fichasrpg.dto;

/** Um recurso no resumo da ficha: código (ex.: "PV"), valor atual e máximo (nulos quando não preenchidos). */
public record RecursoResumo(String codigo, Integer atual, Integer maximo) {
}
