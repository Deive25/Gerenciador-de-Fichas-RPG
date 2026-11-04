package dev.spironello.fichasrpg.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Retrato do personagem (JPEG já validado), numa tabela própria: a ficha não carrega os bytes.
 * A chave é o id da ficha; não há relação JPA com SheetPlayer porque nenhuma leitura da ficha
 * precisa da imagem (a versão vem por subconsulta, ver SheetPlayer.retratoVersao).
 * Sem @Lob: no PostgreSQL, @Lob vira "oid" (objeto grande); byte[] simples vira "bytea".
 */
@Entity
@Table(name = "retrato_ficha")
public class RetratoFicha {

	@Id
	@Column(name = "ficha_id")
	private Long fichaId;

	@Column(nullable = false)
	private byte[] conteudo;

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	protected RetratoFicha() {
		// exigido pelo JPA
	}

	public RetratoFicha(Long fichaId, byte[] conteudo, Instant atualizadoEm) {
		this.fichaId = fichaId;
		this.conteudo = conteudo;
		this.atualizadoEm = atualizadoEm;
	}

	public Long getFichaId() {
		return fichaId;
	}

	public byte[] getConteudo() {
		return conteudo;
	}

	public Instant getAtualizadoEm() {
		return atualizadoEm;
	}
}
