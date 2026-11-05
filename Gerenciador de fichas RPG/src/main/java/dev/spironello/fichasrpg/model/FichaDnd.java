package dev.spironello.fichasrpg.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Ficha de D&D (5e de 2014 ou 2024: a coluna edicao diz qual). Perícia sem entrada no mapa é sem proficiência.
 * Cada atributo e cada salvaguarda é uma coluna própria (como em FichaOrdem). O mapa é carregado sob demanda
 * (lazy); as leituras do repository usam @EntityGraph quando precisam dele, para evitar N+1.
 */
@Entity
@Table(name = "ficha_dnd")
@PrimaryKeyJoinColumn(name = "ficha_id")
@DiscriminatorValue("DND")
public class FichaDnd extends Ficha {

	@Enumerated(EnumType.STRING)
	private EdicaoDnd edicao;

	private String classe;
	private String especie;
	private String antecedente;

	private int nivel;

	private int forca;
	private int destreza;
	private int constituicao;
	private int inteligencia;
	private int sabedoria;
	private int carisma;

	private Integer pvAtual;
	private Integer pvMax;
	private Integer pvTemporario;
	private Integer ca;

	private boolean inspiracao;

	private boolean salvaForca;
	private boolean salvaDestreza;
	private boolean salvaConstituicao;
	private boolean salvaInteligencia;
	private boolean salvaSabedoria;
	private boolean salvaCarisma;

	@ElementCollection
	@CollectionTable(name = "ficha_dnd_pericia", joinColumns = @JoinColumn(name = "ficha_id"))
	@MapKeyEnumerated(EnumType.STRING)
	@MapKeyColumn(name = "pericia")
	@Enumerated(EnumType.STRING)
	@Column(name = "grau")
	private Map<PericiaDnd, GrauDnd> pericias = new EnumMap<>(PericiaDnd.class);

	@Override
	public Sistema getSistema() {
		return Sistema.DND;
	}

	public int getAtributo(AtributoDnd atributo) {
		return switch (atributo) {
			case FOR -> forca;
			case DES -> destreza;
			case CON -> constituicao;
			case INT -> inteligencia;
			case SAB -> sabedoria;
			case CAR -> carisma;
		};
	}

	public void setAtributo(AtributoDnd atributo, int valor) {
		switch (atributo) {
			case FOR -> forca = valor;
			case DES -> destreza = valor;
			case CON -> constituicao = valor;
			case INT -> inteligencia = valor;
			case SAB -> sabedoria = valor;
			case CAR -> carisma = valor;
		}
	}

	public boolean isProficienteEmSalvaguarda(AtributoDnd atributo) {
		return switch (atributo) {
			case FOR -> salvaForca;
			case DES -> salvaDestreza;
			case CON -> salvaConstituicao;
			case INT -> salvaInteligencia;
			case SAB -> salvaSabedoria;
			case CAR -> salvaCarisma;
		};
	}

	/** Define as salvaguardas proficientes: os atributos do conjunto ficam true e os demais false (nulo = nenhum). */
	public void setSalvaguardas(Set<AtributoDnd> proficientes) {
		Set<AtributoDnd> conjunto = proficientes == null ? Set.of() : proficientes;
		salvaForca = conjunto.contains(AtributoDnd.FOR);
		salvaDestreza = conjunto.contains(AtributoDnd.DES);
		salvaConstituicao = conjunto.contains(AtributoDnd.CON);
		salvaInteligencia = conjunto.contains(AtributoDnd.INT);
		salvaSabedoria = conjunto.contains(AtributoDnd.SAB);
		salvaCarisma = conjunto.contains(AtributoDnd.CAR);
	}

	public EdicaoDnd getEdicao() {
		return edicao;
	}
	public void setEdicao(EdicaoDnd edicao) {
		this.edicao = edicao;
	}
	public String getClasse() {
		return classe;
	}
	public void setClasse(String classe) {
		this.classe = classe;
	}
	public String getEspecie() {
		return especie;
	}
	public void setEspecie(String especie) {
		this.especie = especie;
	}
	public String getAntecedente() {
		return antecedente;
	}
	public void setAntecedente(String antecedente) {
		this.antecedente = antecedente;
	}
	public int getNivel() {
		return nivel;
	}
	public void setNivel(int nivel) {
		this.nivel = nivel;
	}
	public int getForca() {
		return forca;
	}
	public void setForca(int forca) {
		this.forca = forca;
	}
	public int getDestreza() {
		return destreza;
	}
	public void setDestreza(int destreza) {
		this.destreza = destreza;
	}
	public int getConstituicao() {
		return constituicao;
	}
	public void setConstituicao(int constituicao) {
		this.constituicao = constituicao;
	}
	public int getInteligencia() {
		return inteligencia;
	}
	public void setInteligencia(int inteligencia) {
		this.inteligencia = inteligencia;
	}
	public int getSabedoria() {
		return sabedoria;
	}
	public void setSabedoria(int sabedoria) {
		this.sabedoria = sabedoria;
	}
	public int getCarisma() {
		return carisma;
	}
	public void setCarisma(int carisma) {
		this.carisma = carisma;
	}
	public Integer getPvAtual() {
		return pvAtual;
	}
	public void setPvAtual(Integer pvAtual) {
		this.pvAtual = pvAtual;
	}
	public Integer getPvMax() {
		return pvMax;
	}
	public void setPvMax(Integer pvMax) {
		this.pvMax = pvMax;
	}
	public Integer getPvTemporario() {
		return pvTemporario;
	}
	public void setPvTemporario(Integer pvTemporario) {
		this.pvTemporario = pvTemporario;
	}
	public Integer getCa() {
		return ca;
	}
	public void setCa(Integer ca) {
		this.ca = ca;
	}
	public boolean isInspiracao() {
		return inspiracao;
	}
	public void setInspiracao(boolean inspiracao) {
		this.inspiracao = inspiracao;
	}
	public Map<PericiaDnd, GrauDnd> getPericias() {
		return pericias;
	}
	public void setPericias(Map<PericiaDnd, GrauDnd> pericias) {
		this.pericias = pericias;
	}
}
