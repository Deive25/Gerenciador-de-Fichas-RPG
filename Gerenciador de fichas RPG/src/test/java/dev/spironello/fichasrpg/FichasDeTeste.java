package dev.spironello.fichasrpg;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.spironello.fichasrpg.model.AtributoDnd;
import dev.spironello.fichasrpg.model.AtributoOrdem;
import dev.spironello.fichasrpg.model.EdicaoDnd;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.HabilidadeFicha;
import dev.spironello.fichasrpg.model.TipoHabilidade;

/** Fichas válidas para os testes que só precisam de "uma ficha" no banco. */
public final class FichasDeTeste {

	private FichasDeTeste() {
	}

	/** Ficha de Ordem Paranormal válida: NEX 5, os cinco atributos em 1, sem perícias nem recursos. */
	public static FichaOrdem ordem(String personagem) {
		FichaOrdem ficha = new FichaOrdem();
		ficha.setJogador("Ana");
		ficha.setPersonagem(personagem);
		ficha.setGenero("F");
		ficha.setNex(5);
		for (AtributoOrdem atributo : AtributoOrdem.values()) {
			ficha.setAtributo(atributo, 1);
		}
		return ficha;
	}

	/** Ficha de D&D válida: edição 2024, nível 1, os seis atributos em 10, sem perícias, salvaguardas nem recursos. */
	public static FichaDnd dnd(String personagem) {
		FichaDnd ficha = new FichaDnd();
		ficha.setJogador("Ana");
		ficha.setPersonagem(personagem);
		ficha.setGenero("F");
		ficha.setEdicao(EdicaoDnd.D2024);
		ficha.setNivel(1);
		for (AtributoDnd atributo : AtributoDnd.values()) {
			ficha.setAtributo(atributo, 10);
		}
		return ficha;
	}

	/**
	 * Corpo JSON válido de uma ficha de Ordem, como mapa mutável: o teste troca ou remove a chave que quer
	 * estragar e serializa com o ObjectMapper. NEX 5, os cinco atributos em 1, sem perícias nem recursos.
	 */
	public static Map<String, Object> corpoOrdem() {
		Map<String, Object> atributos = new LinkedHashMap<>();
		for (AtributoOrdem atributo : AtributoOrdem.values()) {
			atributos.put(atributo.name(), 1);
		}
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("sistema", "ORDEM_PARANORMAL");
		corpo.put("jogador", "Arthur");
		corpo.put("personagem", "Ana");
		corpo.put("genero", "F");
		corpo.put("nex", 5);
		corpo.put("atributos", atributos);
		return corpo;
	}

	/**
	 * Corpo JSON válido de uma ficha de D&D, como mapa mutável: edição 2024, nível 1, os seis atributos em 10,
	 * sem recursos, salvaguardas nem perícias.
	 */
	public static Map<String, Object> corpoDnd() {
		Map<String, Object> atributos = new LinkedHashMap<>();
		for (AtributoDnd atributo : AtributoDnd.values()) {
			atributos.put(atributo.name(), 10);
		}
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("sistema", "DND");
		corpo.put("jogador", "Arthur");
		corpo.put("personagem", "Ana");
		corpo.put("genero", "F");
		corpo.put("edicao", "D2024");
		corpo.put("nivel", 1);
		corpo.put("atributos", atributos);
		return corpo;
	}

	/** Habilidade pronta para salvar (a ficha já tem de estar no banco). */
	public static HabilidadeFicha habilidade(Ficha ficha, String nome,
			TipoHabilidade tipo, String descricao) {
		HabilidadeFicha habilidade = new HabilidadeFicha();
		habilidade.setFicha(ficha);
		habilidade.setNome(nome);
		habilidade.setTipo(tipo);
		habilidade.setDescricao(descricao);
		return habilidade;
	}
}
