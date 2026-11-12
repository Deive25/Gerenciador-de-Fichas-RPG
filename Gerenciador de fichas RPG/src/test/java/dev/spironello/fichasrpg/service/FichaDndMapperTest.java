package dev.spironello.fichasrpg.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.spironello.fichasrpg.dto.EspacoDnd;
import dev.spironello.fichasrpg.dto.FichaDndRequest;
import dev.spironello.fichasrpg.dto.FichaDndResponse;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.dto.MagiaResponse;
import dev.spironello.fichasrpg.dto.RecursoValor;
import dev.spironello.fichasrpg.model.AtributoDnd;
import dev.spironello.fichasrpg.model.EdicaoDnd;
import dev.spironello.fichasrpg.model.EspacoMagia;
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
			Set<AtributoDnd> salvaguardas, Map<PericiaDnd, GrauDnd> pericias, AtributoDnd atributoConjuracao) {
		return new FichaDndRequest("Arthur", "Ana", "F", 30, EdicaoDnd.D2014, "Mago", "Elfo", "Sábio", 5,
				atributos(), recursos, 13, inspiracao, salvaguardas, pericias, atributoConjuracao);
	}

	private static FichaDndRequest pedido(Map<RecursoDnd, RecursoValor> recursos, Boolean inspiracao,
			Set<AtributoDnd> salvaguardas, Map<PericiaDnd, GrauDnd> pericias) {
		return pedido(recursos, inspiracao, salvaguardas, pericias, null);
	}

	@Test
	@DisplayName("cada atributo do pedido vai para o seu campo e volta com o mesmo código")
	void seisAtributos() {
		FichaDnd ficha = mapper.criar(pedido(null, null, null, null));
		FichaDndResponse resposta = mapper.resposta(ficha, PartesComunsDeTeste.VAZIAS, ListasDeTeste.VAZIAS_DND);

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
		FichaDndResponse resposta = mapper.resposta(ficha, PartesComunsDeTeste.VAZIAS, ListasDeTeste.VAZIAS_DND);

		assertThat(ficha.getPvAtual()).isEqualTo(7);
		assertThat(ficha.getPvMax()).isEqualTo(9);
		assertThat(ficha.getPvTemporario()).isEqualTo(3);
		assertThat(resposta.recursos().get(RecursoDnd.PV)).isEqualTo(new RecursoValor(7, 9));
		assertThat(resposta.recursos().get(RecursoDnd.PV_TEMP)).isEqualTo(new RecursoValor(3, null));

		FichaDndResponse vazia = mapper.resposta(mapper.criar(pedido(null, null, null, null)), PartesComunsDeTeste.VAZIAS, ListasDeTeste.VAZIAS_DND);
		assertThat(vazia.recursos()).containsOnlyKeys(RecursoDnd.PV, RecursoDnd.PV_TEMP);
		assertThat(vazia.recursos().get(RecursoDnd.PV)).isEqualTo(new RecursoValor(null, null));
	}

	@Test
	@DisplayName("inspiracao nula vira false; salvaguardas nulas viram vazio; salvaguardas saem na ordem do enum")
	void nulosEOrdem() {
		FichaDndResponse padrao = mapper.resposta(mapper.criar(pedido(null, null, null, null)), PartesComunsDeTeste.VAZIAS, ListasDeTeste.VAZIAS_DND);
		assertThat(padrao.inspiracao()).isFalse();
		assertThat(padrao.salvaguardas()).isEmpty();

		FichaDndResponse com = mapper.resposta(mapper.criar(pedido(null, true,
				Set.of(AtributoDnd.CAR, AtributoDnd.FOR, AtributoDnd.INT), null)), PartesComunsDeTeste.VAZIAS, ListasDeTeste.VAZIAS_DND);
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
		assertThat(mapper.resposta(ficha, PartesComunsDeTeste.VAZIAS, ListasDeTeste.VAZIAS_DND).pericias()).isEqualTo(ficha.getPericias());

		mapper.copiar(pedido(null, null, null, Map.of(PericiaDnd.ARCANISMO, GrauDnd.ESPECIALISTA)), ficha);
		assertThat(ficha.getPericias()).containsOnly(Map.entry(PericiaDnd.ARCANISMO, GrauDnd.ESPECIALISTA));
	}

	@Test
	@DisplayName("atributo de conjuração: vai para a ficha e volta na resposta; ausente no pedido vira nulo")
	void atributoDeConjuracao() {
		FichaDnd ficha = mapper.criar(pedido(null, null, null, null, AtributoDnd.SAB));

		assertThat(ficha.getAtributoConjuracao()).isEqualTo(AtributoDnd.SAB);
		assertThat(mapper.resposta(ficha, PartesComunsDeTeste.VAZIAS, ListasDeTeste.VAZIAS_DND).atributoConjuracao())
				.isEqualTo(AtributoDnd.SAB);

		mapper.copiar(pedido(null, null, null, null), ficha);
		assertThat(ficha.getAtributoConjuracao()).isNull();
		assertThat(mapper.resposta(ficha, PartesComunsDeTeste.VAZIAS, ListasDeTeste.VAZIAS_DND).atributoConjuracao())
				.isNull();
	}

	@Test
	@DisplayName("copiar não toca nos espaços de magia da ficha (só o PATCH de espaços grava neles)")
	void copiarNaoTocaNosEspacos() {
		FichaDnd ficha = mapper.criar(pedido(null, null, null, null));
		ficha.getEspacos().put(2, new EspacoMagia(3, 1));

		mapper.copiar(pedido(null, null, null, null, AtributoDnd.INT), ficha);

		assertThat(ficha.getEspacos()).containsOnly(Map.entry(2, new EspacoMagia(3, 1)));
	}

	@Test
	@DisplayName("a resposta leva adiante as magias e os espaços das listas")
	void respostaLevaMagiasEEspacos() {
		FichaDnd ficha = mapper.criar(pedido(null, null, null, null));
		MagiaResponse luz = new MagiaResponse(1L, "Luz", 0, null, null, null, null, null, false, false, true, null);
		Map<Integer, EspacoDnd> espacos = Map.of(1, new EspacoDnd(4, 1));

		FichaDndResponse resposta = mapper.resposta(ficha, PartesComunsDeTeste.VAZIAS,
				new ListasDnd(List.of(), List.of(), List.of(luz), espacos));

		assertThat(resposta.magias()).containsExactly(luz);
		assertThat(resposta.espacos()).isEqualTo(espacos);
	}

	@Test
	@DisplayName("EspacosDaFicha traz sempre os nove níveis na ordem; nível sem linha vale 0 de 0")
	void espacosDaFichaTemNoveNiveis() {
		FichaDnd ficha = new FichaDnd();
		ficha.getEspacos().put(9, new EspacoMagia(1, 1));
		ficha.getEspacos().put(3, new EspacoMagia(2, 0));

		Map<Integer, EspacoDnd> espacos = EspacosDaFicha.de(ficha);

		assertThat(espacos.keySet()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
		assertThat(espacos.get(3)).isEqualTo(new EspacoDnd(2, 0));
		assertThat(espacos.get(9)).isEqualTo(new EspacoDnd(1, 1));
		assertThat(espacos.get(1)).isEqualTo(new EspacoDnd(0, 0));
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
