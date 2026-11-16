package dev.spironello.fichasrpg.auth;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Política de senha do CADASTRO, no espírito da NIST SP 800-63B-4 e do OWASP ASVS 5.0: tamanho mínimo generoso (15),
 * sem regras de composição (maiúscula, número, símbolo) e sem expiração; espaços e acentos valem; a senha não pode ser
 * previsível (trecho repetido, sequência, nome, e-mail, lista de senhas comuns). O login não passa por aqui: contas
 * antigas com senhas mais curtas continuam entrando.
 *
 * <p>As regras são verificadas nesta ordem; a primeira que falha devolve a mensagem.
 */
@Component
public class PoliticaDeSenha {

	static final int MIN_CARACTERES = 15;
	/** O BCrypt só lê os primeiros 72 bytes da senha; mais que isso seria ignorado em silêncio. */
	static final int MAX_BYTES = 72;

	private static final String[] SEQUENCIAS = { "0123456789", "abcdefghijklmnopqrstuvwxyz", "qwertyuiopasdfghjklzxcvbnm" };
	private static final Pattern MARCAS = Pattern.compile("\\p{M}+");
	private static final Pattern NAO_LETRAS = Pattern.compile("[^\\p{L}]+");
	private static final Pattern ESPACOS = Pattern.compile("\\s+");

	private final Set<String> comuns;
	/** Cada sequência três vezes, e as inversas: "contida nelas" cobre qualquer trecho crescente ou decrescente. */
	private final String[] sequenciasTriplas;

	/** Usado pelo Spring: lê o arquivo uma vez, ao subir. */
	@Autowired
	public PoliticaDeSenha(@Value("classpath:senhas-comuns.txt") Resource arquivo) {
		this(carregar(arquivo));
	}

	/** Usado nos testes: a lista vem pronta. */
	public PoliticaDeSenha(Set<String> comuns) {
		this.comuns = Set.copyOf(comuns);
		this.sequenciasTriplas = Arrays.stream(SEQUENCIAS)
				.flatMap(s -> Stream.of(s, new StringBuilder(s).reverse().toString()))
				.map(s -> s.repeat(3))
				.toArray(String[]::new);
	}

	/** Devolve o motivo da recusa, ou vazio se a senha serve. */
	public Optional<String> problema(String senha, String nome, String email) {
		if (senha.codePointCount(0, senha.length()) < MIN_CARACTERES) {
			return Optional.of("Use pelo menos 15 caracteres");
		}
		if (senha.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
			return Optional.of("A senha pode ter no máximo 72 caracteres");
		}
		String minuscula = senha.toLowerCase(Locale.ROOT);
		if (ehTrechoRepetido(minuscula)) {
			return Optional.of("Não repita o mesmo trecho");
		}
		String semEspacos = ESPACOS.matcher(minuscula).replaceAll("");
		if (Arrays.stream(sequenciasTriplas).anyMatch(s -> s.contains(semEspacos))) {
			return Optional.of("Não use sequências como 123456 ou qwerty");
		}
		String simples = semAcentos(minuscula);
		if (contemNomeOuEmail(simples, nome, email)) {
			return Optional.of("Não use o seu nome nem o seu e-mail");
		}
		if (comuns.contains(NAO_LETRAS.matcher(simples).replaceAll(""))) {
			return Optional.of("Essa senha é comum demais. Escolha outra");
		}
		return Optional.empty();
	}

	/**
	 * Verdadeiro se o texto é um mesmo trecho repetido pelo menos 2 vezes. O menor período p (s[i] == s[i-p] para todo
	 * i >= p) sai da função de prefixo do KMP: p = n - pi[n-1]. Repetido quando p <= n/2.
	 */
	private static boolean ehTrechoRepetido(String texto) {
		int[] s = texto.codePoints().toArray();
		int n = s.length;
		int[] pi = new int[n];
		for (int i = 1; i < n; i++) {
			int k = pi[i - 1];
			while (k > 0 && s[i] != s[k]) {
				k = pi[k - 1];
			}
			if (s[i] == s[k]) {
				k++;
			}
			pi[i] = k;
		}
		int periodo = n - pi[n - 1];
		return periodo <= n / 2;
	}

	private static boolean contemNomeOuEmail(String senhaSimples, String nome, String email) {
		if (nome != null) {
			for (String palavra : NAO_LETRAS.split(semAcentos(nome.toLowerCase(Locale.ROOT)))) {
				if (palavra.length() >= 4 && senhaSimples.contains(palavra)) {
					return true;
				}
			}
		}
		if (email != null) {
			String local = semAcentos(email.strip().toLowerCase(Locale.ROOT));
			int arroba = local.indexOf('@');
			local = arroba >= 0 ? local.substring(0, arroba) : local;
			return local.length() >= 4 && senhaSimples.contains(local);
		}
		return false;
	}

	/** NFD separa a letra do acento ("ã" vira "a" + til); as marcas somem. */
	private static String semAcentos(String texto) {
		return MARCAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
	}

	private static Set<String> carregar(Resource arquivo) {
		try {
			Set<String> lista = new HashSet<>();
			new String(arquivo.getInputStream().readAllBytes(), StandardCharsets.UTF_8).lines()
					.map(String::strip)
					.filter(linha -> !linha.isEmpty() && !linha.startsWith("#"))
					.forEach(lista::add);
			return lista;
		} catch (IOException e) {
			throw new UncheckedIOException("Não consegui ler a lista de senhas comuns", e);
		}
	}
}
