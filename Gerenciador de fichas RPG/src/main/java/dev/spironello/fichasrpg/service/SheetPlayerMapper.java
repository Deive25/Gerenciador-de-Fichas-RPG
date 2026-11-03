package dev.spironello.fichasrpg.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.dto.SheetPlayerRequest;
import dev.spironello.fichasrpg.dto.SheetPlayerResponse;
import dev.spironello.fichasrpg.model.Pericia;
import dev.spironello.fichasrpg.model.SheetPlayer;

/**
 * Converte entre a entidade e os DTOs, campo a campo. Manual de propósito: são só 41 campos
 * e assim não há dependência nova (MapStruct) nem mágica de reflexão.
 * O JSON continua com atribute1..atribute23; na entidade as perícias são um Map<Pericia, Integer>
 * (tabela ficha_pericia). O vínculo atributeN <-> Pericia é o do enum (Pericia.getCampoLegado) e
 * está fixado nos testes.
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
		// Substituição completa das perícias: esvazia o mapa e põe só as que vieram com valor.
		// Perícia null no pedido não gera entrada (e, portanto, a linha some do banco).
		Map<Pericia, Integer> pericias = entity.getPericias();
		pericias.clear();
		put(pericias, Pericia.ACROBACIA, request.atribute1());
		put(pericias, Pericia.ATLETISMO, request.atribute2());
		put(pericias, Pericia.ADESTRAR_ANIMAIS, request.atribute3());
		put(pericias, Pericia.ATUACAO_LABIA, request.atribute4());
		put(pericias, Pericia.CHARME, request.atribute5());
		put(pericias, Pericia.ESQUIVA, request.atribute6());
		put(pericias, Pericia.FURTIVIDADE, request.atribute7());
		put(pericias, Pericia.LUTA_CORPO_A_CORPO, request.atribute8());
		put(pericias, Pericia.LUTA_ARMA_BRANCA, request.atribute9());
		put(pericias, Pericia.INTIMIDACAO, request.atribute10());
		put(pericias, Pericia.INTUICAO, request.atribute11());
		put(pericias, Pericia.INVESTIGACAO, request.atribute12());
		put(pericias, Pericia.ARMAS_DE_FOGO_LEVES, request.atribute13());
		put(pericias, Pericia.ARMAS_DE_FOGO_MEDIAS, request.atribute14());
		put(pericias, Pericia.ARMAS_DE_FOGO_PESADAS, request.atribute15());
		put(pericias, Pericia.PERCEPCAO, request.atribute16());
		put(pericias, Pericia.USAR_BIBLIOTECAS, request.atribute17());
		put(pericias, Pericia.HISTORIA, request.atribute18());
		put(pericias, Pericia.CURA, request.atribute19());
		put(pericias, Pericia.MALANDRAGEM, request.atribute20());
		put(pericias, Pericia.OCULTISMO, request.atribute21());
		put(pericias, Pericia.PSICOLOGIA, request.atribute22());
		put(pericias, Pericia.ARREMESSAR, request.atribute23());
	}

	private static void put(Map<Pericia, Integer> pericias, Pericia pericia, Integer valor) {
		if (valor != null) {
			pericias.put(pericia, valor);
		}
	}

	public SheetPlayerResponse toResponse(SheetPlayer entity) {
		Map<Pericia, Integer> p = entity.getPericias();
		// Perícia sem entrada no mapa sai como null no JSON.
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
				p.get(Pericia.ACROBACIA),
				p.get(Pericia.ATLETISMO),
				p.get(Pericia.ADESTRAR_ANIMAIS),
				p.get(Pericia.ATUACAO_LABIA),
				p.get(Pericia.CHARME),
				p.get(Pericia.ESQUIVA),
				p.get(Pericia.FURTIVIDADE),
				p.get(Pericia.LUTA_CORPO_A_CORPO),
				p.get(Pericia.LUTA_ARMA_BRANCA),
				p.get(Pericia.INTIMIDACAO),
				p.get(Pericia.INTUICAO),
				p.get(Pericia.INVESTIGACAO),
				p.get(Pericia.ARMAS_DE_FOGO_LEVES),
				p.get(Pericia.ARMAS_DE_FOGO_MEDIAS),
				p.get(Pericia.ARMAS_DE_FOGO_PESADAS),
				p.get(Pericia.PERCEPCAO),
				p.get(Pericia.USAR_BIBLIOTECAS),
				p.get(Pericia.HISTORIA),
				p.get(Pericia.CURA),
				p.get(Pericia.MALANDRAGEM),
				p.get(Pericia.OCULTISMO),
				p.get(Pericia.PSICOLOGIA),
				p.get(Pericia.ARREMESSAR));
	}
}
