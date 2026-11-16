package dev.spironello.fichasrpg.campanha;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CodigoDeConvite (unitário)")
class CodigoDeConviteTest {

	@Test
	@DisplayName("tem 12 caracteres, só de A-Z, a-z e 0-9")
	void tamanhoEAlfabeto() {
		for (int i = 0; i < 2000; i++) {
			assertThat(CodigoDeConvite.gerar()).hasSize(12).matches("[A-Za-z0-9]{12}");
		}
	}

	@Test
	@DisplayName("usa o alfabeto todo (62 símbolos) e não repete em mil códigos")
	void alfabetoCompletoESemRepeticao() {
		Set<Character> vistos = new HashSet<>();
		Set<String> codigos = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			String codigo = CodigoDeConvite.gerar();
			codigos.add(codigo);
			codigo.chars().forEach(c -> vistos.add((char) c));
		}
		assertThat(codigos).hasSize(1000);
		assertThat(vistos).hasSize(62);
	}
}
