package dev.spironello.fichasrpg.dto;

import dev.spironello.fichasrpg.model.ElementoRitual;

/** Um ritual de Ordem Paranormal na resposta. Os textos opcionais saem nulos quando vazios. */
public record RitualResponse(Long id, String nome, int circulo, ElementoRitual elemento, String execucao,
		String alcance, String alvo, String duracao, String resistencia, String descricao) {
}
