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

	protected Usuario() {
	}

	public Usuario(String email, String nome, String senhaHash, Instant criadoEm) {
		this.email = email;
		this.nome = nome;
		this.senhaHash = senhaHash;
		this.criadoEm = criadoEm;
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
}
