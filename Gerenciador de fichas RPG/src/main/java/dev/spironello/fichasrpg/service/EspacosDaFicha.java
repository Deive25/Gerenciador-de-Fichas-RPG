package dev.spironello.fichasrpg.service;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.spironello.fichasrpg.dto.EspacoDnd;
import dev.spironello.fichasrpg.model.EspacoMagia;
import dev.spironello.fichasrpg.model.FichaDnd;

/** Monta os espaços de magia da resposta: sempre os nove níveis, na ordem. Chame dentro da transação (o mapa é lazy). */
final class EspacosDaFicha {

	private EspacosDaFicha() {
	}

	/** Nível sem linha no banco aparece como 0 de 0; assim o front não precisa tratar nível ausente. */
	static Map<Integer, EspacoDnd> de(FichaDnd ficha) {
		Map<Integer, EspacoDnd> espacos = new LinkedHashMap<>();
		for (int nivel = 1; nivel <= 9; nivel++) {
			EspacoMagia gravado = ficha.getEspacos().get(nivel);
			espacos.put(nivel, gravado == null ? new EspacoDnd(0, 0) : new EspacoDnd(gravado.total(), gravado.usados()));
		}
		return espacos;
	}
}
