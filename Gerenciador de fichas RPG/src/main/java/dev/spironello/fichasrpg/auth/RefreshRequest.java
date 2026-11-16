package dev.spironello.fichasrpg.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo do refresh e do logout. */
public record RefreshRequest(
		@NotBlank(message = "é obrigatório") @Size(max = 200, message = "no máximo 200 caracteres") String refreshToken) {
}
