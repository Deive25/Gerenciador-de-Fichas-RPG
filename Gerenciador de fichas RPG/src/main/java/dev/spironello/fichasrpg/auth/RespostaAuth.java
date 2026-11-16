package dev.spironello.fichasrpg.auth;

/** Par de tokens mais o usuário: corpo do registro, do login e do refresh. */
public record RespostaAuth(String accessToken, String refreshToken, UsuarioResponse usuario) {
}
