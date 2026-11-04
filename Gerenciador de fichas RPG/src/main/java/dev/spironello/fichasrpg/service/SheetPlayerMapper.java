package dev.spironello.fichasrpg.service;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.dto.SheetPlayerRequest;
import dev.spironello.fichasrpg.dto.SheetPlayerResponse;
import dev.spironello.fichasrpg.model.SheetPlayer;

/**
 * Converte entre a entidade e os DTOs, campo a campo. Manual de propósito: são só 41 campos
 * e assim não há dependência nova (MapStruct) nem mágica de reflexão.
 */
@Component
public class SheetPlayerMapper {

	/** Cria uma entidade nova a partir do pedido. O id nunca é copiado: é gerado pelo banco. */
	public SheetPlayer toEntity(SheetPlayerRequest request) {
		SheetPlayer entity = new SheetPlayer();
		copyTo(request, entity);
		return entity;
	}

	/** Substituição completa: copia todos os campos do pedido, inclusive os nulos. Não toca no id. */
	public void copyTo(SheetPlayerRequest request, SheetPlayer entity) {
		entity.setNameJ(request.nameJ());
		entity.setNameC(request.nameC());
		entity.setGender(request.gender());
		entity.setAge(request.age());
		entity.setLifeA(request.lifeA());
		entity.setLifeM(request.lifeM());
		entity.setSanA(request.sanA());
		entity.setSanM(request.sanM());
		entity.setSTR(request.str());
		entity.setDEX(request.dex());
		entity.setCONS(request.cons());
		entity.setINT(request.intel());
		entity.setCAR(request.car());
		entity.setSAB(request.sab());
		entity.setTAM(request.tam());
		entity.setSOR(request.sor());
		entity.setPOD(request.pod());
		entity.setAtribute1(request.atribute1());
		entity.setAtribute2(request.atribute2());
		entity.setAtribute3(request.atribute3());
		entity.setAtribute4(request.atribute4());
		entity.setAtribute5(request.atribute5());
		entity.setAtribute6(request.atribute6());
		entity.setAtribute7(request.atribute7());
		entity.setAtribute8(request.atribute8());
		entity.setAtribute9(request.atribute9());
		entity.setAtribute10(request.atribute10());
		entity.setAtribute11(request.atribute11());
		entity.setAtribute12(request.atribute12());
		entity.setAtribute13(request.atribute13());
		entity.setAtribute14(request.atribute14());
		entity.setAtribute15(request.atribute15());
		entity.setAtribute16(request.atribute16());
		entity.setAtribute17(request.atribute17());
		entity.setAtribute18(request.atribute18());
		entity.setAtribute19(request.atribute19());
		entity.setAtribute20(request.atribute20());
		entity.setAtribute21(request.atribute21());
		entity.setAtribute22(request.atribute22());
		entity.setAtribute23(request.atribute23());
	}

	public SheetPlayerResponse toResponse(SheetPlayer entity) {
		return new SheetPlayerResponse(
				entity.getId(),
				entity.getNameJ(),
				entity.getNameC(),
				entity.getGender(),
				entity.getAge(),
				entity.getLifeA(),
				entity.getLifeM(),
				entity.getSanA(),
				entity.getSanM(),
				entity.getSTR(),
				entity.getDEX(),
				entity.getCONS(),
				entity.getINT(),
				entity.getCAR(),
				entity.getSAB(),
				entity.getTAM(),
				entity.getSOR(),
				entity.getPOD(),
				entity.getAtribute1(),
				entity.getAtribute2(),
				entity.getAtribute3(),
				entity.getAtribute4(),
				entity.getAtribute5(),
				entity.getAtribute6(),
				entity.getAtribute7(),
				entity.getAtribute8(),
				entity.getAtribute9(),
				entity.getAtribute10(),
				entity.getAtribute11(),
				entity.getAtribute12(),
				entity.getAtribute13(),
				entity.getAtribute14(),
				entity.getAtribute15(),
				entity.getAtribute16(),
				entity.getAtribute17(),
				entity.getAtribute18(),
				entity.getAtribute19(),
				entity.getAtribute20(),
				entity.getAtribute21(),
				entity.getAtribute22(),
				entity.getAtribute23());
	}
}
