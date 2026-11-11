package dev.spironello.fichasrpg.dto;

/** Um dado rolado: quantos lados tem e o valor que saiu (o 3D precisa saber o tipo de cada dado). */
public record DadoRolado(int lados, int valor) {
}
