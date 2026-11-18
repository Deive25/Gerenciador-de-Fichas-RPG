package dev.spironello.fichasrpg.auth;

/** O usuário nas respostas. {@code demo} diz se a conta é a de demonstração (temporária, do POST /auth/demo). */
public record UsuarioResponse(Long id, String nome, String email, boolean demo) {

	static UsuarioResponse de(Usuario usuario) {
		return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.isDemo());
	}
}
