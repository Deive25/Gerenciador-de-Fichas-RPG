package dev.spironello.fichasrpg.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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

import java.util.List;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import dev.spironello.fichasrpg.model.AtributoDnd;
import dev.spironello.fichasrpg.model.EdicaoDnd;
import dev.spironello.fichasrpg.model.EspacoMagia;
import dev.spironello.fichasrpg.model.MagiaDnd;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.ItemDnd;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.GrauDnd;
import dev.spironello.fichasrpg.model.PericiaDnd;
import dev.spironello.fichasrpg.model.TipoHabilidade;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.repository.HabilidadeFichaRepository;
import dev.spironello.fichasrpg.repository.ItemDndRepository;
import dev.spironello.fichasrpg.repository.MagiaDndRepository;
import tools.jackson.databind.ObjectMapper;

/** Comportamento do CRUD /fichas com fichas de D&D (5e de 2014 e 2024): corpos, ida e volta pelo banco, 409. */
@DisplayName("API /fichas com fichas de D&D")
class FichaDndControllerTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository repository;

	@Autowired
	private HabilidadeFichaRepository habilidades;

	@Autowired
	private ItemDndRepository itens;

	@Autowired
	private MagiaDndRepository magias;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void limparBanco() {
		repository.deleteAll();
	}

	private Map<String, Object> corpoCompleto() {
		Map<String, Object> corpo = FichasDeTeste.corpoDnd();
		corpo.put("idade", 27);
		corpo.put("edicao", "D2014");
		corpo.put("classe", "Mago");
		corpo.put("especie", "Elfo");
		corpo.put("antecedente", "Sábio");
		corpo.put("nivel", 5);
		corpo.put("atributos", Map.of("FOR", 8, "DES", 14, "CON", 12, "INT", 18, "SAB", 13, "CAR", 9));
		corpo.put("recursos", Map.of("PV", Map.of("atual", 20, "maximo", 28), "PV_TEMP", Map.of("atual", 5)));
		corpo.put("ca", 13);
		corpo.put("inspiracao", true);
		corpo.put("salvaguardas", List.of("INT", "SAB"));
		corpo.put("pericias", Map.of("ARCANISMO", "ESPECIALISTA", "HISTORIA", "PROFICIENTE"));
		return corpo;
	}

	@Test
	@DisplayName("POST /fichas com D&D devolve 201, Location e a ficha com todas as chaves; salva no banco")
	void criar() throws Exception {
		MvcResult resultado = mockMvc.perform(post("/fichas")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpoCompleto())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", notNullValue()))
				.andExpect(jsonPath("$.sistema").value("DND"))
				.andExpect(jsonPath("$.edicao").value("D2014"))
				.andExpect(jsonPath("$.classe").value("Mago"))
				.andExpect(jsonPath("$.especie").value("Elfo"))
				.andExpect(jsonPath("$.antecedente").value("Sábio"))
				.andExpect(jsonPath("$.idade").value(27))
				.andExpect(jsonPath("$.nivel").value(5))
				.andExpect(jsonPath("$.atributos.INT").value(18))
				.andExpect(jsonPath("$.atributos.FOR").value(8))
				.andExpect(jsonPath("$.recursos.PV.atual").value(20))
				.andExpect(jsonPath("$.recursos.PV.maximo").value(28))
				.andExpect(jsonPath("$.recursos.PV_TEMP.atual").value(5))
				.andExpect(jsonPath("$.recursos.PV_TEMP.maximo", nullValue()))
				.andExpect(jsonPath("$.ca").value(13))
				.andExpect(jsonPath("$.inspiracao").value(true))
				.andExpect(jsonPath("$.salvaguardas", containsInAnyOrder("INT", "SAB")))
				.andExpect(jsonPath("$.pericias.ARCANISMO").value("ESPECIALISTA"))
				.andExpect(jsonPath("$.pericias.HISTORIA").value("PROFICIENTE"))
				.andExpect(jsonPath("$.retratoVersao", nullValue()))
				.andReturn();

		Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
		assertThat(resultado.getResponse().getHeader("Location")).isEqualTo("/fichas/" + id);

		// Ida e volta pelo banco: o GET lê a ficha salva, não a resposta do POST.
		mockMvc.perform(get("/fichas/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pericias.ARCANISMO").value("ESPECIALISTA"))
				.andExpect(jsonPath("$.salvaguardas", containsInAnyOrder("INT", "SAB")))
				.andExpect(jsonPath("$.recursos.PV_TEMP.atual").value(5));
	}

	@Test
	@DisplayName("GET /fichas/{id} devolve os recursos sempre completos, sem salvaguardas nem perícias quando vazios")
	void buscarComRecursosCompletos() throws Exception {
		FichaDnd ficha = repository.save(FichasDeTeste.dnd("Ana"));

		mockMvc.perform(get("/fichas/{id}", ficha.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.recursos.PV.atual", nullValue()))
				.andExpect(jsonPath("$.recursos.PV.maximo", nullValue()))
				.andExpect(jsonPath("$.recursos.PV_TEMP.atual", nullValue()))
				.andExpect(jsonPath("$.recursos.PV_TEMP.maximo", nullValue()))
				.andExpect(jsonPath("$.inspiracao").value(false))
				.andExpect(jsonPath("$.salvaguardas", hasSize(0)))
				.andExpect(jsonPath("$.pericias").isEmpty());
	}

	@Test
	@DisplayName("inspiracao nula vira false e salvaguardas nulas viram lista vazia")
	void nulosViramPadrao() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoDnd();
		corpo.put("inspiracao", null);
		corpo.put("salvaguardas", null);

		mockMvc.perform(post("/fichas")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.inspiracao").value(false))
				.andExpect(jsonPath("$.salvaguardas", hasSize(0)));
	}

	@Test
	@DisplayName("PUT substitui a ficha inteira, inclusive perícias e salvaguardas, e a edição pode mudar")
	void atualizar() throws Exception {
		FichaDnd ficha = FichasDeTeste.dnd("Antiga");
		ficha.setSalvaguardas(Set.of(AtributoDnd.FOR, AtributoDnd.CON));
		ficha.getPericias().put(PericiaDnd.ATLETISMO, GrauDnd.PROFICIENTE);
		ficha.getPericias().put(PericiaDnd.HISTORIA, GrauDnd.PROFICIENTE);
		ficha.setEdicao(EdicaoDnd.D2024);
		ficha = repository.save(ficha);

		mockMvc.perform(put("/fichas/{id}", ficha.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpoCompleto())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ficha.getId()))
				.andExpect(jsonPath("$.edicao").value("D2014"));

		mockMvc.perform(get("/fichas/{id}", ficha.getId()))
				.andExpect(jsonPath("$.personagem").value("Ana"))
				.andExpect(jsonPath("$.edicao").value("D2014"))
				.andExpect(jsonPath("$.salvaguardas", containsInAnyOrder("INT", "SAB")))
				.andExpect(jsonPath("$.pericias.ARCANISMO").value("ESPECIALISTA"))
				.andExpect(jsonPath("$.pericias.HISTORIA").value("PROFICIENTE"))
				.andExpect(jsonPath("$.pericias.ATLETISMO").doesNotExist());

		// Segundo PUT sem perícias, salvaguardas nem recursos: tudo é apagado.
		mockMvc.perform(put("/fichas/{id}", ficha.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(FichasDeTeste.corpoDnd())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.edicao").value("D2024"))
				.andExpect(jsonPath("$.pericias").isEmpty())
				.andExpect(jsonPath("$.salvaguardas", hasSize(0)))
				.andExpect(jsonPath("$.recursos.PV.atual", nullValue()))
				.andExpect(jsonPath("$.ca", nullValue()));
	}

	@Test
	@DisplayName("PUT /fichas/{id} de D&D não apaga a descrição nem as habilidades (ADR 0018)")
	void atualizarPreservaDescricaoEHabilidades() throws Exception {
		FichaDnd ficha = FichasDeTeste.dnd("Ana");
		ficha.setAparencia("Meio-elfa");
		ficha.setHistoria("Ex-soldado");
		ficha.setAnotacoes("Dívida com a guilda");
		ficha = repository.save(ficha);
		habilidades.save(FichasDeTeste.habilidade(ficha, "Sentido de Perigo", TipoHabilidade.TALENTO, "Vantagem"));

		mockMvc.perform(put("/fichas/{id}", ficha.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(FichasDeTeste.corpoDnd())))
				.andExpect(status().isOk());

		mockMvc.perform(get("/fichas/{id}", ficha.getId()))
				.andExpect(jsonPath("$.descricao.aparencia").value("Meio-elfa"))
				.andExpect(jsonPath("$.descricao.historia").value("Ex-soldado"))
				.andExpect(jsonPath("$.descricao.anotacoes").value("Dívida com a guilda"))
				.andExpect(jsonPath("$.habilidades", hasSize(1)))
				.andExpect(jsonPath("$.habilidades[0].nome").value("Sentido de Perigo"))
				.andExpect(jsonPath("$.habilidades[0].tipo").value("TALENTO"));
	}

	@Test
	@DisplayName("GET /fichas/{id} de uma ficha de D&D nova traz itens vazios e as cinco moedas em 0")
	void fichaNovaTemItensVaziosEMoedasZeradas() throws Exception {
		FichaDnd ficha = repository.save(FichasDeTeste.dnd("Ana"));

		mockMvc.perform(get("/fichas/{id}", ficha.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.itens", hasSize(0)))
				.andExpect(jsonPath("$.moedas.pc").value(0))
				.andExpect(jsonPath("$.moedas.pp").value(0))
				.andExpect(jsonPath("$.moedas.pe").value(0))
				.andExpect(jsonPath("$.moedas.po").value(0))
				.andExpect(jsonPath("$.moedas.pl").value(0));
	}

	@Test
	@DisplayName("GET /fichas/{id} de D&D traz os itens (peso, equipado) na ordem de criação e as moedas da ficha")
	void buscarTrazItensEMoedas() throws Exception {
		FichaDnd ficha = FichasDeTeste.dnd("Ana");
		ficha.setPc(1);
		ficha.setPp(2);
		ficha.setPe(3);
		ficha.setPo(4);
		ficha.setPl(5);
		ficha = repository.save(ficha);
		ItemDnd primeiro = FichasDeTeste.itemDnd(ficha, "Espada");
		primeiro.setQuantidade(2);
		primeiro.setPeso(new BigDecimal("1.50"));
		primeiro.setEquipado(true);
		primeiro.setDescricao("Longa");
		itens.save(primeiro);
		ItemDnd segundo = FichasDeTeste.itemDnd(ficha, "Pluma");
		segundo.setPeso(BigDecimal.ZERO);
		itens.save(segundo);

		mockMvc.perform(get("/fichas/{id}", ficha.getId()))
				.andExpect(jsonPath("$.itens", hasSize(2)))
				.andExpect(jsonPath("$.itens[0].nome").value("Espada"))
				.andExpect(jsonPath("$.itens[0].quantidade").value(2))
				.andExpect(jsonPath("$.itens[0].peso").value(1.5))
				.andExpect(jsonPath("$.itens[0].equipado").value(true))
				.andExpect(jsonPath("$.itens[0].descricao").value("Longa"))
				.andExpect(jsonPath("$.itens[1].nome").value("Pluma"))
				.andExpect(jsonPath("$.itens[1].peso").value(0))
				.andExpect(jsonPath("$.itens[1].equipado").value(false))
				.andExpect(jsonPath("$.moedas.pc").value(1))
				.andExpect(jsonPath("$.moedas.pp").value(2))
				.andExpect(jsonPath("$.moedas.pe").value(3))
				.andExpect(jsonPath("$.moedas.po").value(4))
				.andExpect(jsonPath("$.moedas.pl").value(5));
	}

	@Test
	@DisplayName("PUT /fichas/{id} de D&D não apaga os itens nem zera as moedas (ADR 0018)")
	void atualizarPreservaItensEMoedas() throws Exception {
		FichaDnd ficha = FichasDeTeste.dnd("Ana");
		ficha.setPo(42);
		ficha = repository.save(ficha);
		itens.save(FichasDeTeste.itemDnd(ficha, "Corda"));

		mockMvc.perform(put("/fichas/{id}", ficha.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(FichasDeTeste.corpoDnd())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.itens", hasSize(1)))
				.andExpect(jsonPath("$.moedas.po").value(42));

		assertThat(itens.findByFichaIdOrderById(ficha.getId())).extracting(ItemDnd::getNome).containsExactly("Corda");
		assertThat(((FichaDnd) repository.findById(ficha.getId()).orElseThrow()).getPo()).isEqualTo(42);
	}

	@Test
	@DisplayName("PUT /fichas/{id} de D&D com itens no corpo devolve 400 (o JSON é estrito)")
	void atualizarComItensNoCorpoDa400() throws Exception {
		FichaDnd ficha = repository.save(FichasDeTeste.dnd("Ana"));
		Map<String, Object> corpo = FichasDeTeste.corpoDnd();
		corpo.put("itens", List.of());

		mockMvc.perform(put("/fichas/{id}", ficha.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("PUT /fichas/{id} de D&D com moedas no corpo devolve 400 (o JSON é estrito)")
	void atualizarComMoedasNoCorpoDa400() throws Exception {
		FichaDnd ficha = repository.save(FichasDeTeste.dnd("Ana"));
		Map<String, Object> corpo = FichasDeTeste.corpoDnd();
		corpo.put("moedas", Map.of("po", 1));

		mockMvc.perform(put("/fichas/{id}", ficha.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("PUT /fichas/{id} de D&D grava o atributo de conjuração (INT, SAB ou CAR) e o GET devolve")
	void atualizarGravaAtributoDeConjuracao() throws Exception {
		FichaDnd ficha = repository.save(FichasDeTeste.dnd("Ana"));
		for (String atributo : List.of("INT", "SAB", "CAR")) {
			Map<String, Object> corpo = FichasDeTeste.corpoDnd();
			corpo.put("atributoConjuracao", atributo);

			mockMvc.perform(put("/fichas/{id}", ficha.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(corpo)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.atributoConjuracao").value(atributo));

			mockMvc.perform(get("/fichas/{id}", ficha.getId()))
					.andExpect(jsonPath("$.atributoConjuracao").value(atributo));
		}
		assertThat(((FichaDnd) repository.findById(ficha.getId()).orElseThrow()).getAtributoConjuracao())
				.isEqualTo(AtributoDnd.CAR);
	}

	@Test
	@DisplayName("POST /fichas de D&D aceita o atributo de conjuração, e uma ficha nova vem com ele nulo")
	void criarComAtributoDeConjuracao() throws Exception {
		mockMvc.perform(post("/fichas")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(FichasDeTeste.corpoDnd())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.atributoConjuracao").value(nullValue()));

		Map<String, Object> corpo = FichasDeTeste.corpoDnd();
		corpo.put("atributoConjuracao", "SAB");
		mockMvc.perform(post("/fichas")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.atributoConjuracao").value("SAB"));
	}

	@Test
	@DisplayName("PUT /fichas/{id} de D&D sem atributoConjuracao zera o campo (o PUT substitui a ficha)")
	void atualizarSemAtributoDeConjuracaoZera() throws Exception {
		FichaDnd ficha = FichasDeTeste.dnd("Ana");
		ficha.setAtributoConjuracao(AtributoDnd.INT);
		ficha = repository.save(ficha);

		mockMvc.perform(put("/fichas/{id}", ficha.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(FichasDeTeste.corpoDnd())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.atributoConjuracao").value(nullValue()));
	}

	@Test
	@DisplayName("atributoConjuracao FOR, DES ou CON dá 400 em errors.atributoConjuracao, no PUT e no POST")
	void atributoDeConjuracaoFisicoDa400() throws Exception {
		FichaDnd ficha = repository.save(FichasDeTeste.dnd("Ana"));
		for (String atributo : List.of("FOR", "DES", "CON")) {
			Map<String, Object> corpo = FichasDeTeste.corpoDnd();
			corpo.put("atributoConjuracao", atributo);

			mockMvc.perform(put("/fichas/{id}", ficha.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(corpo)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errors.atributoConjuracao").exists());
			mockMvc.perform(post("/fichas")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(corpo)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errors.atributoConjuracao").exists());
		}
		assertThat(((FichaDnd) repository.findById(ficha.getId()).orElseThrow()).getAtributoConjuracao()).isNull();
	}

	@Test
	@DisplayName("atributoConjuracao que não é um atributo dá 400")
	void atributoDeConjuracaoDesconhecidoDa400() throws Exception {
		Map<String, Object> corpo = FichasDeTeste.corpoDnd();
		corpo.put("atributoConjuracao", "XYZ");

		mockMvc.perform(post("/fichas")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("GET /fichas/{id} de uma ficha de D&D nova traz magias vazias, os nove espaços em 0 e conjuração nula")
	void fichaNovaTemMagiasVaziasEEspacosZerados() throws Exception {
		FichaDnd ficha = repository.save(FichasDeTeste.dnd("Ana"));

		mockMvc.perform(get("/fichas/{id}", ficha.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.magias", hasSize(0)))
				.andExpect(jsonPath("$.espacos.length()").value(9))
				.andExpect(jsonPath("$.espacos['5'].total").value(0))
				.andExpect(jsonPath("$.espacos['5'].usados").value(0))
				.andExpect(jsonPath("$.atributoConjuracao").value(nullValue()));
	}

	@Test
	@DisplayName("PUT /fichas/{id} de D&D não apaga as magias nem os espaços de magia (ADR 0018)")
	void atualizarPreservaMagiasEEspacos() throws Exception {
		FichaDnd nova = FichasDeTeste.dnd("Ana");
		nova.getEspacos().put(1, new EspacoMagia(4, 1));
		nova.getEspacos().put(9, new EspacoMagia(1, 0));
		FichaDnd ficha = repository.save(nova);
		magias.save(FichasDeTeste.magiaDnd(ficha, "Luz", 0));

		mockMvc.perform(put("/fichas/{id}", ficha.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(FichasDeTeste.corpoDnd())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.magias", hasSize(1)))
				.andExpect(jsonPath("$.espacos['1'].total").value(4))
				.andExpect(jsonPath("$.espacos['1'].usados").value(1))
				.andExpect(jsonPath("$.espacos['9'].total").value(1));

		assertThat(magias.findByFichaIdOrderByNivelAscIdAsc(ficha.getId())).extracting(MagiaDnd::getNome)
				.containsExactly("Luz");
		mockMvc.perform(get("/fichas/{id}", ficha.getId()))
				.andExpect(jsonPath("$.espacos['1'].total").value(4))
				.andExpect(jsonPath("$.espacos['1'].usados").value(1));
	}

	@Test
	@DisplayName("PUT /fichas/{id} de D&D com magias ou espacos no corpo devolve 400 (o JSON é estrito)")
	void atualizarComMagiasOuEspacosNoCorpoDa400() throws Exception {
		FichaDnd ficha = repository.save(FichasDeTeste.dnd("Ana"));
		Map<String, Object> comMagias = FichasDeTeste.corpoDnd();
		comMagias.put("magias", List.of());
		Map<String, Object> comEspacos = FichasDeTeste.corpoDnd();
		comEspacos.put("espacos", Map.of("1", Map.of("total", 1, "usados", 0)));

		for (Map<String, Object> corpo : List.of(comMagias, comEspacos)) {
			mockMvc.perform(put("/fichas/{id}", ficha.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(corpo)))
					.andExpect(status().isBadRequest());
		}
	}

	@Test
	@DisplayName("DELETE /fichas/{id} de D&D com magias devolve 204 e as magias somem do banco")
	void excluirFichaComMagias() throws Exception {
		FichaDnd ficha = repository.save(FichasDeTeste.dnd("Ana"));
		magias.save(FichasDeTeste.magiaDnd(ficha, "Luz", 0));

		mockMvc.perform(delete("/fichas/{id}", ficha.getId())).andExpect(status().isNoContent());

		assertThat(magias.count()).isZero();
	}

	@Test
	@DisplayName("DELETE /fichas/{id} de D&D com itens devolve 204 e os itens somem do banco")
	void excluirFichaComItens() throws Exception {
		FichaDnd ficha = repository.save(FichasDeTeste.dnd("Ana"));
		itens.save(FichasDeTeste.itemDnd(ficha, "Corda"));

		mockMvc.perform(delete("/fichas/{id}", ficha.getId())).andExpect(status().isNoContent());

		assertThat(itens.count()).isZero();
	}

	@Test
	@DisplayName("DELETE /fichas/{id} de D&D devolve 204 e a ficha some")
	void excluir() throws Exception {
		FichaDnd ficha = repository.save(FichasDeTeste.dnd("Ana"));

		mockMvc.perform(delete("/fichas/{id}", ficha.getId())).andExpect(status().isNoContent());

		mockMvc.perform(get("/fichas/{id}", ficha.getId())).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("GET /fichas de uma ficha de D&D traz só o PV nos recursos")
	void resumoSoComPv() throws Exception {
		FichaDnd ficha = FichasDeTeste.dnd("Ana");
		ficha.setPvAtual(7);
		ficha.setPvMax(9);
		ficha.setPvTemporario(3);
		repository.save(ficha);

		mockMvc.perform(get("/fichas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].sistema").value("DND"))
				.andExpect(jsonPath("$[0].recursos", hasSize(1)))
				.andExpect(jsonPath("$[0].recursos[0].codigo").value("PV"))
				.andExpect(jsonPath("$[0].recursos[0].atual").value(7))
				.andExpect(jsonPath("$[0].recursos[0].maximo").value(9));
	}

	@Test
	@DisplayName("PUT com corpo de Ordem numa ficha de D&D devolve 409 e a ficha não muda")
	void putOrdemEmDnd() throws Exception {
		FichaDnd ficha = repository.save(FichasDeTeste.dnd("Original"));
		Map<String, Object> corpo = FichasDeTeste.corpoOrdem();
		corpo.put("personagem", "Trocada");

		mockMvc.perform(put("/fichas/{id}", ficha.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409));

		assertThat(repository.count()).isEqualTo(1);
		assertThat(repository.findById(ficha.getId()).orElseThrow()).isInstanceOfSatisfying(FichaDnd.class, depois -> {
			assertThat(depois.getPersonagem()).isEqualTo("Original");
			assertThat(depois.getEdicao()).isEqualTo(EdicaoDnd.D2024);
		});
	}

	@Test
	@DisplayName("PUT com corpo de D&D numa ficha de Ordem devolve 409 e a ficha não muda")
	void putDndEmOrdem() throws Exception {
		FichaOrdem ficha = repository.save(FichasDeTeste.ordem("Original"));
		Map<String, Object> corpo = FichasDeTeste.corpoDnd();
		corpo.put("personagem", "Trocada");

		mockMvc.perform(put("/fichas/{id}", ficha.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(corpo)))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409));

		assertThat(repository.count()).isEqualTo(1);
		assertThat(repository.findById(ficha.getId()).orElseThrow()).isInstanceOfSatisfying(FichaOrdem.class,
				depois -> {
					assertThat(depois.getPersonagem()).isEqualTo("Original");
					assertThat(depois.getNex()).isEqualTo(5);
				});
	}
}
