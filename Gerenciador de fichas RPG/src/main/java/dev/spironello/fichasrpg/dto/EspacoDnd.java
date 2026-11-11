package dev.spironello.fichasrpg.dto;

/** Os espaços de magia de um nível na resposta: quantos a ficha tem e quantos já usou. */
public record EspacoDnd(int total, int usados) {
}
