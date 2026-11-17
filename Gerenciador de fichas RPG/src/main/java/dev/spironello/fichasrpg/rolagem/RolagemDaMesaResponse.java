package dev.spironello.fichasrpg.rolagem;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonRawValue;

import dev.spironello.fichasrpg.dto.Referencia;

/**
 * A rolagem como a mesa a vê, no GET do histórico e no tópico (spec §5). {@code personagem} nulo e {@code doMestre}
 * verdadeiro andam juntos: é a ficha do mestre, e o nome do NPC nem chega aqui. Não há {@code fichaId}, para ninguém
 * descobrir o id do NPC. O {@code detalhe} já é JSON em texto e sai como objeto ({@code @JsonRawValue}).
 */
public record RolagemDaMesaResponse(Long id, Long campanhaId, Instant criadaEm, Referencia autor, String personagem,
		boolean doMestre, TipoRolagem tipo, String rotulo, int total, @JsonRawValue String detalhe) {

	public static RolagemDaMesaResponse de(RolagemDaMesa rolagem, Referencia autor) {
		return new RolagemDaMesaResponse(rolagem.getId(), rolagem.getCampanhaId(), rolagem.getCriadaEm(), autor,
				rolagem.getPersonagem(), rolagem.getPersonagem() == null, rolagem.getTipo(), rolagem.getRotulo(),
				rolagem.getTotal(), rolagem.getDetalhe());
	}
}
