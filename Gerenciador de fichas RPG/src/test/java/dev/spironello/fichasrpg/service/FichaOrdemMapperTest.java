package dev.spironello.fichasrpg.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.spironello.fichasrpg.dto.FichaOrdemRequest;
import dev.spironello.fichasrpg.dto.FichaOrdemResponse;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.dto.RecursoResumo;
import dev.spironello.fichasrpg.dto.RitualResponse;
import dev.spironello.fichasrpg.dto.RecursoValor;
import dev.spironello.fichasrpg.model.AtributoOrdem;
import dev.spironello.fichasrpg.model.ElementoRitual;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.GrauOrdem;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.model.RecursoOrdem;

/**
 * Garante que o mapper leva cada perícia, atributo e recurso do JSON para o campo certo da entidade, e de
 * volta, nos dois sentidos. Unitário: sem Spring e sem Docker.
 */
@DisplayName("FichaOrdemMapper (pedido <-> entidade <-> resposta)")
class FichaOrdemMapperTest {

	private final FichaOrdemMapper mapper = new FichaOrdemMapper();

	@Test
	@DisplayName("cada perícia do pedido vai para a mesma perícia da entidade e volta com o mesmo código na resposta")
	void cadaChaveVaiParaASuaPericia() {
		Map<PericiaOrdem, GrauOrdem> doPedido = new EnumMap<>(PericiaOrdem.class);
		for (PericiaOrdem p : PericiaOrdem.values()) {
			doPedido.put(p, p.ordinal() % 2 == 0 ? GrauOrdem.TREINADO : GrauOrdem.VETERANO);
		}
		FichaOrdem ficha = mapper.criar(pedido(doPedido));
		FichaOrdemResponse resposta = mapper.resposta(ficha, PartesComunsDeTeste.VAZIAS, ListasDeTeste.VAZIAS_ORDEM);

		assertThat(ficha.getPericias()).hasSize(28);
		for (PericiaOrdem p : PericiaOrdem.values()) {
			assertThat(ficha.getPericias()).as("%s", p).containsEntry(p, doPedido.get(p));
			assertThat(resposta.pericias()).as("resposta %s", p).containsEntry(p, doPedido.get(p));
		}
	}

	@Test
	@DisplayName("perícia sem grau não gera entrada e não sai na resposta; substituir remove a que veio null")
	void nuloNaoGeraEntradaERemoveAAnterior() {
		Map<PericiaOrdem, GrauOrdem> todas = new EnumMap<>(PericiaOrdem.class);
		Map<PericiaOrdem, GrauOrdem> todasNulas = new EnumMap<>(PericiaOrdem.class);
		for (PericiaOrdem p : PericiaOrdem.values()) {
			todas.put(p, GrauOrdem.TREINADO);
			todasNulas.put(p, null);
		}
		FichaOrdem ficha = mapper.criar(pedido(todas));

		mapper.copiar(pedido(todasNulas), ficha);

		assertThat(ficha.getPericias()).isEmpty();
		assertThat(mapper.resposta(ficha, PartesComunsDeTeste.VAZIAS, ListasDeTeste.VAZIAS_ORDEM).pericias()).isEmpty();
	}

	@Test
	@DisplayName("atributos e recursos vão para os campos certos e voltam; sem recursos, PV, SAN e PE vêm vazios")
	void atributosERecursos() {
		Map<AtributoOrdem, Integer> atributos = new EnumMap<>(AtributoOrdem.class);
		atributos.put(AtributoOrdem.AGI, 1);
		atributos.put(AtributoOrdem.FOR, 2);
		atributos.put(AtributoOrdem.INT, 3);
		atributos.put(AtributoOrdem.PRE, 4);
		atributos.put(AtributoOrdem.VIG, 5);
		Map<RecursoOrdem, RecursoValor> recursos = Map.of(
				RecursoOrdem.PV, new RecursoValor(10, 12),
				RecursoOrdem.SAN, new RecursoValor(null, 20));

		FichaOrdem ficha = mapper.criar(new FichaOrdemRequest("j", "p", "g", null, null, null, 5, atributos, recursos, null));

		assertThat(ficha.getAgilidade()).isEqualTo(1);
		assertThat(ficha.getForca()).isEqualTo(2);
		assertThat(ficha.getIntelecto()).isEqualTo(3);
		assertThat(ficha.getPresenca()).isEqualTo(4);
		assertThat(ficha.getVigor()).isEqualTo(5);
		assertThat(ficha.getPvAtual()).isEqualTo(10);
		assertThat(ficha.getPvMax()).isEqualTo(12);
		assertThat(ficha.getSanAtual()).isNull();
		assertThat(ficha.getSanMax()).isEqualTo(20);
		assertThat(ficha.getPeAtual()).isNull();

		FichaOrdemResponse resposta = mapper.resposta(ficha, PartesComunsDeTeste.VAZIAS, ListasDeTeste.VAZIAS_ORDEM);
		assertThat(resposta.atributos()).containsExactly(
				Map.entry(AtributoOrdem.AGI, 1), Map.entry(AtributoOrdem.FOR, 2), Map.entry(AtributoOrdem.INT, 3),
				Map.entry(AtributoOrdem.PRE, 4), Map.entry(AtributoOrdem.VIG, 5));
		assertThat(resposta.recursos()).containsExactly(
				Map.entry(RecursoOrdem.PV, new RecursoValor(10, 12)),
				Map.entry(RecursoOrdem.SAN, new RecursoValor(null, 20)),
				Map.entry(RecursoOrdem.PE, new RecursoValor(null, null)));
	}

	@Test
	@DisplayName("a resposta leva adiante os rituais da lista")
	void respostaLevaOsRituais() {
		FichaOrdem ficha = mapper.criar(pedido(null));
		RitualResponse ritual = new RitualResponse(1L, "Cicatrização", 1, ElementoRitual.SANGUE, null, null, null,
				null, null, null);

		FichaOrdemResponse resposta = mapper.resposta(ficha, PartesComunsDeTeste.VAZIAS,
				new ListasOrdem(List.of(), List.of(ritual)));

		assertThat(resposta.rituais()).containsExactly(ritual);
		assertThat(mapper.resposta(ficha, PartesComunsDeTeste.VAZIAS, ListasDeTeste.VAZIAS_ORDEM).rituais())
				.isEmpty();
	}

	@Test
	@DisplayName("o resumo traz PV, SAN e PE nessa ordem, sem atributos nem perícias")
	void resumo() {
		FichaOrdem ficha = mapper.criar(pedido(Map.of(PericiaOrdem.LUTA, GrauOrdem.TREINADO)));
		ficha.setId(3L);
		ficha.setPvAtual(8);
		ficha.setPvMax(10);
		ficha.setPeMax(4);

		FichaResumoResponse resumo = mapper.resumo(ficha);

		assertThat(resumo.id()).isEqualTo(3L);
		assertThat(resumo.sistema().name()).isEqualTo("ORDEM_PARANORMAL");
		assertThat(resumo.recursos()).containsExactly(
				new RecursoResumo("PV", 8, 10),
				new RecursoResumo("SAN", null, null),
				new RecursoResumo("PE", null, 4));
	}

	private static FichaOrdemRequest pedido(Map<PericiaOrdem, GrauOrdem> pericias) {
		Map<AtributoOrdem, Integer> atributos = new EnumMap<>(AtributoOrdem.class);
		for (AtributoOrdem atributo : AtributoOrdem.values()) {
			atributos.put(atributo, 1);
		}
		return new FichaOrdemRequest("x", "x", "x", null, null, null, 5, atributos, null, pericias);
	}
}
