package dev.spironello.fichasrpg.service;

import java.util.List;

import dev.spironello.fichasrpg.dto.ItemOrdemResponse;

/** As listas da ficha de Ordem Paranormal que ficam fora das partes comuns (por ora, só os itens). */
public record ListasOrdem(List<ItemOrdemResponse> itens) {
}
