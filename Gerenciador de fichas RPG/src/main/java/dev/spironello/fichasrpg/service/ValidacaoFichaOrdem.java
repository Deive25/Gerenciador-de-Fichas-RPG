package dev.spironello.fichasrpg.service;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.spironello.fichasrpg.dto.FichaOrdemRequest;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.model.AtributoOrdem;
import dev.spironello.fichasrpg.model.GrauOrdem;
import dev.spironello.fichasrpg.model.PericiaOrdem;

/**
 * Regras de Ordem que cruzam campos e por isso não cabem no Bean Validation: os cinco atributos são
 * obrigatórios e cada grau de perícia exige um NEX mínimo (Veterano 35%, Expert 70%). Roda depois do
 * Bean Validation, então aqui o NEX e o mapa de atributos já existem e estão nos limites.
 * Lança {@link DadosInvalidosException} com todos os erros de uma vez (chave = campo, ex.: "pericias.LUTA").
 */
public final class ValidacaoFichaOrdem {

	private ValidacaoFichaOrdem() {
	}

	public static void validar(FichaOrdemRequest request) {
		Map<String, String> erros = new LinkedHashMap<>();
		for (AtributoOrdem atributo : AtributoOrdem.values()) {
			if (!request.atributos().containsKey(atributo)) {
				erros.put("atributos." + atributo.name(), "é obrigatório");
			}
		}
		if (request.pericias() != null) {
			for (Map.Entry<PericiaOrdem, GrauOrdem> entrada : request.pericias().entrySet()) {
				GrauOrdem grau = entrada.getValue();
				if (grau != null && request.nex() < grau.getNexMinimo()) {
					erros.put("pericias." + entrada.getKey().name(), nomeDoGrau(grau) + " exige NEX "
							+ grau.getNexMinimo() + "% ou mais");
				}
			}
		}
		if (!erros.isEmpty()) {
			throw new DadosInvalidosException(erros);
		}
	}

	private static String nomeDoGrau(GrauOrdem grau) {
		String nome = grau.name();
		return nome.charAt(0) + nome.substring(1).toLowerCase();
	}
}
