package dev.spironello.fichasrpg.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.model.ClasseOrdem;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.GrauOrdem;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.repository.FichaRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * Comportamento esperado da API /fichas com fichas de Ordem Paranormal (códigos HTTP, corpos e erros).
 * Veio do SheetPlayerControllerTest (d100): os mesmos comportamentos com o JSON de Ordem.
 */
@DisplayName("API /fichas (comportamento esperado)")
class FichaControllerTest extends IntegrationTestBase {

	private static final Long ID_INEXISTENTE = 999_999L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository repository;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void limparBanco() {
		repository.deleteAll();
	}

	// ---------- GET /fichas ----------

	@Test
	@DisplayName("GET /fichas sem fichas devolve 200 e lista vazia")
	void listarSemFichas() throws Exception {
		mockMvc.perform(get("/fichas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	@DisplayName("GET /fichas devolve o resumo de cada ficha, com PV, SAN e PE nessa ordem")
	void listarComFichas() throws Exception {
		FichaOrdem primeira = FichasDeTeste.ordem("Ana");
		primeira.setJogador("Arthur");
		primeira.setPvAtual(8);
		primeira.setPvMax(10);
		primeira.setPeAtual(3);
		repository.save(primeira);
		FichaOrdem segunda = FichasDeTeste.ordem("Beto");
		segunda.setJogador("Bruno");
		repository.save(segunda);

		mockMvc.perform(get("/fichas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[*].jogador", containsInAnyOrder("Arthur", "Bruno")))
				.andExpect(jsonPath("$[*].personagem", containsInAnyOrder("Ana", "Beto")))
				.andExpect(jsonPath("$[*].sistema", containsInAnyOrder("ORDEM_PARANORMAL", "ORDEM_PARANORMAL")))
				.andExpect(jsonPath("$[?(@.personagem == 'Ana')].recursos[0].codigo", contains("PV")))
				.andExpect(jsonPath("$[?(@.personagem == 'Ana')].recursos[0].atual", contains(8)))
				.andExpect(jsonPath("$[?(@.personagem == 'Ana')].recursos[0].maximo", contains(10)))
				.andExpect(jsonPath("$[?(@.personagem == 'Ana')].recursos[1].codigo", contains("SAN")))
				.andExpect(jsonPath("$[?(@.personagem == 'Ana')].recursos[2].codigo", contains("PE")))
				.andExpect(jsonPath("$[?(@.personagem == 'Ana')].recursos[2].atual", contains(3)));
	}

	// ---------- GET /fichas/{id} ----------

	@Test
	@DisplayName("GET /fichas/{id} existente devolve 200 e a ficha de Ordem, com os recursos PV, SAN e PE")
	void buscarFichaExistente() throws Exception {
		FichaOrdem ficha = FichasDeTeste.ordem("Ana");
		ficha.setClasse(ClasseOrdem.OCULTISTA);
		ficha.setOrigem("Acadêmico");
		ficha.setSanAtual(7);
		ficha = repository.save(ficha);

		mockMvc.perform(get("/fichas/{id}", ficha.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ficha.getId()))
				.andExpect(jsonPath("$.sistema").value("ORDEM_PARANORMAL"))
				.andExpect(jsonPath("$.jogador").value("Ana"))
				.andExpect(jsonPath("$.personagem").value("Ana"))
				.andExpect(jsonPath("$.classe").value("OCULTISTA"))
				.andExpect(jsonPath("$.origem").value("Acadêmico"))
				.andExpect(jsonPath("$.nex").value(5))
				.andExpect(jsonPath("$.atributos.AGI").value(1))
				.andExpect(jsonPath("$.recursos.length()").value(3))
				.andExpect(jsonPath("$.recursos.SAN.atual").value(7))
				.andExpect(jsonPath("$.recursos.PV.atual", nullValue()))
				.andExpect(jsonPath("$.recursos.PE.maximo", nullValue()));
	}

	@Test
	@DisplayName("GET /fichas/{id} inexistente devolve 404 com ProblemDetail")
	void buscarFichaInexistente() throws Exception {
		mockMvc.perform(get("/fichas/{id}", ID_INEXISTENTE))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"))
				.andExpect(jsonPath("$.detail", containsString(String.valueOf(ID_INEXISTENTE))));
	}

	// ---------- POST /fichas ----------

	@Test
	@DisplayName("POST /fichas devolve 201, Location e a ficha criada, com id gerado")
	void criarFicha() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("idade", 30);
		corpo.put("classe", "COMBATENTE");
		corpo.put("origem", "Policial");
		corpo.put("nex", 50);
		corpo.put("atributos", Map.of("AGI", 3, "FOR", 2, "INT", 1, "PRE", 1, "VIG", 0));
		corpo.put("recursos", Map.of("PV", Map.of("atual", 12, "maximo", 20), "PE", Map.of("maximo", 6)));

		MvcResult resultado = mockMvc.perform(post("/fichas")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", notNullValue()))
				.andExpect(jsonPath("$.sistema").value("ORDEM_PARANORMAL"))
				.andExpect(jsonPath("$.jogador").value("Arthur"))
				.andExpect(jsonPath("$.idade").value(30))
				.andExpect(jsonPath("$.classe").value("COMBATENTE"))
				.andExpect(jsonPath("$.nex").value(50))
				.andExpect(jsonPath("$.atributos.AGI").value(3))
				.andExpect(jsonPath("$.atributos.VIG").value(0))
				.andExpect(jsonPath("$.recursos.PV.atual").value(12))
				.andExpect(jsonPath("$.recursos.PV.maximo").value(20))
				.andExpect(jsonPath("$.recursos.PE.atual", nullValue()))
				.andExpect(jsonPath("$.recursos.PE.maximo").value(6))
				.andExpect(jsonPath("$.recursos.SAN.atual", nullValue()))
				.andExpect(jsonPath("$.retratoVersao", nullValue()))
				.andReturn();

		Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
		assertThat(resultado.getResponse().getHeader("Location")).isEqualTo("/fichas/" + id);

		assertThat(repository.findAll()).singleElement().isInstanceOfSatisfying(FichaOrdem.class, salva -> {
			assertThat(salva.getJogador()).isEqualTo("Arthur");
			assertThat(salva.getClasse()).isEqualTo(ClasseOrdem.COMBATENTE);
			assertThat(salva.getAgilidade()).isEqualTo(3);
			assertThat(salva.getPvAtual()).isEqualTo(12);
			assertThat(salva.getPeMax()).isEqualTo(6);
			assertThat(salva.getSanAtual()).isNull();
		});
	}

	@Test
	@DisplayName("POST /fichas sem os campos obrigatórios devolve 400 com os erros por campo e nada é salvo")
	void criarFichaSemCamposObrigatorios() throws Exception {
		String corpo = objectMapper.writeValueAsString(Map.of("sistema", "ORDEM_PARANORMAL", "idade", 30));

		mockMvc.perform(post("/fichas")
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Dados inválidos"))
				.andExpect(jsonPath("$.detail").value("Há campos inválidos na requisição. Veja a propriedade errors."))
				.andExpect(jsonPath("$.errors.jogador").value("é obrigatório"))
				.andExpect(jsonPath("$.errors.personagem").value("é obrigatório"))
				.andExpect(jsonPath("$.errors.genero").value("é obrigatório"))
				.andExpect(jsonPath("$.errors.nex").value("é obrigatório"))
				.andExpect(jsonPath("$.errors.atributos").value("é obrigatório"));

		assertThat(repository.count()).isZero();
	}

	// ---------- PUT /fichas/{id} ----------

	@Test
	@DisplayName("PUT /fichas/{id} existente devolve 200 e atualiza a ficha")
	void atualizarFichaExistente() throws Exception {
		FichaOrdem ficha = repository.save(FichasDeTeste.ordem("Ana"));
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("jogador", "Arthur Atualizado");
		corpo.put("genero", "M");
		corpo.put("idade", 31);
		corpo.put("nex", 60);
		corpo.put("atributos", Map.of("AGI", 2, "FOR", 4, "INT", 1, "PRE", 1, "VIG", 1));

		mockMvc.perform(put("/fichas/{id}", ficha.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ficha.getId()))
				.andExpect(jsonPath("$.jogador").value("Arthur Atualizado"))
				.andExpect(jsonPath("$.genero").value("M"))
				.andExpect(jsonPath("$.idade").value(31))
				.andExpect(jsonPath("$.nex").value(60))
				.andExpect(jsonPath("$.atributos.FOR").value(4));

		FichaOrdem noBanco = (FichaOrdem) repository.findById(ficha.getId()).orElseThrow();
		assertThat(noBanco.getJogador()).isEqualTo("Arthur Atualizado");
		assertThat(noBanco.getIdade()).isEqualTo(31);
		assertThat(noBanco.getNex()).isEqualTo(60);
		assertThat(noBanco.getForca()).isEqualTo(4);
	}

	@Test
	@DisplayName("PUT /fichas/{id} troca a ficha inteira, de propósito: campos omitidos viram null")
	void atualizarFichaApagaCamposOmitidos() throws Exception {
		FichaOrdem ficha = FichasDeTeste.ordem("Ana");
		ficha.setIdade(30);
		ficha.setClasse(ClasseOrdem.ESPECIALISTA);
		ficha.setOrigem("Atleta");
		ficha.setPvAtual(8);
		ficha.setPvMax(10);
		ficha = repository.save(ficha);

		// Intencional (ADR 0008): o PUT é substituição completa. O que não vier no JSON é gravado como null.
		mockMvc.perform(put("/fichas/{id}", ficha.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(FichasDeTeste.corpoOrdem())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.idade", nullValue()))
				.andExpect(jsonPath("$.classe", nullValue()))
				.andExpect(jsonPath("$.origem", nullValue()))
				.andExpect(jsonPath("$.recursos.PV.atual", nullValue()))
				.andExpect(jsonPath("$.recursos.PV.maximo", nullValue()));

		FichaOrdem noBanco = (FichaOrdem) repository.findById(ficha.getId()).orElseThrow();
		assertThat(noBanco.getIdade()).isNull();
		assertThat(noBanco.getClasse()).isNull();
		assertThat(noBanco.getOrigem()).isNull();
		assertThat(noBanco.getPvAtual()).isNull();
		assertThat(noBanco.getPvMax()).isNull();
	}

	@Test
	@DisplayName("PUT /fichas/{id} inexistente devolve 404 com ProblemDetail e não cria nada")
	void atualizarFichaInexistente() throws Exception {
		mockMvc.perform(put("/fichas/{id}", ID_INEXISTENTE)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(FichasDeTeste.corpoOrdem())))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail", containsString(String.valueOf(ID_INEXISTENTE))));

		assertThat(repository.count()).isZero();
	}

	// ---------- perícias ----------

	@Test
	@DisplayName("POST com pericias devolve só as treinadas, por código do enum, e a leitura seguinte as traz do banco")
	void criarFichaComPericias() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("nex", 40);
		corpo.put("pericias", Map.of("ACROBACIA", "TREINADO", "PERCEPCAO", "VETERANO"));

		MvcResult criada = mockMvc.perform(post("/fichas").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.pericias.ACROBACIA").value("TREINADO"))
				.andExpect(jsonPath("$.pericias.PERCEPCAO").value("VETERANO"))
				.andExpect(jsonPath("$.pericias.length()").value(2))
				.andReturn();

		Number id = JsonPath.read(criada.getResponse().getContentAsString(), "$.id");
		mockMvc.perform(get("/fichas/{id}", id.longValue()))
				.andExpect(jsonPath("$.pericias.ACROBACIA").value("TREINADO"))
				.andExpect(jsonPath("$.pericias.PERCEPCAO").value("VETERANO"))
				.andExpect(jsonPath("$.pericias.length()").value(2));
	}

	@Test
	@DisplayName("POST com pericias de grau null (como o Angular envia) ignora as nulas: não viram linha nem chave")
	void periciasNulasSaoIgnoradas() throws Exception {
		String corpo = """
				{"sistema": "ORDEM_PARANORMAL", "id": null, "jogador": "Arthur", "personagem": "Ana", "genero": "F",
				 "nex": 5, "atributos": {"AGI": 1, "FOR": 1, "INT": 1, "PRE": 1, "VIG": 1},
				 "pericias": {"ACROBACIA": null, "CRIME": "TREINADO", "LUTA": null}}""";

		MvcResult criada = mockMvc.perform(post("/fichas").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.pericias.CRIME").value("TREINADO"))
				.andExpect(jsonPath("$.pericias.length()").value(1))
				.andReturn();

		Number id = JsonPath.read(criada.getResponse().getContentAsString(), "$.id");
		mockMvc.perform(get("/fichas/{id}", id.longValue()))
				.andExpect(jsonPath("$.pericias.CRIME").value("TREINADO"))
				.andExpect(jsonPath("$.pericias.length()").value(1));
	}

	@Test
	@DisplayName("PUT com outro conjunto de perícias substitui o anterior, e o GET mostra só o novo (ida e volta pelo banco)")
	void putSubstituiAsPericias() throws Exception {
		Map<String, Object> criacao = FichasDeTeste.corpoOrdem();
		criacao.put("nex", 40);
		criacao.put("pericias", Map.of("ACROBACIA", "TREINADO", "PERCEPCAO", "TREINADO"));
		MvcResult criada = mockMvc.perform(post("/fichas").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(criacao)))
				.andExpect(status().isCreated())
				.andReturn();
		Number id = JsonPath.read(criada.getResponse().getContentAsString(), "$.id");

		Map<String, Object> troca = FichasDeTeste.corpoOrdem();
		troca.put("nex", 40);
		troca.put("pericias", Map.of("PERCEPCAO", "VETERANO", "FURTIVIDADE", "TREINADO"));
		mockMvc.perform(put("/fichas/{id}", id.longValue()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(troca)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pericias.PERCEPCAO").value("VETERANO"))
				.andExpect(jsonPath("$.pericias.ACROBACIA").doesNotExist())
				.andExpect(jsonPath("$.pericias.length()").value(2));

		mockMvc.perform(get("/fichas/{id}", id.longValue()))
				.andExpect(jsonPath("$.pericias.PERCEPCAO").value("VETERANO"))
				.andExpect(jsonPath("$.pericias.FURTIVIDADE").value("TREINADO"))
				.andExpect(jsonPath("$.pericias.ACROBACIA").doesNotExist())
				.andExpect(jsonPath("$.pericias.length()").value(2));
	}

	@Test
	@DisplayName("PUT sem o objeto pericias remove todas as perícias da ficha")
	void putOmitindoPericiasRemoveTodas() throws Exception {
		Map<String, Object> criacao = FichasDeTeste.corpoOrdem();
		criacao.put("pericias", Map.of("ACROBACIA", "TREINADO"));
		MvcResult criada = mockMvc.perform(post("/fichas").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(criacao)))
				.andExpect(status().isCreated())
				.andReturn();
		Number id = JsonPath.read(criada.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(put("/fichas/{id}", id.longValue()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(FichasDeTeste.corpoOrdem())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pericias.length()").value(0));

		mockMvc.perform(get("/fichas/{id}", id.longValue()))
				.andExpect(jsonPath("$.pericias.length()").value(0));
	}

	// ---------- DELETE /fichas/{id} ----------

	@Test
	@DisplayName("DELETE /fichas/{id} existente devolve 204 sem corpo e remove a ficha (e as perícias dela)")
	void excluirFichaExistente() throws Exception {
		FichaOrdem ficha = FichasDeTeste.ordem("Ana");
		ficha.getPericias().put(PericiaOrdem.LUTA, GrauOrdem.TREINADO);
		ficha = repository.save(ficha);

		mockMvc.perform(delete("/fichas/{id}", ficha.getId()))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		assertThat(repository.existsById(ficha.getId())).isFalse();
	}

	@Test
	@DisplayName("DELETE /fichas/{id} inexistente devolve 404 com ProblemDetail e não remove nada")
	void excluirFichaInexistente() throws Exception {
		repository.save(FichasDeTeste.ordem("Ana"));

		mockMvc.perform(delete("/fichas/{id}", ID_INEXISTENTE))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404));

		assertThat(repository.count()).isEqualTo(1);
	}
}
