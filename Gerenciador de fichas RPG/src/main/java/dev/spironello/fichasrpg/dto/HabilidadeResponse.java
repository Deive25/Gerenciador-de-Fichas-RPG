package dev.spironello.fichasrpg.dto;

import dev.spironello.fichasrpg.model.TipoHabilidade;

/** Uma habilidade da ficha, como aparece na lista da aba Habilidades. */
public record HabilidadeResponse(Long id, String nome, TipoHabilidade tipo, String descricao) {
}
