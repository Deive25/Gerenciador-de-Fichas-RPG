package dev.spironello.fichasrpg.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.model.Sistema;

/**
 * Registro dos sistemas: o Spring injeta todos os {@link SistemaDeFicha} e este componente os indexa pelo
 * {@link Sistema}. Dois componentes para o mesmo sistema é erro de configuração e falha já na subida.
 */
@Component
public class SistemasDeFicha {

	private final Map<Sistema, SistemaDeFicha> porSistema = new EnumMap<>(Sistema.class);

	public SistemasDeFicha(List<SistemaDeFicha> sistemas) {
		for (SistemaDeFicha sistema : sistemas) {
			if (porSistema.putIfAbsent(sistema.sistema(), sistema) != null) {
				throw new IllegalStateException("Sistema com mais de uma implementação: " + sistema.sistema());
			}
		}
	}

	public SistemaDeFicha de(Sistema sistema) {
		SistemaDeFicha encontrado = porSistema.get(sistema);
		if (encontrado == null) {
			throw new IllegalStateException("Sistema sem implementação: " + sistema);
		}
		return encontrado;
	}
}
