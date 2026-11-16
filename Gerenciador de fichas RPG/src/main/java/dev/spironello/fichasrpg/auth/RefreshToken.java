package dev.spironello.fichasrpg.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Refresh token guardado só como hash (SHA-256 em hexadecimal). A "familia" identifica um login: todos os tokens
 * trocados a partir dele compartilham o mesmo uuid, e revogar a cadeia inteira é um UPDATE por família.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "usuario_id", nullable = false)
	private Long usuarioId;

	@Column(nullable = false)
	private UUID familia;

	@Column(nullable = false, length = 64)
	private String hash;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	@Column(name = "expira_em", nullable = false)
	private Instant expiraEm;

	@Column(name = "revogado_em")
	private Instant revogadoEm;

	@Column(name = "substituido_por")
	private Long substituidoPor;

	protected RefreshToken() {
	}

	public RefreshToken(Long usuarioId, UUID familia, String hash, Instant criadoEm, Instant expiraEm) {
		this.usuarioId = usuarioId;
		this.familia = familia;
		this.hash = hash;
		this.criadoEm = criadoEm;
		this.expiraEm = expiraEm;
	}

	public Long getId() {
		return id;
	}
	public Long getUsuarioId() {
		return usuarioId;
	}
	public UUID getFamilia() {
		return familia;
	}
	public String getHash() {
		return hash;
	}
	public Instant getCriadoEm() {
		return criadoEm;
	}
	public Instant getExpiraEm() {
		return expiraEm;
	}
	public Instant getRevogadoEm() {
		return revogadoEm;
	}
	public Long getSubstituidoPor() {
		return substituidoPor;
	}

	public boolean revogado() {
		return revogadoEm != null;
	}

	public void revogar(Instant quando) {
		this.revogadoEm = quando;
	}

	public void substituidoPor(Long novoId) {
		this.substituidoPor = novoId;
	}
}
