package dev.spironello.fichasrpg.model;

import java.util.EnumMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;

@Entity
public class SheetPlayer {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	/*INFORMAÇÕES PESSOAIS*/
	@Column(nullable=false)
	private String nameJ;
	@Column(nullable=false)
	private String nameC;
	@Column(nullable=false)
	private String gender;
	@Column(nullable=true)
	private Integer age;
	@Column(nullable=true)
	private Integer lifeA;
	@Column(nullable=true)
	private Integer lifeM;
	@Column(nullable=true)
	private Integer sanA;
	@Column(nullable=true)
	private Integer sanM;
	/*ATRIBUTOS*/
	@Column(nullable=true)
	private Integer STR;
	@Column(nullable=true)
	private Integer DEX;
	@Column(nullable=true)
	private Integer CONS;
	// "int" e palavra reservada do SQL: precisa de aspas (e so esta coluna precisa).
	@Column(name = "\"int\"", nullable=true)
	private Integer INT;
	@Column(nullable=true)
	private Integer CAR;
	@Column(nullable=true)
	private Integer SAB;
	@Column(nullable=true)
	private Integer TAM;
	@Column(nullable=true)
	private Integer SOR;
	@Column(nullable=true)
	private Integer POD;
	/*
	 * Perícias em tabela própria (ficha_pericia, uma linha por perícia preenchida; migração V2).
	 * Perícia sem valor não tem entrada no mapa. O @ElementCollection é carregado sob demanda (lazy);
	 * as leituras do repository usam @EntityGraph para trazê-lo junto e evitar N+1.
	 */
	@ElementCollection
	@CollectionTable(name = "ficha_pericia", joinColumns = @JoinColumn(name = "ficha_id"))
	@MapKeyEnumerated(EnumType.STRING)
	@MapKeyColumn(name = "pericia")
	@Column(name = "valor")
	private Map<Pericia, Integer> pericias = new EnumMap<>(Pericia.class);
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getNameJ() {
		return nameJ;
	}
	public void setNameJ(String nameJ) {
		this.nameJ = nameJ;
	}
	public String getNameC() {
		return nameC;
	}
	public void setNameC(String nameC) {
		this.nameC = nameC;
	}
	public String getGender() {
		return gender;
	}
	public void setGender(String gender) {
		this.gender = gender;
	}
	public Integer getAge() {
		return age;
	}
	public void setAge(Integer age) {
		this.age = age;
	}
	
	
	public Integer getLifeA() {
		return lifeA;
	}
	public void setLifeA(Integer lifeA) {
		this.lifeA = lifeA;
	}
	public Integer getLifeM() {
		return lifeM;
	}
	public void setLifeM(Integer lifeM) {
		this.lifeM = lifeM;
	}
	public Integer getSanA() {
		return sanA;
	}
	public void setSanA(Integer sanA) {
		this.sanA = sanA;
	}
	public Integer getSanM() {
		return sanM;
	}
	public void setSanM(Integer sanM) {
		this.sanM = sanM;
	}
	public Integer getSTR() {
		return STR;
	}
	public void setSTR(Integer sTR) {
		STR = sTR;
	}
	public Integer getDEX() {
		return DEX;
	}
	public void setDEX(Integer dEX) {
		DEX = dEX;
	}
	public Integer getCONS() {
		return CONS;
	}
	public void setCONS(Integer cONS) {
		CONS = cONS;
	}
	public Integer getINT() {
		return INT;
	}
	public void setINT(Integer iNT) {
		INT = iNT;
	}
	public Integer getCAR() {
		return CAR;
	}
	public void setCAR(Integer cAR) {
		CAR = cAR;
	}
	public Integer getSAB() {
		return SAB;
	}
	public void setSAB(Integer sAB) {
		SAB = sAB;
	}
	public Integer getTAM() {
		return TAM;
	}
	public void setTAM(Integer tAM) {
		TAM = tAM;
	}
	public Integer getSOR() {
		return SOR;
	}
	public void setSOR(Integer sOR) {
		SOR = sOR;
	}
	public Integer getPOD() {
		return POD;
	}
	public void setPOD(Integer pOD) {
		POD = pOD;
	}

	public Map<Pericia, Integer> getPericias() {
		return pericias;
	}
	public void setPericias(Map<Pericia, Integer> pericias) {
		this.pericias = pericias;
	}
}
