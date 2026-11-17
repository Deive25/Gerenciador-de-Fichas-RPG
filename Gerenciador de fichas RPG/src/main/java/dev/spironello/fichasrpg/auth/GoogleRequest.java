package dev.spironello.fichasrpg.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo do POST /auth/google: o ID token que o botão do Google entregou ao navegador. */
public record GoogleRequest(
		@NotBlank(message = "é obrigatório") @Size(max = 4096, message = "no máximo 4096 caracteres") String idToken) {
}
