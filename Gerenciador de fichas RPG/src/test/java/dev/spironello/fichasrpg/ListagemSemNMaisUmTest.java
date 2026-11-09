package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.spironello.fichasrpg.dto.FichaDndRequest;
import dev.spironello.fichasrpg.dto.FichaOrdemRequest;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.model.AtributoDnd;
import dev.spironello.fichasrpg.model.AtributoOrdem;
import dev.spironello.fichasrpg.model.EdicaoDnd;
import dev.spironello.fichasrpg.model.GrauDnd;
import dev.spironello.fichasrpg.model.GrauOrdem;
import dev.spironello.fichasrpg.model.PericiaDnd;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.model.TipoHabilidade;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.repository.HabilidadeFichaRepository;
import dev.spironello.fichasrpg.service.FichaService;
import jakarta.persistence.EntityManagerFactory;

/**
 * Prova que listar as fichas (de Ordem e de D&D) não gera N+1: o resumo não toca nas perícias (nem em nenhuma coleção lazy) e
 * a herança JOINED lê ficha + ficha_ordem + ficha_dnd num SELECT só. Conta as consultas pelas estatísticas do Hibernate.
 */
@DisplayName("Listagem sem N+1 (herança JOINED, perícias em tabela própria)")
class ListagemSemNMaisUmTest extends IntegrationTestBase {

	@Autowired
	private FichaService service;
	@Autowired
	private FichaRepository repository;
	@Autowired
	private HabilidadeFichaRepository habilidades;
	@Autowired
	private EntityManagerFactory entityManagerFactory;

	private final List<Long> criadas = new ArrayList<>();

	@AfterEach
	void limpar() {
		repository.deleteAllById(criadas);
	}

	@Test
	@DisplayName("listar 2 fichas de Ordem e 2 de D&D, todas com perícias e habilidades, executa 1 consulta, e não 1 + 4")
	void listarEmUmaConsulta() {
		for (int i = 1; i <= 2; i++) {
			criadas.add(service.criar(pedido("ficha" + i)).id());
			criadas.add(service.criar(pedidoDnd("dnd" + i)).id());
		}
		// Habilidades em todas as fichas: o resumo não pode lê-las (nem a descrição) para continuar em 1 consulta.
		for (Long id : criadas) {
			habilidades.save(FichasDeTeste.habilidade(repository.getReferenceById(id), "Ataque", TipoHabilidade.CLASSE, null));
		}
		Statistics estatisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		estatisticas.setStatisticsEnabled(true);
		estatisticas.clear();

		List<FichaResumoResponse> fichas = service.listar();

		assertThat(fichas).hasSizeGreaterThanOrEqualTo(4);
		assertThat(fichas).filteredOn(f -> criadas.contains(f.id())).extracting(FichaResumoResponse::personagem)
				.containsExactlyInAnyOrder("ficha1", "dnd1", "ficha2", "dnd2");
		assertThat(estatisticas.getPrepareStatementCount()).as("consultas SQL preparadas").isEqualTo(1);
	}

	private FichaOrdemRequest pedido(String nome) {
		Map<AtributoOrdem, Integer> atributos = new EnumMap<>(AtributoOrdem.class);
		for (AtributoOrdem atributo : AtributoOrdem.values()) {
			atributos.put(atributo, 1);
		}
		return new FichaOrdemRequest(nome, nome, "F", null, null, null, 40, atributos, null,
				Map.of(PericiaOrdem.ACROBACIA, GrauOrdem.TREINADO, PericiaOrdem.LUTA, GrauOrdem.VETERANO));
	}

	private FichaDndRequest pedidoDnd(String nome) {
		Map<AtributoDnd, Integer> atributos = new EnumMap<>(AtributoDnd.class);
		for (AtributoDnd atributo : AtributoDnd.values()) {
			atributos.put(atributo, 10);
		}
		return new FichaDndRequest(nome, nome, "F", null, EdicaoDnd.D2024, null, null, null, 3, atributos, null, null,
				null, Set.of(AtributoDnd.FOR),
				Map.of(PericiaDnd.ATLETISMO, GrauDnd.PROFICIENTE, PericiaDnd.ARCANISMO, GrauDnd.ESPECIALISTA));
	}
}
