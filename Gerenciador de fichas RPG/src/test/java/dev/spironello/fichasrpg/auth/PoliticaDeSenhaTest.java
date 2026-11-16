package dev.spironello.fichasrpg.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Unitário, sem Spring: a política de senha do cadastro (NIST SP 800-63B-4 e OWASP ASVS 5.0). */
@DisplayName("Política de senha")
class PoliticaDeSenhaTest {

	private static final String CURTA = "Use pelo menos 15 caracteres";
	private static final String REPETIDO = "Não repita o mesmo trecho";
	private static final String SEQUENCIA = "Não use sequências como 123456 ou qwerty";
	private static final String NOME_EMAIL = "Não use o seu nome nem o seu e-mail";
	private static final String COMUM = "Essa senha é comum demais. Escolha outra";
	private static final String LONGA = "A senha pode ter no máximo 72 caracteres";

	private final PoliticaDeSenha politica = new PoliticaDeSenha(
			Set.of("password", "senha", "correcthorsebatterystaple", "dungeonsanddragons"));

	private Optional<String> problema(String senha) {
		return politica.problema(senha, "Ana Souza", "ana@x.com");
	}

	@Test
	@DisplayName("regra 1: 14 caracteres é recusado, 15 é aceito; espaços contam; conta code points (emoji = 1)")
	void tamanho() {
		assertThat(problema("dragao na taver")).isEmpty(); // 15, com espaços
		assertThat(problema("dragao na tave")).contains(CURTA); // 14
		// 14 emojis são 28 chars UTF-16 mas só 14 code points
		assertThat(problema("🐉".repeat(14))).contains(CURTA);
	}

	@Test
	@DisplayName("regra 2: mais de 72 bytes em UTF-8 é recusado (37 'ç' = 74 bytes) e 72 bytes passa")
	void bytes() {
		assertThat(problema("ç".repeat(37))).contains(LONGA);
		assertThat(problema("dragao vermelho dorme na taverna do velho mestre perto da ponte antiga e")).isEmpty();
	}

	@Test
	@DisplayName("regra 3: o mesmo trecho repetido 2 vezes ou mais é recusado")
	void trechoRepetido() {
		assertThat(problema("aaaaaaaaaaaaaaa")).contains(REPETIDO);
		assertThat(problema("senhasenhasenha")).contains(REPETIDO);
		assertThat(problema("abcabcabcabcabc")).contains(REPETIDO);
		assertThat(problema("Rolar Dados Rolar Dados ")).contains(REPETIDO); // em minúsculas, 2 vezes
		assertThat(problema("dragao e dragao e mais")).isEmpty(); // repete, mas não é um período do texto todo
	}

	@Test
	@DisplayName("regra 4: sequências (numérica, alfabética, teclado) e suas inversas, sem espaços")
	void sequencias() {
		assertThat(problema("123456789012345")).contains(SEQUENCIA);
		assertThat(problema("qwertyuiopasdfg")).contains(SEQUENCIA);
		assertThat(problema("zyxwvutsrqponml")).contains(SEQUENCIA);
		assertThat(problema("ABCDEFGHIJKLMNOPQRST")).contains(SEQUENCIA);
		assertThat(problema("1234 5678 9012 345")).contains(SEQUENCIA); // espaços não escapam
		assertThat(problema("9876543210987654")).contains(SEQUENCIA);
	}

	@Test
	@DisplayName("regra 5: o nome (palavras de 4+ letras, sem acento) ou a parte local do e-mail na senha")
	void nomeOuEmail() {
		assertThat(politica.problema("o arthur rola dados demais", "Arthur Spironello", "a@x.com")).contains(NOME_EMAIL);
		assertThat(politica.problema("Spironello na taverna!", "Arthur Spironello", "a@x.com")).contains(NOME_EMAIL);
		assertThat(politica.problema("a Conceição chegou ontem", "Maria da Conceicao", "m@x.com")).contains(NOME_EMAIL); // sem acentos
		assertThat(politica.problema("meu login mestre.kai fica aqui", "Kai", "mestre.kai@x.com")).contains(NOME_EMAIL);
		// palavras do nome com menos de 4 letras e parte local curta não valem
		assertThat(politica.problema("o ana rola dados demais", "Ana Li", "ana@x.com")).isEmpty();
	}

	@Test
	@DisplayName("regra 6: tira o que não é letra; se sobra uma senha comum, recusa")
	void comum() {
		assertThat(problema("password12345678")).contains(COMUM);
		assertThat(problema("Senha@2026!!!!!!!")).contains(COMUM);
		assertThat(problema("correct horse battery staple")).contains(COMUM);
		assertThat(problema("Dungeons and Dragons 2026!")).contains(COMUM);
		assertThat(problema("dragao e taverna senha")).isEmpty(); // contém "senha", mas não é só ela
	}

	@Test
	@DisplayName("positivo: frase com espaços e acentos é aceita")
	void positivo() {
		assertThat(problema("dragão vermelho na taverna")).isEmpty();
	}

	@Test
	@DisplayName("a ordem das regras: a primeira que falha é a devolvida")
	void ordem() {
		assertThat(problema("aaaa")).contains(CURTA); // curta e repetida: vence a 1
		assertThat(problema("ç".repeat(37))).contains(LONGA); // grande em bytes e repetida: vence a 2
		assertThat(problema("01234567890123456789")).contains(REPETIDO); // repetida e sequência: vence a 3
		assertThat(politica.problema("qwertyuiopasdfg", "Ana", "qwerty@x.com")).contains(SEQUENCIA); // 4 antes da 5
		assertThat(politica.problema("Senha@2026!!!!!!!", "Senha Boa", "x@x.com")).contains(NOME_EMAIL); // 5 antes da 6
	}

	@Test
	@DisplayName("nome e e-mail nulos não quebram")
	void nulos() {
		assertThat(politica.problema("dragão vermelho na taverna", null, null)).isEmpty();
	}

	@Test
	@DisplayName("o arquivo senhas-comuns.txt: de 200 a 400 entradas, só letras minúsculas, sem repetição e com o xkcd")
	void arquivo() throws IOException {
		var linhas = new String(new ClassPathResource("senhas-comuns.txt").getInputStream().readAllBytes(),
				java.nio.charset.StandardCharsets.UTF_8).lines().toList();
		var entradas = linhas.stream().filter(l -> !l.startsWith("#") && !l.isBlank()).toList();

		assertThat(linhas.getFirst()).startsWith("#");
		assertThat(entradas).hasSizeBetween(200, 400).doesNotHaveDuplicates().allMatch(l -> l.matches("[a-z]+"));
		assertThat(entradas).contains("correcthorsebatterystaple", "password", "senha", "qwerty", "dungeonsanddragons");

		PoliticaDeSenha real = new PoliticaDeSenha(new ClassPathResource("senhas-comuns.txt"));
		assertThat(real.problema("correct horse battery staple", "Ana", "a@x.com")).contains(COMUM);
		assertThat(real.problema("Senha@2026!!!!!!!", "Ana", "a@x.com")).contains(COMUM);
	}
}
