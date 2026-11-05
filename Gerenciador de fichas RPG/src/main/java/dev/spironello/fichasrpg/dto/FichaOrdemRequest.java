package dev.spironello.fichasrpg.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import dev.spironello.fichasrpg.model.AtributoOrdem;
import dev.spironello.fichasrpg.model.ClasseOrdem;
import dev.spironello.fichasrpg.model.GrauOrdem;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.model.RecursoOrdem;
import dev.spironello.fichasrpg.model.Sistema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ficha de Ordem Paranormal enviada pelo cliente. Não tem id: ele vem da URL (PUT) ou é gerado (POST);
 * se vier no corpo é aceito e ignorado. O resto do JSON é estrito (chave desconhecida dá 400).
 *
 * O Bean Validation cuida do que se vê campo a campo; o que cruza campos (atributo faltando, grau que
 * exige mais NEX) fica em {@code ValidacaoFichaOrdem}. Perícia ausente é destreinada. Recursos e
 * perícias omitidos no PUT são apagados: o PUT substitui a ficha inteira.
 */
@JsonIgnoreProperties("id")
public record FichaOrdemRequest(
		@NotBlank(message = "é obrigatório") @Size(max = 255, message = "deve ter no máximo 255 caracteres") String jogador,
		@NotBlank(message = "é obrigatório") @Size(max = 255, message = "deve ter no máximo 255 caracteres") String personagem,
		@NotBlank(message = "é obrigatório") @Size(max = 255, message = "deve ter no máximo 255 caracteres") String genero,
		Integer idade,
		ClasseOrdem classe,
		@Size(max = 255, message = "deve ter no máximo 255 caracteres") String origem,
		@NotNull(message = "é obrigatório") @Min(value = 5, message = "deve estar entre 5 e 99") @Max(value = 99, message = "deve estar entre 5 e 99") Integer nex,
		@NotNull(message = "é obrigatório") Map<AtributoOrdem, @NotNull(message = "é obrigatório") @Min(value = 0, message = "deve estar entre 0 e 5") @Max(value = 5, message = "deve estar entre 0 e 5") Integer> atributos,
		Map<RecursoOrdem, @Valid RecursoValor> recursos,
		Map<PericiaOrdem, GrauOrdem> pericias) implements FichaRequest {

	@Override
	public Sistema sistema() {
		return Sistema.ORDEM_PARANORMAL;
	}
}
