package dev.spironello.fichasrpg.auth;

public record UsuarioResponse(Long id, String nome, String email) {

	static UsuarioResponse de(Usuario usuario) {
		return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
	}
}
