package dev.spironello.fichasrpg.campanha;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Uma mesa de jogo: o mestre convida os jogadores por um link com o código de convite (12 caracteres, sem validade). */
@Entity
@Table(name = "campanha")
public class Campanha {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nome;

	@Column(length = 1000)
	private String descricao;

	@Column(name = "codigo_convite", nullable = false, length = 12)
	private String codigoConvite;

	@Column(name = "convite_ativo", nullable = false)
	private boolean conviteAtivo;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	protected Campanha() {
	}

	public Campanha(String nome, String descricao, String codigoConvite, Instant criadoEm) {
		this.nome = nome;
		this.descricao = descricao;
		this.codigoConvite = codigoConvite;
		this.conviteAtivo = true;
		this.criadoEm = criadoEm;
	}

	public Long getId() {
		return id;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public String getDescricao() {
		return descricao;
	}
	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}
	public String getCodigoConvite() {
		return codigoConvite;
	}
	public void setCodigoConvite(String codigoConvite) {
		this.codigoConvite = codigoConvite;
	}
	public boolean isConviteAtivo() {
		return conviteAtivo;
	}
	public void setConviteAtivo(boolean conviteAtivo) {
		this.conviteAtivo = conviteAtivo;
	}
	public Instant getCriadoEm() {
		return criadoEm;
	}
}
