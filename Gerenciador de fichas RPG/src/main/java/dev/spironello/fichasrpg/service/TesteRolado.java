package dev.spironello.fichasrpg.service;

import java.util.List;

/** Resultado de um teste de Ordem: todos os dados rolados, o que ficou, o bônus do grau e o total. */
public record TesteRolado(List<Integer> dados, int mantido, int bonus, int total) {
}
