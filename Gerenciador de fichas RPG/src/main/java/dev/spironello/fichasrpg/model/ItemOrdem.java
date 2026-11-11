package dev.spironello.fichasrpg.model;

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
 * Um item do inventário de uma ficha de Ordem Paranormal. A ligação é só deste lado: a FichaOrdem não tem
 * coleção, então a listagem de fichas não toca nos itens e o PUT da ficha não os gerencia.
 */
@Entity
@Table(name = "ficha_ordem_item")
public class ItemOrdem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ficha_id", nullable = false)
	private FichaOrdem ficha;

	@Column(nullable = false)
	private String nome;

	/** De 0 a 4 (mostrada como 0, I, II, III, IV). */
	private int categoria;

	private int espacos;

	private int quantidade;

	@Column(length = 4000)
	private String descricao;

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
	public int getCategoria() {
		return categoria;
	}
	public void setCategoria(int categoria) {
		this.categoria = categoria;
	}
	public int getEspacos() {
		return espacos;
	}
	public void setEspacos(int espacos) {
		this.espacos = espacos;
	}
	public int getQuantidade() {
		return quantidade;
	}
	public void setQuantidade(int quantidade) {
		this.quantidade = quantidade;
	}
	public String getDescricao() {
		return descricao;
	}
	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}
}
