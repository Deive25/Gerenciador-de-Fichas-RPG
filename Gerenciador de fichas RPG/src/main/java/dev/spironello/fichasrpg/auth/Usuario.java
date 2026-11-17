package dev.spironello.fichasrpg.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Conta de usuário. senhaHash nulo = não entra com senha (o usuário "legado" da V10). */
@Entity
@Table(name = "usuario")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(nullable = false, length = 100)
	private String nome;

	@Column(name = "senha_hash", length = 100)
	private String senhaHash;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	/** Identidade (claim "sub") da conta do Google ligada; nulo = conta so por senha (ou o legado). */
	@Column(name = "google_sub", length = 255)
	private String googleSub;

	/** Conta de demonstração (V14): temporária, sem senha e sem Google; a limpeza a apaga depois de 24 h. */
	@Column(nullable = false)
	private boolean demo;

	protected Usuario() {
	}

	public Usuario(String email, String nome, String senhaHash, Instant criadoEm) {
		this.email = email;
		this.nome = nome;
		this.senhaHash = senhaHash;
		this.criadoEm = criadoEm;
	}

	/** Conta nova vinda do Google: sem senha. */
	public static Usuario doGoogle(String email, String nome, String googleSub, Instant criadoEm) {
		Usuario usuario = new Usuario(email, nome, null, criadoEm);
		usuario.googleSub = googleSub;
		return usuario;
	}

	/** Conta de demonstração: sem senha e sem Google, com um e-mail interno que não serve para login (domínio .invalid, RFC 2606). */
	public static Usuario demo(String email, String nome, Instant criadoEm) {
		Usuario usuario = new Usuario(email, nome, null, criadoEm);
		usuario.demo = true;
		return usuario;
	}

	/** Liga a conta ao Google e apaga a senha (quem provou ser dono do e-mail vence; spec §3). */
	public void ligarAoGoogle(String sub) {
		this.googleSub = sub;
		this.senhaHash = null;
	}

	public Long getId() {
		return id;
	}
	public String getEmail() {
		return email;
	}
	public String getNome() {
		return nome;
	}
	public String getSenhaHash() {
		return senhaHash;
	}
	public Instant getCriadoEm() {
		return criadoEm;
	}
	public String getGoogleSub() {
		return googleSub;
	}
	public boolean isDemo() {
		return demo;
	}
}
