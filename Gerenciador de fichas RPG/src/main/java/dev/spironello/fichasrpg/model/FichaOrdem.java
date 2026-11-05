package dev.spironello.fichasrpg.model;

import java.util.EnumMap;
import java.util.Map;

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
 * Ficha de Ordem Paranormal. Perícia sem entrada no mapa é destreinada. O mapa é carregado sob demanda (lazy);
 * as leituras do repository usam @EntityGraph quando precisam dele, para evitar N+1.
 */
@Entity
@Table(name = "ficha_ordem")
@PrimaryKeyJoinColumn(name = "ficha_id")
@DiscriminatorValue("ORDEM_PARANORMAL")
public class FichaOrdem extends Ficha {

	@Enumerated(EnumType.STRING)
	private ClasseOrdem classe;

	private String origem;

	private int nex;

	private int agilidade;
	private int forca;
	private int intelecto;
	private int presenca;
	private int vigor;

	private Integer pvAtual;
	private Integer pvMax;
	private Integer sanAtual;
	private Integer sanMax;
	private Integer peAtual;
	private Integer peMax;

	@ElementCollection
	@CollectionTable(name = "ficha_ordem_pericia", joinColumns = @JoinColumn(name = "ficha_id"))
	@MapKeyEnumerated(EnumType.STRING)
	@MapKeyColumn(name = "pericia")
	@Enumerated(EnumType.STRING)
	@Column(name = "grau")
	private Map<PericiaOrdem, GrauOrdem> pericias = new EnumMap<>(PericiaOrdem.class);

	@Override
	public Sistema getSistema() {
		return Sistema.ORDEM_PARANORMAL;
	}

	public int getAtributo(AtributoOrdem atributo) {
		return switch (atributo) {
			case AGI -> agilidade;
			case FOR -> forca;
			case INT -> intelecto;
			case PRE -> presenca;
			case VIG -> vigor;
		};
	}

	public void setAtributo(AtributoOrdem atributo, int valor) {
		switch (atributo) {
			case AGI -> agilidade = valor;
			case FOR -> forca = valor;
			case INT -> intelecto = valor;
			case PRE -> presenca = valor;
			case VIG -> vigor = valor;
		}
	}

	public ClasseOrdem getClasse() {
		return classe;
	}
	public void setClasse(ClasseOrdem classe) {
		this.classe = classe;
	}
	public String getOrigem() {
		return origem;
	}
	public void setOrigem(String origem) {
		this.origem = origem;
	}
	public int getNex() {
		return nex;
	}
	public void setNex(int nex) {
		this.nex = nex;
	}
	public int getAgilidade() {
		return agilidade;
	}
	public void setAgilidade(int agilidade) {
		this.agilidade = agilidade;
	}
	public int getForca() {
		return forca;
	}
	public void setForca(int forca) {
		this.forca = forca;
	}
	public int getIntelecto() {
		return intelecto;
	}
	public void setIntelecto(int intelecto) {
		this.intelecto = intelecto;
	}
	public int getPresenca() {
		return presenca;
	}
	public void setPresenca(int presenca) {
		this.presenca = presenca;
	}
	public int getVigor() {
		return vigor;
	}
	public void setVigor(int vigor) {
		this.vigor = vigor;
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
	public Integer getSanAtual() {
		return sanAtual;
	}
	public void setSanAtual(Integer sanAtual) {
		this.sanAtual = sanAtual;
	}
	public Integer getSanMax() {
		return sanMax;
	}
	public void setSanMax(Integer sanMax) {
		this.sanMax = sanMax;
	}
	public Integer getPeAtual() {
		return peAtual;
	}
	public void setPeAtual(Integer peAtual) {
		this.peAtual = peAtual;
	}
	public Integer getPeMax() {
		return peMax;
	}
	public void setPeMax(Integer peMax) {
		this.peMax = peMax;
	}
	public Map<PericiaOrdem, GrauOrdem> getPericias() {
		return pericias;
	}
	public void setPericias(Map<PericiaOrdem, GrauOrdem> pericias) {
		this.pericias = pericias;
	}
}
