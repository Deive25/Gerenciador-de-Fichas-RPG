package dev.spironello.fichasrpg.dto;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import dev.spironello.fichasrpg.model.AtributoDnd;
import dev.spironello.fichasrpg.model.EdicaoDnd;
import dev.spironello.fichasrpg.model.GrauDnd;
import dev.spironello.fichasrpg.model.PericiaDnd;
import dev.spironello.fichasrpg.model.RecursoDnd;
import dev.spironello.fichasrpg.model.Sistema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ficha de D&D (5e de 2014 ou 2024) enviada pelo cliente. Mesmas regras de entrada da ficha de Ordem: sem id
 * (se vier, é ignorado), JSON estrito e PUT que substitui tudo.
 *
 * O Bean Validation cuida do que se vê campo a campo (nível 1 a 20, atributo 1 a 30); o que cruza campos
 * (atributo faltando, PV temporário com máximo) fica em {@code ValidacaoFichaDnd}. Inspiração nula vira
 * false e salvaguardas nulas viram nenhuma. Perícia ausente é sem proficiência.
 */
@JsonIgnoreProperties("id")
public record FichaDndRequest(
		@NotBlank(message = "é obrigatório") @Size(max = 255, message = "deve ter no máximo 255 caracteres") String jogador,
		@NotBlank(message = "é obrigatório") @Size(max = 255, message = "deve ter no máximo 255 caracteres") String personagem,
		@NotBlank(message = "é obrigatório") @Size(max = 255, message = "deve ter no máximo 255 caracteres") String genero,
		Integer idade,
		@NotNull(message = "é obrigatório") EdicaoDnd edicao,
		@Size(max = 255, message = "deve ter no máximo 255 caracteres") String classe,
		@Size(max = 255, message = "deve ter no máximo 255 caracteres") String especie,
		@Size(max = 255, message = "deve ter no máximo 255 caracteres") String antecedente,
		@NotNull(message = "é obrigatório") @Min(value = 1, message = "deve estar entre 1 e 20") @Max(value = 20, message = "deve estar entre 1 e 20") Integer nivel,
		@NotNull(message = "é obrigatório") Map<AtributoDnd, @NotNull(message = "é obrigatório") @Min(value = 1, message = "deve estar entre 1 e 30") @Max(value = 30, message = "deve estar entre 1 e 30") Integer> atributos,
		Map<RecursoDnd, @Valid RecursoValor> recursos,
		@Min(value = 0, message = "deve ser 0 ou mais") Integer ca,
		Boolean inspiracao,
		Set<AtributoDnd> salvaguardas,
		Map<PericiaDnd, GrauDnd> pericias,
		AtributoDnd atributoConjuracao) implements FichaRequest {

	@Override
	public Sistema sistema() {
		return Sistema.DND;
	}
}
