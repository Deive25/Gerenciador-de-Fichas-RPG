package dev.spironello.fichasrpg.service;

import java.util.List;

import dev.spironello.fichasrpg.dto.AtaqueOrdemResponse;
import dev.spironello.fichasrpg.dto.ItemOrdemResponse;
import dev.spironello.fichasrpg.dto.RitualResponse;

/** As listas da ficha de Ordem Paranormal que ficam fora das partes comuns: ataques, itens e rituais. */
public record ListasOrdem(List<AtaqueOrdemResponse> ataques, List<ItemOrdemResponse> itens, List<RitualResponse> rituais) {
}
