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
import dev.spironello.fichasrpg.model.EspacoMagia;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.GrauDnd;
import dev.spironello.fichasrpg.model.PericiaDnd;
import dev.spironello.fichasrpg.model.GrauOrdem;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.model.TipoHabilidade;
import dev.spironello.fichasrpg.repository.AtaqueDndRepository;
import dev.spironello.fichasrpg.repository.AtaqueOrdemRepository;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.repository.HabilidadeFichaRepository;
import dev.spironello.fichasrpg.repository.ItemDndRepository;
import dev.spironello.fichasrpg.repository.ItemOrdemRepository;
import dev.spironello.fichasrpg.repository.MagiaDndRepository;
import dev.spironello.fichasrpg.repository.RitualOrdemRepository;

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

	@Autowired
	private AtaqueOrdemRepository ataquesOrdem;

	@Autowired
	private AtaqueDndRepository ataquesDnd;

	@Autowired
	private ItemOrdemRepository itensOrdem;

	@Autowired
	private ItemDndRepository itensDnd;

	@Autowired
	private MagiaDndRepository magias;

	@Autowired
	private RitualOrdemRepository rituais;

	@BeforeEach
	void limparBanco() {
		repository.deleteAll();
	}

	@Test
	@DisplayName("GET /fichas/{id} devolve exatamente as 18 chaves da ficha de Ordem, com mapas por código")
	void chavesDaFicha() throws Exception {
		FichaOrdem ficha = FichasDeTeste.ordem("Ana");
		ficha.setAparencia("Alta");
		ficha.setPvAtual(10);
		ficha.getPericias().put(PericiaOrdem.ACROBACIA, GrauOrdem.TREINADO);
		ficha.getPericias().put(PericiaOrdem.PERCEPCAO, GrauOrdem.TREINADO);
		ficha = repository.save(ficha);
		habilidades.save(FichasDeTeste.habilidade(ficha, "Ataque", TipoHabilidade.CLASSE, "Forte"));
		ataquesOrdem.save(FichasDeTeste.ataqueOrdem(ficha, "Faca"));
		itensOrdem.save(FichasDeTeste.itemOrdem(ficha, "Pistola"));
		rituais.save(FichasDeTeste.ritualOrdem(ficha, "Cicatrizacao", 1));

		String corpo = mockMvc.perform(get("/fichas/{id}", ficha.getId()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		Map<String, Object> json = JsonPath.read(corpo, "$");

		List<String> esperadas = List.of(
				"id", "sistema", "jogador", "personagem", "genero", "idade", "retratoVersao",
				"descricao", "habilidades", "ataques", "itens", "rituais", "classe", "origem", "nex", "atributos", "recursos", "pericias");

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
		assertThat(JsonPath.<List<Object>>read(corpo, "$.itens")).hasSize(1);
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.itens[0]").keySet())
				.containsExactlyInAnyOrder("id", "nome", "categoria", "espacos", "quantidade", "descricao");
		assertThat(JsonPath.<List<Object>>read(corpo, "$.ataques")).hasSize(1);
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.ataques[0]").keySet())
				.containsExactlyInAnyOrder("id", "nome", "pericia", "bonus", "dano", "tipoDano", "alcance", "margem", "multiplicador");
		assertThat(JsonPath.<List<Object>>read(corpo, "$.rituais")).hasSize(1);
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.rituais[0]").keySet())
				.containsExactlyInAnyOrder("id", "nome", "circulo", "elemento", "execucao", "alcance", "alvo",
						"duracao", "resistencia", "descricao");
		assertThat(JsonPath.<String>read(corpo, "$.rituais[0].elemento")).isEqualTo("SANGUE");
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
	@DisplayName("GET /fichas/{id} de D&D devolve exatamente as 26 chaves, com mapas por código")
	void chavesDaFichaDnd() throws Exception {
		FichaDnd ficha = FichasDeTeste.dnd("Ana");
		ficha.setHistoria("Ex-soldado");
		ficha.setPvAtual(10);
		ficha.setSalvaguardas(Set.of(AtributoDnd.FOR));
		ficha.setAtributoConjuracao(AtributoDnd.INT);
		ficha.getEspacos().put(3, new EspacoMagia(2, 1));
		ficha.getPericias().put(PericiaDnd.ATLETISMO, GrauDnd.PROFICIENTE);
		ficha.getPericias().put(PericiaDnd.ARCANISMO, GrauDnd.ESPECIALISTA);
		ficha = repository.save(ficha);
		habilidades.save(FichasDeTeste.habilidade(ficha, "Ataque", TipoHabilidade.TALENTO, "Forte"));
		ataquesDnd.save(FichasDeTeste.ataqueDnd(ficha, "Espada"));
		itensDnd.save(FichasDeTeste.itemDnd(ficha, "Corda"));
		magias.save(FichasDeTeste.magiaDnd(ficha, "Luz", 0));

		String corpo = mockMvc.perform(get("/fichas/{id}", ficha.getId()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		Map<String, Object> json = JsonPath.read(corpo, "$");

		assertThat(json.keySet()).containsExactlyInAnyOrder(
				"id", "sistema", "jogador", "personagem", "genero", "idade", "retratoVersao",
				"descricao", "habilidades", "ataques", "itens", "magias", "espacos", "moedas", "edicao", "classe", "especie", "antecedente", "nivel", "atributoConjuracao", "atributos", "recursos", "ca",
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
		assertThat(JsonPath.<List<Object>>read(corpo, "$.itens")).hasSize(1);
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.itens[0]").keySet())
				.containsExactlyInAnyOrder("id", "nome", "quantidade", "peso", "equipado", "descricao");
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.moedas").keySet())
				.containsExactlyInAnyOrder("pc", "pp", "pe", "po", "pl");
		assertThat(JsonPath.<List<Object>>read(corpo, "$.ataques")).hasSize(1);
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.ataques[0]").keySet())
				.containsExactlyInAnyOrder("id", "nome", "atributo", "proficiente", "bonus", "dano", "tipoDano", "alcance");
		assertThat(JsonPath.<List<Object>>read(corpo, "$.magias")).hasSize(1);
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.magias[0]").keySet())
				.containsExactlyInAnyOrder("id", "nome", "nivel", "escola", "tempo", "alcance", "componentes",
						"duracao", "concentracao", "ritual", "preparada", "descricao");
		// Os nove níveis sempre, na ordem, e cada um só com total e usados.
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.espacos").keySet())
				.containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");
		assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.espacos['3']").keySet())
				.containsExactlyInAnyOrder("total", "usados");
		assertThat(JsonPath.<Integer>read(corpo, "$.espacos['3'].total")).isEqualTo(2);
		assertThat(JsonPath.<Integer>read(corpo, "$.espacos['3'].usados")).isEqualTo(1);
		assertThat(JsonPath.<Integer>read(corpo, "$.espacos['4'].total")).isZero();
		assertThat(json.get("atributoConjuracao")).isEqualTo("INT");
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
