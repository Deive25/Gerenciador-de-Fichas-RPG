package dev.spironello.fichasrpg.model;

import org.hibernate.annotations.Formula;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Dados comuns a qualquer sistema de RPG. Herança JOINED: esta tabela (ficha) guarda o que é comum e a coluna
 * "sistema" (o discriminador); cada sistema tem a sua tabela com chave igual ao id da ficha (ex.: ficha_ordem).
 */
@Entity
@Table(name = "ficha")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "sistema", discriminatorType = DiscriminatorType.STRING, length = 30)
public abstract class Ficha {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String jogador;

	@Column(nullable = false)
	private String personagem;

	@Column(nullable = false)
	private String genero;

	@Column(nullable = true)
	private Integer idade;

	/*
	 * Versão do retrato (retrato_ficha.atualizado_em em milissegundos), ou null sem retrato (2.1c).
	 * É uma subconsulta dentro do mesmo SELECT da ficha: a listagem continua com uma consulta só
	 * (sem N+1) e nunca lê os bytes da imagem. Só leitura: quem grava é o RetratoService.
	 */
	// 'epoch' como texto: sem aspas, o Hibernate acharia que é uma coluna e poria o alias da tabela na frente.
	// extract devolve numeric (exato) no PostgreSQL 14+, então a conta em ms não tem erro de arredondamento.
	@Formula("(select cast(floor(extract('epoch' from r.atualizado_em) * 1000) as bigint) "
			+ "from retrato_ficha r where r.ficha_id = id)")
	private Long retratoVersao;

	/** Sistema desta ficha; é o valor gravado na coluna discriminadora. */
	public abstract Sistema getSistema();

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getJogador() {
		return jogador;
	}
	public void setJogador(String jogador) {
		this.jogador = jogador;
	}
	public String getPersonagem() {
		return personagem;
	}
	public void setPersonagem(String personagem) {
		this.personagem = personagem;
	}
	public String getGenero() {
		return genero;
	}
	public void setGenero(String genero) {
		this.genero = genero;
	}
	public Integer getIdade() {
		return idade;
	}
	public void setIdade(Integer idade) {
		this.idade = idade;
	}
	public Long getRetratoVersao() {
		return retratoVersao;
	}
}
