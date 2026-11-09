package dev.spironello.fichasrpg.service;

import java.util.List;

import dev.spironello.fichasrpg.dto.ItemDndResponse;

/** As listas da ficha de D&D que ficam fora das partes comuns (por ora, só os itens). */
public record ListasDnd(List<ItemDndResponse> itens) {
}
