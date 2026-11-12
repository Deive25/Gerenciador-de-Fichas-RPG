package dev.spironello.fichasrpg.service;

import java.util.List;
import java.util.Map;

import dev.spironello.fichasrpg.dto.AtaqueDndResponse;
import dev.spironello.fichasrpg.dto.EspacoDnd;
import dev.spironello.fichasrpg.dto.ItemDndResponse;
import dev.spironello.fichasrpg.dto.MagiaResponse;

/** As listas da ficha de D&D que ficam fora das partes comuns: ataques, itens, magias e os espaços de magia (níveis 1 a 9). */
public record ListasDnd(List<AtaqueDndResponse> ataques, List<ItemDndResponse> itens, List<MagiaResponse> magias, Map<Integer, EspacoDnd> espacos) {
}
