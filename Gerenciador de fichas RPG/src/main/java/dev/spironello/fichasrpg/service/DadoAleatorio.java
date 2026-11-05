package dev.spironello.fichasrpg.service;

import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

/** Implementação de produção do {@link Dado}, com o gerador padrão do JDK (thread-safe). */
@Component
public class DadoAleatorio implements Dado {

	private final RandomGenerator gerador = RandomGenerator.getDefault();

	@Override
	public int rolar(int faces) {
		return gerador.nextInt(1, faces + 1); // limite superior exclusivo: 1..faces
	}
}
