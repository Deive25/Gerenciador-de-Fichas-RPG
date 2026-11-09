package dev.spironello.fichasrpg.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.spironello.fichasrpg.dto.FichaDndRequest;
import dev.spironello.fichasrpg.dto.FichaDndResponse;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.dto.RecursoValor;
import dev.spironello.fichasrpg.model.AtributoDnd;
import dev.spironello.fichasrpg.model.EdicaoDnd;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.GrauDnd;
import dev.spironello.fichasrpg.model.PericiaDnd;
import dev.spironello.fichasrpg.model.RecursoDnd;

/** O mapper leva cada campo do pedido para o campo certo da entidade e de volta. Unitário: sem Spring nem Docker. */
@DisplayName("FichaDndMapper (pedido <-> entidade <-> resposta)")
class FichaDndMapperTest {

	private final FichaDndMapper mapper = new FichaDndMapper();

	private static Map<AtributoDnd, Integer> atributos() {
		Map<AtributoDnd, Integer> atributos = new EnumMap<>(AtributoDnd.class);
		for (AtributoDnd atributo : AtributoDnd.values()) {
			atributos.put(atributo, 8 + atributo.ordinal());
		}
		return atributos;
	}

	private static FichaDndRequest pedido(Map<RecursoDnd, RecursoValor> recursos, Boolean inspiracao,
			Set<AtributoDnd> salvaguardas, Map<PericiaDnd, GrauDnd> pericias) {
		return new FichaDndRequest("Arthur", "Ana", "F", 30, EdicaoDnd.D2014, "Mago", "Elfo", "Sábio", 5,
				atributos(), recursos, 13, inspiracao, salvaguardas, pericias);
	}

	@Test
	@DisplayName("cada atributo do pedido vai para o seu campo e volta com o mesmo código")
	void seisAtributos() {
		FichaDnd ficha = mapper.criar(pedido(null, null, null, null));
		FichaDndResponse resposta = mapper.resposta(ficha, PartesComunsDeTeste.VAZIAS);

		assertThat(ficha.getForca()).isEqualTo(8);
		assertThat(ficha.getDestreza()).isEqualTo(9);
		assertThat(ficha.getConstituicao()).isEqualTo(10);
		assertThat(ficha.getInteligencia()).isEqualTo(11);
		assertThat(ficha.getSabedoria()).isEqualTo(12);
		assertThat(ficha.getCarisma()).isEqualTo(13);
		assertThat(resposta.atributos()).isEqualTo(atributos());
		assertThat(ficha.getEdicao()).isEqualTo(EdicaoDnd.D2014);
		assertThat(ficha.getNivel()).isEqualTo(5);
		assertThat(ficha.getCa()).isEqualTo(13);
	}

	@Test
	@DisplayName("recursos: PV e PV_TEMP vão para os campos certos; a resposta traz sempre os dois")
	void recursos() {
		FichaDnd ficha = mapper.criar(pedido(
				Map.of(RecursoDnd.PV, new RecursoValor(7, 9), RecursoDnd.PV_TEMP, new RecursoValor(3, null)),
				null, null, null));
		FichaDndResponse resposta = mapper.resposta(ficha, PartesComunsDeTeste.VAZIAS);

		assertThat(ficha.getPvAtual()).isEqualTo(7);
		assertThat(ficha.getPvMax()).isEqualTo(9);
		assertThat(ficha.getPvTemporario()).isEqualTo(3);
		assertThat(resposta.recursos().get(RecursoDnd.PV)).isEqualTo(new RecursoValor(7, 9));
		assertThat(resposta.recursos().get(RecursoDnd.PV_TEMP)).isEqualTo(new RecursoValor(3, null));

		FichaDndResponse vazia = mapper.resposta(mapper.criar(pedido(null, null, null, null)), PartesComunsDeTeste.VAZIAS);
		assertThat(vazia.recursos()).containsOnlyKeys(RecursoDnd.PV, RecursoDnd.PV_TEMP);
		assertThat(vazia.recursos().get(RecursoDnd.PV)).isEqualTo(new RecursoValor(null, null));
	}

	@Test
	@DisplayName("inspiracao nula vira false; salvaguardas nulas viram vazio; salvaguardas saem na ordem do enum")
	void nulosEOrdem() {
		FichaDndResponse padrao = mapper.resposta(mapper.criar(pedido(null, null, null, null)), PartesComunsDeTeste.VAZIAS);
		assertThat(padrao.inspiracao()).isFalse();
		assertThat(padrao.salvaguardas()).isEmpty();

		FichaDndResponse com = mapper.resposta(mapper.criar(pedido(null, true,
				Set.of(AtributoDnd.CAR, AtributoDnd.FOR, AtributoDnd.INT), null)), PartesComunsDeTeste.VAZIAS);
		assertThat(com.inspiracao()).isTrue();
		assertThat(com.salvaguardas()).containsExactly(AtributoDnd.FOR, AtributoDnd.INT, AtributoDnd.CAR);
	}

	@Test
	@DisplayName("perícias: só as com grau; substituir remove as anteriores")
	void pericias() {
		Map<PericiaDnd, GrauDnd> doPedido = new EnumMap<>(PericiaDnd.class);
		for (PericiaDnd p : PericiaDnd.values()) {
			doPedido.put(p, p.ordinal() % 2 == 0 ? GrauDnd.PROFICIENTE : GrauDnd.ESPECIALISTA);
		}
		doPedido.put(PericiaDnd.ATLETISMO, null);
		FichaDnd ficha = mapper.criar(pedido(null, null, null, doPedido));

		assertThat(ficha.getPericias()).hasSize(17).doesNotContainKey(PericiaDnd.ATLETISMO);
		assertThat(mapper.resposta(ficha, PartesComunsDeTeste.VAZIAS).pericias()).isEqualTo(ficha.getPericias());

		mapper.copiar(pedido(null, null, null, Map.of(PericiaDnd.ARCANISMO, GrauDnd.ESPECIALISTA)), ficha);
		assertThat(ficha.getPericias()).containsOnly(Map.entry(PericiaDnd.ARCANISMO, GrauDnd.ESPECIALISTA));
	}

	@Test
	@DisplayName("resumo traz só o PV e não toca nas perícias")
	void resumoSoComPv() {
		FichaDnd ficha = mapper.criar(pedido(Map.of(RecursoDnd.PV, new RecursoValor(7, 9)), null, null, null));
		ficha.setPericias(null); // se o resumo lesse as perícias, falharia aqui

		FichaResumoResponse resumo = mapper.resumo(ficha);

		assertThat(resumo.recursos()).hasSize(1);
		assertThat(resumo.recursos().get(0).codigo()).isEqualTo("PV");
		assertThat(resumo.recursos().get(0).atual()).isEqualTo(7);
		assertThat(resumo.recursos().get(0).maximo()).isEqualTo(9);
	}
}
