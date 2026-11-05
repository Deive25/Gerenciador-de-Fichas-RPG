package dev.spironello.fichasrpg.model;

/**
 * As 28 perícias de Ordem Paranormal, cada uma com o nome de exibição e o atributo-base.
 * No banco, a constante é gravada pelo nome (ex.: ACROBACIA), então renomear exige uma migração.
 */
public enum PericiaOrdem {
	ACROBACIA("Acrobacia", AtributoOrdem.AGI),
	ADESTRAMENTO("Adestramento", AtributoOrdem.PRE),
	ARTES("Artes", AtributoOrdem.PRE),
	ATLETISMO("Atletismo", AtributoOrdem.FOR),
	ATUALIDADES("Atualidades", AtributoOrdem.INT),
	CIENCIAS("Ciências", AtributoOrdem.INT),
	CRIME("Crime", AtributoOrdem.AGI),
	DIPLOMACIA("Diplomacia", AtributoOrdem.PRE),
	ENGANACAO("Enganação", AtributoOrdem.PRE),
	FORTITUDE("Fortitude", AtributoOrdem.VIG),
	FURTIVIDADE("Furtividade", AtributoOrdem.AGI),
	INICIATIVA("Iniciativa", AtributoOrdem.AGI),
	INTIMIDACAO("Intimidação", AtributoOrdem.PRE),
	INTUICAO("Intuição", AtributoOrdem.PRE),
	INVESTIGACAO("Investigação", AtributoOrdem.INT),
	LUTA("Luta", AtributoOrdem.FOR),
	MEDICINA("Medicina", AtributoOrdem.INT),
	OCULTISMO("Ocultismo", AtributoOrdem.INT),
	PERCEPCAO("Percepção", AtributoOrdem.PRE),
	PILOTAGEM("Pilotagem", AtributoOrdem.AGI),
	PONTARIA("Pontaria", AtributoOrdem.AGI),
	PROFISSAO("Profissão", AtributoOrdem.INT),
	REFLEXOS("Reflexos", AtributoOrdem.AGI),
	RELIGIAO("Religião", AtributoOrdem.PRE),
	SOBREVIVENCIA("Sobrevivência", AtributoOrdem.INT),
	TATICA("Tática", AtributoOrdem.INT),
	TECNOLOGIA("Tecnologia", AtributoOrdem.INT),
	VONTADE("Vontade", AtributoOrdem.PRE);

	private final String nome;
	private final AtributoOrdem atributo;

	PericiaOrdem(String nome, AtributoOrdem atributo) {
		this.nome = nome;
		this.atributo = atributo;
	}

	public String getNome() {
		return nome;
	}

	public AtributoOrdem getAtributo() {
		return atributo;
	}
}
