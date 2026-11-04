package dev.spironello.fichasrpg.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import dev.spironello.fichasrpg.model.Pericia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo de entrada do POST e do PUT. Não tem id: o id vem da URL (PUT) ou é gerado (POST).
 * Os nomes dos componentes são as próprias chaves do JSON (contrato do incremento 1.6, ADR 0012),
 * por isso não precisa de anotação para os nomes.
 *
 * O JSON de entrada é estrito: chave desconhecida dá 400. A única exceção é o "id", que o
 * Angular sempre envia e que aqui é aceito e IGNORADO (ADR 0007).
 *
 * "pericias" é um objeto cujas chaves são os códigos do enum {@link Pericia} (ex.: "ACROBACIA");
 * código que não existe dá 400. Valor null equivale a perícia ausente (o Angular envia as 23 com
 * null nas vazias). Perícia omitida no PUT é removida: o PUT é substituição completa (ADR 0008).
 *
 * Só jogador, personagem e genero têm regra (iguais às colunas not null varchar(255)). Os números
 * não têm limites de domínio inventados (ADR 0008).
 */
@JsonIgnoreProperties("id")
public record SheetPlayerRequest(
		@NotBlank(message = "é obrigatório") @Size(max = 255, message = "deve ter no máximo 255 caracteres") String jogador,
		@NotBlank(message = "é obrigatório") @Size(max = 255, message = "deve ter no máximo 255 caracteres") String personagem,
		@NotBlank(message = "é obrigatório") @Size(max = 255, message = "deve ter no máximo 255 caracteres") String genero,
		Integer idade,
		Integer vidaAtual,
		Integer vidaMaxima,
		Integer sanidadeAtual,
		Integer sanidadeMaxima,
		Integer forca,
		Integer destreza,
		Integer constituicao,
		Integer inteligencia,
		Integer carisma,
		Integer sabedoria,
		Integer tamanho,
		Integer sorte,
		Integer poder,
		Map<Pericia, Integer> pericias) {
}
