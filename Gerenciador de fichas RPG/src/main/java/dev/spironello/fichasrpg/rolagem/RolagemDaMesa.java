package dev.spironello.fichasrpg.rolagem;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Uma rolagem gravada na mesa (tabela "rolagem", V12). O nome da classe tem o "DaMesa" porque {@code service.Rolagem}
 * já é o registro da notação. Colunas simples (sem @ManyToOne), como a participação: a integridade fica nas chaves
 * estrangeiras. {@code personagem} é uma cópia do nome na hora da rolagem e fica nulo quando a ficha é do mestre.
 */
@Entity
@Table(name = "rolagem")
public class RolagemDaMesa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "campanha_id", nullable = false)
	private Long campanhaId;

	@Column(name = "ficha_id")
	private Long fichaId;

	@Column(name = "usuario_id", nullable = false)
	private Long usuarioId;

	@Column(length = 255)
	private String personagem;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TipoRolagem tipo;

	@Column(nullable = false, length = 255)
	private String rotulo;

	@Column(nullable = false)
	private int total;

	/** O JSON da resposta da rota, já como texto: o Hibernate só o entrega ao jsonb, sem passar pelo Jackson dele. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private String detalhe;

	@Column(name = "criada_em", nullable = false)
	private Instant criadaEm;

	protected RolagemDaMesa() {
	}

	public RolagemDaMesa(Long campanhaId, Long fichaId, Long usuarioId, String personagem, TipoRolagem tipo, String rotulo,
			int total, String detalhe, Instant criadaEm) {
		this.campanhaId = campanhaId;
		this.fichaId = fichaId;
		this.usuarioId = usuarioId;
		this.personagem = personagem;
		this.tipo = tipo;
		this.rotulo = rotulo;
		this.total = total;
		this.detalhe = detalhe;
		this.criadaEm = criadaEm;
	}

	public Long getId() {
		return id;
	}
	public Long getCampanhaId() {
		return campanhaId;
	}
	public Long getFichaId() {
		return fichaId;
	}
	public Long getUsuarioId() {
		return usuarioId;
	}
	public String getPersonagem() {
		return personagem;
	}
	public TipoRolagem getTipo() {
		return tipo;
	}
	public String getRotulo() {
		return rotulo;
	}
	public int getTotal() {
		return total;
	}
	public String getDetalhe() {
		return detalhe;
	}
	public Instant getCriadaEm() {
		return criadaEm;
	}
}
