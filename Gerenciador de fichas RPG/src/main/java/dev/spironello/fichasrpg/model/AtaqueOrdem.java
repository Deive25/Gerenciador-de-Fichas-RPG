package dev.spironello.fichasrpg.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Um ataque de uma ficha de Ordem Paranormal. A ligação é só deste lado: a FichaOrdem não tem coleção, então a
 * listagem de fichas não toca nos ataques e o PUT da ficha não os gerencia.
 */
@Entity
@Table(name = "ficha_ordem_ataque")
public class AtaqueOrdem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ficha_id", nullable = false)
	private FichaOrdem ficha;

	@Column(nullable = false)
	private String nome;

	/** Só LUTA ou PONTARIA (a API e o CHECK do banco recusam as outras). */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PericiaOrdem pericia;

	private int bonus;

	/** Notação de dados já validada e canônica (ex.: 1d8+3). */
	@Column(nullable = false, length = 60)
	private String dano;

	private String tipoDano;

	private String alcance;

	/** Menor valor do d20 que é crítico (de 2 a 20). */
	private int margem;

	/** Quanto se multiplicam os dados do dano no crítico (de 2 a 5). */
	private int multiplicador;

	public Long getId() {
		return id;
	}
	public FichaOrdem getFicha() {
		return ficha;
	}
	public void setFicha(FichaOrdem ficha) {
		this.ficha = ficha;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public PericiaOrdem getPericia() {
		return pericia;
	}
	public void setPericia(PericiaOrdem pericia) {
		this.pericia = pericia;
	}
	public int getBonus() {
		return bonus;
	}
	public void setBonus(int bonus) {
		this.bonus = bonus;
	}
	public String getDano() {
		return dano;
	}
	public void setDano(String dano) {
		this.dano = dano;
	}
	public String getTipoDano() {
		return tipoDano;
	}
	public void setTipoDano(String tipoDano) {
		this.tipoDano = tipoDano;
	}
	public String getAlcance() {
		return alcance;
	}
	public void setAlcance(String alcance) {
		this.alcance = alcance;
	}
	public int getMargem() {
		return margem;
	}
	public void setMargem(int margem) {
		this.margem = margem;
	}
	public int getMultiplicador() {
		return multiplicador;
	}
	public void setMultiplicador(int multiplicador) {
		this.multiplicador = multiplicador;
	}
}
