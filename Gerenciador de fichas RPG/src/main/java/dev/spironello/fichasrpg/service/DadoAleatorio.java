package dev.spironello.fichasrpg.service;

import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

/**
 * Implementação de produção do {@link Dado}. Usa o "Random" (java.base, thread-safe) de propósito: o gerador padrão do JDK 21
 * (L32X64MixRandom) mora no módulo jdk.random, que a imagem eclipse-temurin:21-jre do Docker não traz, e a API não subia.
 */
@Component
public class DadoAleatorio implements Dado {

	private final RandomGenerator gerador = RandomGenerator.of("Random");

	@Override
	public int rolar(int faces) {
		return gerador.nextInt(1, faces + 1); // limite superior exclusivo: 1..faces
	}
}
