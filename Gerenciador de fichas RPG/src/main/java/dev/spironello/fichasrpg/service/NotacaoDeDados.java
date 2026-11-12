package dev.spironello.fichasrpg.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.spironello.fichasrpg.dto.DadoRolado;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.exception.NotacaoInvalidaException;

/**
 * A notação de dados do servidor, única para o dano dos ataques e para a rolagem livre (ADR 0021): termos
 * {@code NdM} (N de 1 a 20; M em 4, 6, 8, 10, 12, 20 ou 100) ou constantes de 0 a 99, ligados por {@code +} ou
 * {@code -}. Até 8 termos e 20 dados somados; só as constantes podem ser subtraídas. Espaços são ignorados e o
 * {@code d} pode ser maiúsculo. Classe sem estado e sem Spring: ler e rolar são funções puras (o {@link Dado} vem
 * de fora).
 */
public final class NotacaoDeDados {

	private static final int MAX_TERMOS = 8;
	private static final int MAX_DADOS = 20;
	private static final int MAX_CONSTANTE = 99;
	private static final Set<Integer> LADOS = Set.of(4, 6, 8, 10, 12, 20, 100);

	private static final String INVALIDA = "notação inválida (ex.: 2d6+3)";

	/** Um termo ou constante: {@code NdM} ou só um número. Sem espaços, depois da limpeza. */
	private static final String TERMO = "\\d+(?:[dD]\\d+)?";
	private static final Pattern EXPRESSAO = Pattern.compile(TERMO + "(?:[+-]" + TERMO + ")*");
	private static final Pattern UM_TERMO = Pattern.compile("([+-]?)(\\d+)(?:[dD](\\d+))?");

	private NotacaoDeDados() {
	}

	/** {@code quantidade} dados de {@code lados} lados. */
	public record Termo(int quantidade, int lados) {
	}

	/** A expressão lida: os dados na ordem em que aparecem, o bônus fixo e a forma canônica (sem espaços, minúscula). */
	public record Expressao(List<Termo> dados, int bonus, String canonica) {
	}

	/** Lê e valida; lança {@link NotacaoInvalidaException} com a mensagem do caso. */
	public static Expressao ler(String texto) {
		String limpo = texto == null ? "" : texto.replaceAll("\\s+", "");
		if (!EXPRESSAO.matcher(limpo).matches()) {
			throw new NotacaoInvalidaException(INVALIDA);
		}
		List<Termo> dados = new ArrayList<>();
		StringBuilder canonica = new StringBuilder();
		int bonus = 0;
		int totalDeDados = 0;
		int termos = 0;
		Matcher termo = UM_TERMO.matcher(limpo);
		while (termo.find()) {
			termos++;
			if (termos > MAX_TERMOS) {
				throw new NotacaoInvalidaException("no máximo " + MAX_TERMOS + " termos");
			}
			boolean negativo = "-".equals(termo.group(1));
			int primeiro = numero(termo.group(2));
			if (termo.group(3) == null) {
				if (primeiro > MAX_CONSTANTE) {
					throw new NotacaoInvalidaException("a constante deve ir de 0 a " + MAX_CONSTANTE);
				}
				bonus += negativo ? -primeiro : primeiro;
				anexar(canonica, negativo, String.valueOf(primeiro));
				continue;
			}
			int lados = numero(termo.group(3));
			if (negativo) {
				throw new NotacaoInvalidaException("dados não podem ser subtraídos");
			}
			if (primeiro == 0) {
				throw new NotacaoInvalidaException("use de 1 a " + MAX_DADOS + " dados em cada termo");
			}
			if (primeiro > MAX_DADOS) {
				throw new NotacaoInvalidaException("no máximo " + MAX_DADOS + " dados");
			}
			if (!LADOS.contains(lados)) {
				throw new NotacaoInvalidaException("use d4, d6, d8, d10, d12, d20 ou d100");
			}
			totalDeDados += primeiro;
			dados.add(new Termo(primeiro, lados));
			anexar(canonica, false, primeiro + "d" + lados);
		}
		if (totalDeDados > MAX_DADOS) {
			throw new NotacaoInvalidaException("no máximo " + MAX_DADOS + " dados");
		}
		return new Expressao(List.copyOf(dados), bonus, canonica.toString());
	}

	/**
	 * Como {@link #ler}, mas o erro já sai como 400 no campo informado (errors.dano, errors.expressao).
	 */
	public static Expressao lerNoCampo(String texto, String campo) {
		try {
			return ler(texto);
		} catch (NotacaoInvalidaException e) {
			throw new DadosInvalidosException(Map.of(campo, e.getMessage()));
		}
	}

	/**
	 * Lê anotando o erro no mapa (campo -> mensagem) em vez de lançar, para o serviço juntar com os outros erros do
	 * pedido e devolver todos de uma vez. Devolve null se a notação é inválida.
	 */
	public static Expressao lerAnotando(String texto, String campo, Map<String, String> erros) {
		try {
			return ler(texto);
		} catch (NotacaoInvalidaException e) {
			erros.put(campo, e.getMessage());
			return null;
		}
	}

	/**
	 * Rola a expressão. O {@code multiplicadorDeDados} é 1 no normal e 2 (ou o multiplicador do ataque) no
	 * crítico: ele multiplica a quantidade de dados de cada termo, nunca as constantes. O total não fica abaixo de 0.
	 */
	public static Rolagem rolar(Expressao expressao, int multiplicadorDeDados, Dado dado) {
		List<DadoRolado> rolados = new ArrayList<>();
		int soma = 0;
		for (Termo termo : expressao.dados()) {
			for (int i = 0; i < termo.quantidade() * multiplicadorDeDados; i++) {
				int valor = dado.rolar(termo.lados());
				rolados.add(new DadoRolado(termo.lados(), valor));
				soma += valor;
			}
		}
		return new Rolagem(List.copyOf(rolados), expressao.bonus(), Math.max(0, soma + expressao.bonus()));
	}

	private static void anexar(StringBuilder canonica, boolean negativo, String termo) {
		if (!canonica.isEmpty()) {
			canonica.append(negativo ? '-' : '+');
		}
		canonica.append(termo);
	}

	/** Número grande demais para um int vira o maior int: cai nos limites em vez de estourar. */
	private static int numero(String digitos) {
		return digitos.length() > 9 ? Integer.MAX_VALUE : Integer.parseInt(digitos);
	}
}
