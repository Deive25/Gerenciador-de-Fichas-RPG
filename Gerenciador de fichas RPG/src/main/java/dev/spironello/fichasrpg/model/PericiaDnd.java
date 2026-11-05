package dev.spironello.fichasrpg.model;

/**
 * As 18 perícias de D&D (iguais nas duas edições), cada uma com o nome de exibição e o atributo-base.
 * No banco, a constante é gravada pelo nome (ex.: ACROBACIA), então renomear exige uma migração.
 */
public enum PericiaDnd {
	ATLETISMO("Atletismo", AtributoDnd.FOR),
	ACROBACIA("Acrobacia", AtributoDnd.DES),
	FURTIVIDADE("Furtividade", AtributoDnd.DES),
	PRESTIDIGITACAO("Prestidigitação", AtributoDnd.DES),
	ARCANISMO("Arcanismo", AtributoDnd.INT),
	HISTORIA("História", AtributoDnd.INT),
	INVESTIGACAO("Investigação", AtributoDnd.INT),
	NATUREZA("Natureza", AtributoDnd.INT),
	RELIGIAO("Religião", AtributoDnd.INT),
	INTUICAO("Intuição", AtributoDnd.SAB),
	LIDAR_COM_ANIMAIS("Lidar com Animais", AtributoDnd.SAB),
	MEDICINA("Medicina", AtributoDnd.SAB),
	PERCEPCAO("Percepção", AtributoDnd.SAB),
	SOBREVIVENCIA("Sobrevivência", AtributoDnd.SAB),
	ATUACAO("Atuação", AtributoDnd.CAR),
	ENGANACAO("Enganação", AtributoDnd.CAR),
	INTIMIDACAO("Intimidação", AtributoDnd.CAR),
	PERSUASAO("Persuasão", AtributoDnd.CAR);

	private final String nome;
	private final AtributoDnd atributo;

	PericiaDnd(String nome, AtributoDnd atributo) {
		this.nome = nome;
		this.atributo = atributo;
	}

	public String getNome() {
		return nome;
	}

	public AtributoDnd getAtributo() {
		return atributo;
	}
}
