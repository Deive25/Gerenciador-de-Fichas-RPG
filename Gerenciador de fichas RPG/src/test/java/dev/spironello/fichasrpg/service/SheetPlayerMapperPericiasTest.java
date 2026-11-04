package dev.spironello.fichasrpg.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.spironello.fichasrpg.dto.SheetPlayerRequest;
import dev.spironello.fichasrpg.dto.SheetPlayerResponse;
import dev.spironello.fichasrpg.model.Pericia;
import dev.spironello.fichasrpg.model.SheetPlayer;

/**
 * Garante que o mapper leva cada perícia do objeto "pericias" do JSON para a perícia certa da
 * entidade, e de volta, nos dois sentidos. Unitário: sem Spring e sem Docker.
 */
@DisplayName("SheetPlayerMapper (perícias <-> objeto pericias)")
class SheetPlayerMapperPericiasTest {

	private final SheetPlayerMapper mapper = new SheetPlayerMapper();

	@Test
	@DisplayName("cada perícia do pedido vai para a mesma perícia da entidade e volta com o mesmo código na resposta")
	void cadaChaveVaiParaASuaPericia() {
		Map<Pericia, Integer> doPedido = new EnumMap<>(Pericia.class);
		for (Pericia p : Pericia.values()) {
			doPedido.put(p, (p.ordinal() + 1) * 10);
		}
		SheetPlayer ficha = mapper.toEntity(requestComPericias(doPedido));
		SheetPlayerResponse resposta = mapper.toResponse(ficha);

		assertThat(ficha.getPericias()).hasSize(23);
		for (Pericia p : Pericia.values()) {
			assertThat(ficha.getPericias()).as("%s", p).containsEntry(p, doPedido.get(p));
			assertThat(resposta.pericias()).as("resposta %s", p).containsEntry(p, doPedido.get(p));
		}
	}

	@Test
	@DisplayName("perícia sem valor não gera entrada e não sai na resposta; substituir remove a que veio null")
	void nuloNaoGeraEntradaERemoveAAnterior() {
		Map<Pericia, Integer> todas = new EnumMap<>(Pericia.class);
		for (Pericia p : Pericia.values()) {
			todas.put(p, 10);
		}
		SheetPlayer ficha = mapper.toEntity(requestComPericias(todas));
		Map<Pericia, Integer> todasNulas = new EnumMap<>(Pericia.class);
		for (Pericia p : Pericia.values()) {
			todasNulas.put(p, null);
		}

		mapper.copyTo(requestComPericias(todasNulas), ficha);

		assertThat(ficha.getPericias()).isEmpty();
		SheetPlayerResponse resposta = mapper.toResponse(ficha);
		assertThat(resposta.pericias()).isEmpty();
	}

	private SheetPlayerRequest requestComPericias(Map<Pericia, Integer> pericias) {
		return new SheetPlayerRequest("x", "x", "x",
				null, null, null, null, null, // idade, vida e sanidade
				null, null, null, null, null, null, null, null, null, // 9 atributos
				pericias);
	}
}
