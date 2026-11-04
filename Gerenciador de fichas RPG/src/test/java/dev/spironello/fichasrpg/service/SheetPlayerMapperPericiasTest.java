package dev.spironello.fichasrpg.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.spironello.fichasrpg.dto.SheetPlayerRequest;
import dev.spironello.fichasrpg.dto.SheetPlayerResponse;
import dev.spironello.fichasrpg.model.Pericia;
import dev.spironello.fichasrpg.model.SheetPlayer;

/**
 * Garante que o mapper liga cada chave atributeN do JSON à perícia certa do enum
 * (Pericia.getCampoLegado), nos dois sentidos. Unitário: sem Spring e sem Docker.
 */
@DisplayName("SheetPlayerMapper (perícias <-> atributeN)")
class SheetPlayerMapperPericiasTest {

	private final SheetPlayerMapper mapper = new SheetPlayerMapper();

	@Test
	@DisplayName("atributeN do pedido vai para a perícia do enum e volta na mesma chave do JSON")
	void cadaChaveVaiParaASuaPericia() throws Exception {
		SheetPlayerRequest request = requestComPericiasDistintas();
		SheetPlayer ficha = mapper.toEntity(request);
		SheetPlayerResponse resposta = mapper.toResponse(ficha);

		assertThat(ficha.getPericias()).hasSize(23);
		for (Pericia p : Pericia.values()) {
			Object doPedido = componente(SheetPlayerRequest.class, p.getCampoLegado()).getAccessor().invoke(request);
			Object daResposta = componente(SheetPlayerResponse.class, p.getCampoLegado()).getAccessor().invoke(resposta);
			assertThat(ficha.getPericias()).as("%s", p).containsEntry(p, (Integer) doPedido);
			assertThat(daResposta).as("resposta %s", p.getCampoLegado()).isEqualTo(doPedido);
		}
	}

	@Test
	@DisplayName("perícia sem valor não gera entrada e sai como null; substituir remove a que veio null")
	void nuloNaoGeraEntradaERemoveAAnterior() {
		SheetPlayer ficha = mapper.toEntity(requestComPericiasDistintas());
		SheetPlayerRequest semPericias = requestComPericias(false);

		mapper.copyTo(semPericias, ficha);

		assertThat(ficha.getPericias()).isEmpty();
		SheetPlayerResponse resposta = mapper.toResponse(ficha);
		assertThat(resposta.atribute1()).isNull();
		assertThat(resposta.atribute23()).isNull();
	}

	private SheetPlayerRequest requestComPericiasDistintas() {
		return requestComPericias(true);
	}

	/** Pedido com atributeN = N * 10 (ou todas null), montado por reflexão. */
	private SheetPlayerRequest requestComPericias(boolean preenchidas) {
		try {
			RecordComponent[] campos = SheetPlayerRequest.class.getRecordComponents();
			Class<?>[] tipos = new Class<?>[campos.length];
			Object[] valores = new Object[campos.length];
			for (int i = 0; i < campos.length; i++) {
				tipos[i] = campos[i].getType();
				String nome = campos[i].getName();
				if (tipos[i] == String.class) {
					valores[i] = "x";
				} else if (nome.startsWith("atribute")) {
					valores[i] = preenchidas ? Integer.valueOf(Integer.parseInt(nome.substring(8)) * 10) : null;
				}
			}
			return SheetPlayerRequest.class.getDeclaredConstructor(tipos).newInstance(valores);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private RecordComponent componente(Class<?> tipo, String nome) {
		for (RecordComponent c : tipo.getRecordComponents()) {
			if (c.getName().equals(nome)) {
				return c;
			}
		}
		throw new IllegalStateException("sem componente " + nome);
	}
}
