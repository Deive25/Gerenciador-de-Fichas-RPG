package dev.spironello.fichasrpg.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Login. O e-mail perde os espaços das pontas já na leitura; a caixa é tratada pelo serviço. */
public record LoginRequest(
		@NotBlank(message = "é obrigatório") @Size(max = 255, message = "no máximo 255 caracteres") String email,
		@NotBlank(message = "é obrigatório") @Size(max = 200, message = "no máximo 200 caracteres") String senha) {

	public LoginRequest {
		email = email == null ? null : email.strip();
	}
}
