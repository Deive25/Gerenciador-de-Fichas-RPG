package dev.spironello.fichasrpg.campanha;

/** O convite da campanha: o código (os 12 caracteres do link) e se está ativo. Só o mestre o vê. */
public record ConviteResponse(String codigo, boolean ativo) {
}
