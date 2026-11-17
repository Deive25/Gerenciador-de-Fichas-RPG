package dev.spironello.fichasrpg.comum;

/** Quantas contas demo e quantos refresh vencidos uma rodada da limpeza apagou (o que ela registra no log). */
public record ResultadoDaLimpeza(int contasDemoApagadas, int refreshVencidosApagados) {
}
