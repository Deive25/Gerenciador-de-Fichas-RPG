package dev.spironello.fichasrpg.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cadastro. Nome e e-mail perdem os espaços das pontas já na leitura; o e-mail é gravado em minúsculas pelo serviço. */
public record RegistroRequest(
		@NotBlank(message = "é obrigatório") @Size(max = 100, message = "no máximo 100 caracteres") String nome,
		@NotBlank(message = "é obrigatório") @Email(message = "e-mail inválido") @Size(max = 255, message = "no máximo 255 caracteres") String email,
		@NotBlank(message = "é obrigatório") @Size(min = 8, max = 72, message = "deve ter de 8 a 72 caracteres") String senha) {

	public RegistroRequest {
		nome = nome == null ? null : nome.strip();
		email = email == null ? null : email.strip();
	}
}
