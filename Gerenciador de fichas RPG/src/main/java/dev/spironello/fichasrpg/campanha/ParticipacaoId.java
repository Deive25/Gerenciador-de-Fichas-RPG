package dev.spironello.fichasrpg.campanha;

import java.io.Serializable;
import java.util.Objects;

/** Chave composta da participação (campanha e usuário), usada com {@code @IdClass}. */
public class ParticipacaoId implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long campanhaId;
	private Long usuarioId;

	public ParticipacaoId() {
	}

	public ParticipacaoId(Long campanhaId, Long usuarioId) {
		this.campanhaId = campanhaId;
		this.usuarioId = usuarioId;
	}

	public Long getCampanhaId() {
		return campanhaId;
	}
	public Long getUsuarioId() {
		return usuarioId;
	}

	@Override
	public boolean equals(Object outro) {
		return outro instanceof ParticipacaoId id && Objects.equals(campanhaId, id.campanhaId)
				&& Objects.equals(usuarioId, id.usuarioId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(campanhaId, usuarioId);
	}
}
