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
 * Uma magia da ficha de D&D (nível 0 é o truque). A ligação é só deste lado, como no {@link ItemDnd}: a
 * FichaDnd não tem coleção, então a listagem não toca nas magias e o PUT da ficha não as gerencia.
 */
@Entity
@Table(name = "ficha_dnd_magia")
public class MagiaDnd {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ficha_id", nullable = false)
	private FichaDnd ficha;

	@Column(nullable = false)
	private String nome;

	private int nivel;

	private String escola;
	private String tempo;
	private String alcance;
	private String componentes;
	private String duracao;

	private boolean concentracao;
	private boolean ritual;
	private boolean preparada;

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
	public int getNivel() {
		return nivel;
	}
	public void setNivel(int nivel) {
		this.nivel = nivel;
	}
	public String getEscola() {
		return escola;
	}
	public void setEscola(String escola) {
		this.escola = escola;
	}
	public String getTempo() {
		return tempo;
	}
	public void setTempo(String tempo) {
		this.tempo = tempo;
	}
	public String getAlcance() {
		return alcance;
	}
	public void setAlcance(String alcance) {
		this.alcance = alcance;
	}
	public String getComponentes() {
		return componentes;
	}
	public void setComponentes(String componentes) {
		this.componentes = componentes;
	}
	public String getDuracao() {
		return duracao;
	}
	public void setDuracao(String duracao) {
		this.duracao = duracao;
	}
	public boolean isConcentracao() {
		return concentracao;
	}
	public void setConcentracao(boolean concentracao) {
		this.concentracao = concentracao;
	}
	public boolean isRitual() {
		return ritual;
	}
	public void setRitual(boolean ritual) {
		this.ritual = ritual;
	}
	public boolean isPreparada() {
		return preparada;
	}
	public void setPreparada(boolean preparada) {
		this.preparada = preparada;
	}
	public String getDescricao() {
		return descricao;
	}
	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}
}
