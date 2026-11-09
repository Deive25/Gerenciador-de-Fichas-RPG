package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import dev.spironello.fichasrpg.model.AtributoDnd;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.GrauDnd;
import dev.spironello.fichasrpg.model.PericiaDnd;
import dev.spironello.fichasrpg.model.GrauOrdem;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.model.TipoHabilidade;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.repository.HabilidadeFichaRepository;

/**
 * Trava o contrato JSON que o frontend Angular consome: o conjunto de chaves da ficha de Ordem e o do
 * resumo da listagem. Lê o corpo com JsonPath para não depender da versão do Jackson.
 */
@DisplayName("Contrato JSON da ficha (consumido pelo frontend)")
class ContratoJsonTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository repository;

	@Autowired
	private HabilidadeFichaRepository habilidades;

	@BeforeEach
	void limparBanco() {
		repository.deleteAll();
	}

	@Test
	@DisplayName("GET /fichas/{id} devolve exatamente as 15 chaves da ficha de Ordem, com mapas por código")
	void chavesDaFicha() throws Exception {
		FichaOrdem ficha = FichasDeTeste.ordem("Ana");
		ficha.setAparencia("Alta");
		ficha.setPvAtual(10);
		ficha.getPericias().put(PericiaOrdem.ACROBACIA, GrauOrdem.TREINADO);
		ficha.getPericias().put(PericiaOrdem.PERCEPCAO, GrauOrdem.TREINADO);
		ficha = repository.save(ficha);
		habilidades.save(FichasDeTeste.habilidade(ficha, "Ataque", TipoHabilidade.CLASSE, "Forte"));

		String corpo = mockMvc.perform(get("/fichas/{id}", ficha.getId()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		Map<String, Object> json = JsonPath.read(corpo, "$");

		List<String> esperadas = List.of(
				"id", "sistema", "jogador", "personagem", "genero", "idade", "retratoVersao",
				"descricao", "habilidades", "classe", "origem", "nex", "atributos", "recursos", "pericias");

		assertThat(json.keySet()).containsExactlyInAnyOrderElementsOf(esperadas);
		assertThat(json.get("sistema")).isEqualTo("ORDEM_PARANORMAL");
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.atributos").keySet())
				.containsExactlyInAnyOrder("AGI", "FOR", "INT", "PRE", "VIG");
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.recursos").keySet())
				.containsExactlyInAnyOrder("PV", "SAN", "PE");
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.recursos.PV").keySet())
				.containsExactlyInAnyOrder("atual", "maximo");
		// pericias é um objeto com só as treinadas, chaveado pelo código do enum
		assertDescricaoEHabilidades(corpo);
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.pericias"))
				.containsOnly(Map.entry("ACROBACIA", "TREINADO"), Map.entry("PERCEPCAO", "TREINADO"));
	}

	/** A descrição tem exatamente as três chaves e cada habilidade exatamente id, nome, tipo e descricao. */
	private static void assertDescricaoEHabilidades(String corpo) {
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.descricao").keySet())
				.containsExactlyInAnyOrder("aparencia", "historia", "anotacoes");
		assertThat(JsonPath.<List<Object>>read(corpo, "$.habilidades")).hasSize(1);
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.habilidades[0]").keySet())
				.containsExactlyInAnyOrder("id", "nome", "tipo", "descricao");
	}

	@Test
	@DisplayName("GET /fichas devolve itens com exatamente as 7 chaves do resumo")
	void chavesDoResumo() throws Exception {
		repository.save(FichasDeTeste.ordem("Ana"));

		String corpo = mockMvc.perform(get("/fichas"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		Map<String, Object> primeiro = JsonPath.read(corpo, "$[0]");

		assertThat(primeiro.keySet()).containsExactlyInAnyOrder(
				"id", "sistema", "jogador", "personagem", "idade", "retratoVersao", "recursos");
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$[0].recursos[0]").keySet())
				.containsExactlyInAnyOrder("codigo", "atual", "maximo");
	}

	@Test
	@DisplayName("GET /fichas/{id} de D&D devolve exatamente as 20 chaves, com mapas por código")
	void chavesDaFichaDnd() throws Exception {
		FichaDnd ficha = FichasDeTeste.dnd("Ana");
		ficha.setHistoria("Ex-soldado");
		ficha.setPvAtual(10);
		ficha.setSalvaguardas(Set.of(AtributoDnd.FOR));
		ficha.getPericias().put(PericiaDnd.ATLETISMO, GrauDnd.PROFICIENTE);
		ficha.getPericias().put(PericiaDnd.ARCANISMO, GrauDnd.ESPECIALISTA);
		ficha = repository.save(ficha);
		habilidades.save(FichasDeTeste.habilidade(ficha, "Ataque", TipoHabilidade.TALENTO, "Forte"));

		String corpo = mockMvc.perform(get("/fichas/{id}", ficha.getId()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		Map<String, Object> json = JsonPath.read(corpo, "$");

		assertThat(json.keySet()).containsExactlyInAnyOrder(
				"id", "sistema", "jogador", "personagem", "genero", "idade", "retratoVersao",
				"descricao", "habilidades", "edicao", "classe", "especie", "antecedente", "nivel", "atributos", "recursos", "ca",
				"inspiracao", "salvaguardas", "pericias");
		assertThat(json.get("sistema")).isEqualTo("DND");
		assertThat(json.get("edicao")).isEqualTo("D2024");
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.atributos").keySet())
				.containsExactlyInAnyOrder("FOR", "DES", "CON", "INT", "SAB", "CAR");
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.recursos").keySet())
				.containsExactlyInAnyOrder("PV", "PV_TEMP");
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.recursos.PV").keySet())
				.containsExactlyInAnyOrder("atual", "maximo");
		assertThat(JsonPath.<List<String>>read(corpo, "$.salvaguardas")).containsExactly("FOR");
		assertDescricaoEHabilidades(corpo);
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.pericias"))
				.containsOnly(Map.entry("ATLETISMO", "PROFICIENTE"), Map.entry("ARCANISMO", "ESPECIALISTA"));
	}

	@Test
	@DisplayName("GET /fichas de D&D tem as 7 chaves do resumo e recursos só com PV")
	void chavesDoResumoDnd() throws Exception {
		FichaDnd ficha = FichasDeTeste.dnd("Ana");
		ficha.setPvAtual(4);
		ficha.setPvTemporario(2);
		repository.save(ficha);

		String corpo = mockMvc.perform(get("/fichas"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$[0]").keySet()).containsExactlyInAnyOrder(
				"id", "sistema", "jogador", "personagem", "idade", "retratoVersao", "recursos");
		assertThat(JsonPath.<List<Object>>read(corpo, "$[0].recursos")).hasSize(1);
		assertThat(JsonPath.<String>read(corpo, "$[0].recursos[0].codigo")).isEqualTo("PV");
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$[0].recursos[0]").keySet())
				.containsExactlyInAnyOrder("codigo", "atual", "maximo");
	}
}
