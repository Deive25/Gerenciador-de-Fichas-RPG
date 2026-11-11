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
 * Um ritual da ficha de Ordem Paranormal. A ligação é só deste lado, como no {@link ItemOrdem}: a FichaOrdem
 * não tem coleção, então a listagem não toca nos rituais e o PUT da ficha não os gerencia.
 */
@Entity
@Table(name = "ficha_ordem_ritual")
public class RitualOrdem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ficha_id", nullable = false)
	private FichaOrdem ficha;

	@Column(nullable = false)
	private String nome;

	private int circulo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ElementoRitual elemento;

	private String execucao;
	private String alcance;
	private String alvo;
	private String duracao;
	private String resistencia;

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
	public int getCirculo() {
		return circulo;
	}
	public void setCirculo(int circulo) {
		this.circulo = circulo;
	}
	public ElementoRitual getElemento() {
		return elemento;
	}
	public void setElemento(ElementoRitual elemento) {
		this.elemento = elemento;
	}
	public String getExecucao() {
		return execucao;
	}
	public void setExecucao(String execucao) {
		this.execucao = execucao;
	}
	public String getAlcance() {
		return alcance;
	}
	public void setAlcance(String alcance) {
		this.alcance = alcance;
	}
	public String getAlvo() {
		return alvo;
	}
	public void setAlvo(String alvo) {
		this.alvo = alvo;
	}
	public String getDuracao() {
		return duracao;
	}
	public void setDuracao(String duracao) {
		this.duracao = duracao;
	}
	public String getResistencia() {
		return resistencia;
	}
	public void setResistencia(String resistencia) {
		this.resistencia = resistencia;
	}
	public String getDescricao() {
		return descricao;
	}
	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}
}
