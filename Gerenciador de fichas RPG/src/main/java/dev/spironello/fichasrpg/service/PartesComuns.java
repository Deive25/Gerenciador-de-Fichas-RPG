package dev.spironello.fichasrpg.service;

import java.util.List;

import dev.spironello.fichasrpg.dto.DescricaoFicha;
import dev.spironello.fichasrpg.dto.HabilidadeResponse;

/** O que a ficha completa tem igual nos dois sistemas: a descrição e as habilidades. */
public record PartesComuns(DescricaoFicha descricao, List<HabilidadeResponse> habilidades) {
}
