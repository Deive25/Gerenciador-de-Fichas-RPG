package dev.spironello.fichasrpg.auth;

/** Resposta do POST /auth/demo: o par de tokens do login e a campanha para onde o visitante vai. */
public record RespostaDemo(String accessToken, String refreshToken, UsuarioResponse usuario, Long campanhaId) {

	static RespostaDemo de(RespostaAuth auth, Long campanhaId) {
		return new RespostaDemo(auth.accessToken(), auth.refreshToken(), auth.usuario(), campanhaId);
	}
}
