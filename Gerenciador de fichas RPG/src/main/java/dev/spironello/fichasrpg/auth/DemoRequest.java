package dev.spironello.fichasrpg.auth;

/** Corpo opcional do POST /auth/demo: o código de convite de uma mesa demo, para entrar nela como jogador. */
public record DemoRequest(String codigoConvite) {
}
