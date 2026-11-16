package dev.spironello.fichasrpg.campanha;

/** O que aceitar um convite fez: {@code novo} é true quando a pessoa entrou agora (201) e false quando já era membro (200). */
public record ResultadoDoAceite(Long campanhaId, boolean novo) {
}
