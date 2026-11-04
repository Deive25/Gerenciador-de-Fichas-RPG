package dev.spironello.fichasrpg.service;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.dto.SheetPlayerRequest;
import dev.spironello.fichasrpg.dto.SheetPlayerResponse;
import dev.spironello.fichasrpg.model.Pericia;
import dev.spironello.fichasrpg.model.SheetPlayer;

/**
 * Converte entre a entidade e os DTOs, campo a campo. Manual de propósito: são poucos campos
 * e assim não há dependência nova (MapStruct) nem mágica de reflexão.
 * Aqui mora a tradução entre o contrato novo (jogador, vidaAtual, forca...) e a entidade, que
 * ainda tem os nomes antigos (nameJ, lifeA, STR...): renomear a entidade e as colunas é um
 * próximo passo (ADR 0012). As perícias são o mesmo Map<Pericia, Integer> nos dois lados.
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
		entity.setNameJ(request.jogador());
		entity.setNameC(request.personagem());
		entity.setGender(request.genero());
		entity.setAge(request.idade());
		entity.setLifeA(request.vidaAtual());
		entity.setLifeM(request.vidaMaxima());
		entity.setSanA(request.sanidadeAtual());
		entity.setSanM(request.sanidadeMaxima());
		entity.setSTR(request.forca());
		entity.setDEX(request.destreza());
		entity.setCONS(request.constituicao());
		entity.setINT(request.inteligencia());
		entity.setCAR(request.carisma());
		entity.setSAB(request.sabedoria());
		entity.setTAM(request.tamanho());
		entity.setSOR(request.sorte());
		entity.setPOD(request.poder());
		// Substituição completa das perícias: esvazia o mapa e põe só as que vieram com valor.
		// Valor null no pedido = perícia ausente: não gera entrada (a linha some do banco).
		Map<Pericia, Integer> pericias = entity.getPericias();
		pericias.clear();
		if (request.pericias() != null) {
			request.pericias().forEach((pericia, valor) -> {
				if (valor != null) {
					pericias.put(pericia, valor);
				}
			});
		}
	}

	public SheetPlayerResponse toResponse(SheetPlayer entity) {
		// Cópia em EnumMap: ordem estável (a do enum) e sem expor a coleção gerenciada pelo Hibernate.
		Map<Pericia, Integer> pericias = new EnumMap<>(Pericia.class);
		pericias.putAll(entity.getPericias());
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
				pericias);
	}
}
