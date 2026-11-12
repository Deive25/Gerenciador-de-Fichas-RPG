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
 * Um ataque de uma ficha de D&D. A ligação é só deste lado: a FichaDnd não tem coleção, então a listagem de
 * fichas não toca nos ataques e o PUT da ficha não os gerencia.
 */
@Entity
@Table(name = "ficha_dnd_ataque")
public class AtaqueDnd {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ficha_id", nullable = false)
	private FichaDnd ficha;

	@Column(nullable = false)
	private String nome;

	/** O atributo cujo modificador entra no ataque. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AtributoDnd atributo;

	/** Se a ficha soma a proficiência (pelo nível) ao ataque. */
	private boolean proficiente;

	private int bonus;

	/** Notação de dados já validada e canônica (ex.: 1d8+3). */
	@Column(nullable = false, length = 60)
	private String dano;

	private String tipoDano;

	private String alcance;

	public Long getId() {
		return id;
	}
	public FichaDnd getFicha() {
		return ficha;
	}
	public void setFicha(FichaDnd ficha) {
		this.ficha = ficha;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public AtributoDnd getAtributo() {
		return atributo;
	}
	public void setAtributo(AtributoDnd atributo) {
		this.atributo = atributo;
	}
	public boolean isProficiente() {
		return proficiente;
	}
	public void setProficiente(boolean proficiente) {
		this.proficiente = proficiente;
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
}
