package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.spironello.fichasrpg.dto.FichaOrdemRequest;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.model.AtributoOrdem;
import dev.spironello.fichasrpg.model.GrauOrdem;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.service.FichaService;
import jakarta.persistence.EntityManagerFactory;

/**
 * Prova que listar as fichas não gera N+1: o resumo não toca nas perícias (nem em nenhuma coleção lazy) e
 * a herança JOINED lê ficha + ficha_ordem num SELECT só. Conta as consultas pelas estatísticas do Hibernate.
 */
@DisplayName("Listagem sem N+1 (herança JOINED, perícias em tabela própria)")
class ListagemSemNMaisUmTest extends IntegrationTestBase {

	@Autowired
	private FichaService service;
	@Autowired
	private FichaRepository repository;
	@Autowired
	private EntityManagerFactory entityManagerFactory;

	private final List<Long> criadas = new ArrayList<>();

	@AfterEach
	void limpar() {
		repository.deleteAllById(criadas);
	}

	@Test
	@DisplayName("listar 3 fichas de Ordem com perícias executa 1 consulta, e não 1 + 3")
	void listarEmUmaConsulta() {
		for (int i = 1; i <= 3; i++) {
			criadas.add(service.criar(pedido("ficha" + i)).id());
		}
		Statistics estatisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		estatisticas.setStatisticsEnabled(true);
		estatisticas.clear();

		List<FichaResumoResponse> fichas = service.listar();

		assertThat(fichas).hasSizeGreaterThanOrEqualTo(3);
		assertThat(fichas).filteredOn(f -> criadas.contains(f.id())).extracting(FichaResumoResponse::personagem)
				.containsExactlyInAnyOrder("ficha1", "ficha2", "ficha3");
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
}
