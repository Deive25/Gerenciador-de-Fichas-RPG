package Atos.SpringAPI.Models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
	@Column(nullable=true)
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
	/*PERICIAS*/
	@Column(nullable=true)
	private Integer atribute1;
	@Column(nullable=true)
	private Integer atribute2;
	@Column(nullable=true)
	private Integer atribute3;
	@Column(nullable=true)
	private Integer atribute4;
	@Column(nullable=true)
	private Integer atribute5;
	@Column(nullable=true)
	private Integer atribute6;
	@Column(nullable=true)
	private Integer atribute7;
	@Column(nullable=true)
	private Integer atribute8;
	@Column(nullable=true)
	private Integer atribute9;
	@Column(nullable=true)
	private Integer atribute10;
	@Column(nullable=true)
	private Integer atribute11;
	@Column(nullable=true)
	private Integer atribute12;
	@Column(nullable=true)
	private Integer atribute13;
	@Column(nullable=true)
	private Integer atribute14;
	@Column(nullable=true)
	private Integer atribute15;
	@Column(nullable=true)
	private Integer atribute16;
	@Column(nullable=true)
	private Integer atribute17;
	@Column(nullable=true)
	private Integer atribute18;
	@Column(nullable=true)
	private Integer atribute19;
	@Column(nullable=true)
	private Integer atribute20;
	@Column(nullable=true)
	private Integer atribute21;
	@Column(nullable=true)
	private Integer atribute22;
	@Column(nullable=true)
	private Integer atribute23;
	
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
	public Integer getAtribute1() {
		return atribute1;
	}
	public void setAtribute1(Integer atribute1) {
		this.atribute1 = atribute1;
	}
	public Integer getAtribute2() {
		return atribute2;
	}
	public void setAtribute2(Integer atribute2) {
		this.atribute2 = atribute2;
	}
	public Integer getAtribute3() {
		return atribute3;
	}
	public void setAtribute3(Integer atribute3) {
		this.atribute3 = atribute3;
	}
	public Integer getAtribute4() {
		return atribute4;
	}
	public void setAtribute4(Integer atribute4) {
		this.atribute4 = atribute4;
	}
	public Integer getAtribute5() {
		return atribute5;
	}
	public void setAtribute5(Integer atribute5) {
		this.atribute5 = atribute5;
	}
	public Integer getAtribute6() {
		return atribute6;
	}
	public void setAtribute6(Integer atribute6) {
		this.atribute6 = atribute6;
	}
	public Integer getAtribute7() {
		return atribute7;
	}
	public void setAtribute7(Integer atribute7) {
		this.atribute7 = atribute7;
	}
	public Integer getAtribute8() {
		return atribute8;
	}
	public void setAtribute8(Integer atribute8) {
		this.atribute8 = atribute8;
	}
	public Integer getAtribute9() {
		return atribute9;
	}
	public void setAtribute9(Integer atribute9) {
		this.atribute9 = atribute9;
	}
	public Integer getAtribute10() {
		return atribute10;
	}
	public void setAtribute10(Integer atribute10) {
		this.atribute10 = atribute10;
	}
	public Integer getAtribute11() {
		return atribute11;
	}
	public void setAtribute11(Integer atribute11) {
		this.atribute11 = atribute11;
	}
	public Integer getAtribute12() {
		return atribute12;
	}
	public void setAtribute12(Integer atribute12) {
		this.atribute12 = atribute12;
	}
	public Integer getAtribute13() {
		return atribute13;
	}
	public void setAtribute13(Integer atribute13) {
		this.atribute13 = atribute13;
	}
	public Integer getAtribute14() {
		return atribute14;
	}
	public void setAtribute14(Integer atribute14) {
		this.atribute14 = atribute14;
	}
	public Integer getAtribute15() {
		return atribute15;
	}
	public void setAtribute15(Integer atribute15) {
		this.atribute15 = atribute15;
	}
	public Integer getAtribute16() {
		return atribute16;
	}
	public void setAtribute16(Integer atribute16) {
		this.atribute16 = atribute16;
	}
	public Integer getAtribute17() {
		return atribute17;
	}
	public void setAtribute17(Integer atribute17) {
		this.atribute17 = atribute17;
	}
	public Integer getAtribute18() {
		return atribute18;
	}
	public void setAtribute18(Integer atribute18) {
		this.atribute18 = atribute18;
	}
	public Integer getAtribute19() {
		return atribute19;
	}
	public void setAtribute19(Integer atribute19) {
		this.atribute19 = atribute19;
	}
	public Integer getAtribute20() {
		return atribute20;
	}
	public void setAtribute20(Integer atribute20) {
		this.atribute20 = atribute20;
	}
	public Integer getAtribute21() {
		return atribute21;
	}
	public void setAtribute21(Integer atribute21) {
		this.atribute21 = atribute21;
	}
	public Integer getAtribute22() {
		return atribute22;
	}
	public void setAtribute22(Integer atribute22) {
		this.atribute22 = atribute22;
	}
	public Integer getAtribute23() {
		return atribute23;
	}
	public void setAtribute23(Integer atribute23) {
		this.atribute23 = atribute23;
	}

}
