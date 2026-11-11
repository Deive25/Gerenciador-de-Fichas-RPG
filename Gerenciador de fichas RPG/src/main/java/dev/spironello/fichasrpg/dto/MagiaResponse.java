package dev.spironello.fichasrpg.dto;

/** Uma magia de D&D na resposta. Nível 0 é o truque; os textos opcionais saem nulos quando vazios. */
public record MagiaResponse(Long id, String nome, int nivel, String escola, String tempo, String alcance,
		String componentes, String duracao, boolean concentracao, boolean ritual, boolean preparada,
		String descricao) {
}
