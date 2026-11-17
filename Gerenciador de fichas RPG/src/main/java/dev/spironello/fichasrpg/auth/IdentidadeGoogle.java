package dev.spironello.fichasrpg.auth;

/** As claims do ID token que a API usa. {@code emailVerificado} só é true se a claim for o booleano true. */
public record IdentidadeGoogle(String sub, String email, boolean emailVerificado, String nome) {
}
