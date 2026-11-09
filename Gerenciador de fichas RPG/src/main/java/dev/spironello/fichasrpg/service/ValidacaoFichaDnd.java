package dev.spironello.fichasrpg.service;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.spironello.fichasrpg.dto.FichaDndRequest;
import dev.spironello.fichasrpg.dto.RecursoValor;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.model.AtributoDnd;
import dev.spironello.fichasrpg.model.RecursoDnd;

/**
 * Regras de D&D que cruzam campos e por isso não cabem no Bean Validation: os seis atributos são
 * obrigatórios e o PV temporário não tem máximo. Roda depois do Bean Validation. Lança
 * {@link DadosInvalidosException} com todos os erros de uma vez (chave = campo, ex.: "atributos.CON").
 */
public final class ValidacaoFichaDnd {

	private ValidacaoFichaDnd() {
	}

	public static void validar(FichaDndRequest request) {
		Map<String, String> erros = new LinkedHashMap<>();
		for (AtributoDnd atributo : AtributoDnd.values()) {
			if (!request.atributos().containsKey(atributo)) {
				erros.put("atributos." + atributo.name(), "é obrigatório");
			}
		}
		RecursoValor pvTemporario = request.recursos() == null ? null : request.recursos().get(RecursoDnd.PV_TEMP);
		if (pvTemporario != null && pvTemporario.maximo() != null) {
			erros.put("recursos.PV_TEMP.maximo", "PV temporário não tem máximo");
		}
		if (!erros.isEmpty()) {
			throw new DadosInvalidosException(erros);
		}
	}
}
