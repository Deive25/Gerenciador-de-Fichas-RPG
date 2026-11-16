package dev.spironello.fichasrpg.campanha;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Participação de um usuário numa campanha, com o papel dele. Colunas simples (sem @ManyToOne), como o dono da ficha:
 * a integridade fica nas chaves estrangeiras do banco (V11).
 */
@Entity
@Table(name = "participacao")
@IdClass(ParticipacaoId.class)
public class Participacao {

	@Id
	@Column(name = "campanha_id")
	private Long campanhaId;

	@Id
	@Column(name = "usuario_id")
	private Long usuarioId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Papel papel;

	@Column(name = "entrou_em", nullable = false)
	private Instant entrouEm;

	protected Participacao() {
	}

	public Participacao(Long campanhaId, Long usuarioId, Papel papel, Instant entrouEm) {
		this.campanhaId = campanhaId;
		this.usuarioId = usuarioId;
		this.papel = papel;
		this.entrouEm = entrouEm;
	}

	public Long getCampanhaId() {
		return campanhaId;
	}
	public Long getUsuarioId() {
		return usuarioId;
	}
	public Papel getPapel() {
		return papel;
	}
	public Instant getEntrouEm() {
		return entrouEm;
	}
	public boolean isMestre() {
		return papel == Papel.MESTRE;
	}
}
