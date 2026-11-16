package dev.spironello.fichasrpg.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cadastro. As regras da senha ficam na PoliticaDeSenha (uma mensagem por regra, a verdade em code points). Os dois @Size
 * repetem só o tamanho, para que um pedido com vários campos inválidos já devolva todos os erros de uma vez. Nome e e-mail perdem os espaços das pontas já na leitura; o e-mail é gravado em minúsculas pelo serviço.
 */
public record RegistroRequest(
		@NotBlank(message = "é obrigatório") @Size(max = 100, message = "no máximo 100 caracteres") String nome,
		@NotBlank(message = "é obrigatório") @Email(message = "e-mail inválido") @Size(max = 255, message = "no máximo 255 caracteres") String email,
		@NotBlank(message = "é obrigatório") @Size(min = 15, message = "Use pelo menos 15 caracteres") @Size(max = 72, message = "A senha pode ter no máximo 72 caracteres") String senha) {

	public RegistroRequest {
		nome = nome == null ? null : nome.strip();
		email = email == null ? null : email.strip();
	}
}
