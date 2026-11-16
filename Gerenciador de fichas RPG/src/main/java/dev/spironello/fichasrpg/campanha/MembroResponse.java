package dev.spironello.fichasrpg.campanha;

import java.time.Instant;

/** Um membro da campanha. Sem e-mail: o nome basta para a mesa (o e-mail é dado pessoal). */
public record MembroResponse(Long usuarioId, String nome, Papel papel, Instant entrouEm) {
}
