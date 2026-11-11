package dev.spironello.fichasrpg.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Um item do inventário de uma ficha de D&D. A ligação é só deste lado: a FichaDnd não tem coleção, então a
 * listagem de fichas não toca nos itens e o PUT da ficha não os gerencia.
 */
@Entity
@Table(name = "ficha_dnd_item")
public class ItemDnd {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ficha_id", nullable = false)
	private FichaDnd ficha;

	@Column(nullable = false)
	private String nome;

	private int quantidade;

	/** Peso de uma unidade, em kg. */
	@Column(nullable = false, precision = 7, scale = 2)
	private BigDecimal peso;

	private boolean equipado;

	@Column(length = 4000)
	private String descricao;

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
	public int getQuantidade() {
		return quantidade;
	}
	public void setQuantidade(int quantidade) {
		this.quantidade = quantidade;
	}
	public BigDecimal getPeso() {
		return peso;
	}
	public void setPeso(BigDecimal peso) {
		this.peso = peso;
	}
	public boolean isEquipado() {
		return equipado;
	}
	public void setEquipado(boolean equipado) {
		this.equipado = equipado;
	}
	public String getDescricao() {
		return descricao;
	}
	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}
}
