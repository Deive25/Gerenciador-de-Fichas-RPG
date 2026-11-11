package dev.spironello.fichasrpg.dto;

/**
 * Um nível no PATCH dos espaços de magia. Sem anotações de propósito: o serviço confere tudo (obrigatório,
 * 0 ou mais, usados até o total) e junta os erros com a chave "nível.campo" ("3.usados").
 */
public record EspacoRequest(Integer total, Integer usados) {
}
