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
 * Uma habilidade da ficha (classe, trilha, talento...). A ligação é só deste lado: a Ficha não tem coleção,
 * então a listagem de fichas não toca nas habilidades e o PUT da ficha não as gerencia.
 */
@Entity
@Table(name = "ficha_habilidade")
public class HabilidadeFicha {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ficha_id", nullable = false)
	private Ficha ficha;

	@Column(nullable = false)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoHabilidade tipo;

	@Column(length = 4000)
	private String descricao;

	public Long getId() {
		return id;
	}
	public Ficha getFicha() {
		return ficha;
	}
	public void setFicha(Ficha ficha) {
		this.ficha = ficha;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public TipoHabilidade getTipo() {
		return tipo;
	}
	public void setTipo(TipoHabilidade tipo) {
		this.tipo = tipo;
	}
	public String getDescricao() {
		return descricao;
	}
	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}
}
