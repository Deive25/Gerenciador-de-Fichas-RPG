package dev.spironello.fichasrpg.model;

/**
 * As 23 perícias da ficha. Cada constante guarda o nome de exibição e o nome da chave do JSON
 * de antes do 1.4b ("atribute1"...), hoje só histórico (V2 e testes). O vínculo é um campo explícito,
 * e não o ordinal(): reordenar o enum não pode mudar qual perícia é qual. No banco, a constante
 * é gravada pelo nome (ex.: ACROBACIA), então renomear uma constante exige uma migração.
 */
public enum Pericia {
	ACROBACIA("Acrobacia", "atribute1"),
	ATLETISMO("Atletismo", "atribute2"),
	ADESTRAR_ANIMAIS("Adestrar animais", "atribute3"),
	ATUACAO_LABIA("Atuação/Lábia", "atribute4"),
	CHARME("Charme", "atribute5"),
	ESQUIVA("Esquiva", "atribute6"),
	FURTIVIDADE("Furtividade", "atribute7"),
	LUTA_CORPO_A_CORPO("Luta corpo a corpo", "atribute8"),
	LUTA_ARMA_BRANCA("Luta com arma branca", "atribute9"),
	INTIMIDACAO("Intimidação", "atribute10"),
	INTUICAO("Intuição", "atribute11"),
	INVESTIGACAO("Investigação", "atribute12"),
	ARMAS_DE_FOGO_LEVES("Armas de fogo leves", "atribute13"),
	ARMAS_DE_FOGO_MEDIAS("Armas de fogo médias", "atribute14"),
	ARMAS_DE_FOGO_PESADAS("Armas de fogo pesadas", "atribute15"),
	PERCEPCAO("Percepção", "atribute16"),
	USAR_BIBLIOTECAS("Usar bibliotecas", "atribute17"),
	HISTORIA("História", "atribute18"),
	CURA("Cura", "atribute19"),
	MALANDRAGEM("Malandragem", "atribute20"),
	OCULTISMO("Ocultismo", "atribute21"),
	PSICOLOGIA("Psicologia", "atribute22"),
	ARREMESSAR("Arremessar", "atribute23");

	private final String nome;
	private final String campoLegado;

	Pericia(String nome, String campoLegado) {
		this.nome = nome;
		this.campoLegado = campoLegado;
	}

	public String getNome() {
		return nome;
	}

	/** Nome da chave antiga do JSON (atribute1...). Desde o 1.6 a API usa o código do enum; este campo só serve à migração V2 e aos testes de mapeamento. */
	public String getCampoLegado() {
		return campoLegado;
	}
}
