package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.spironello.fichasrpg.dto.SheetPlayerRequest;
import dev.spironello.fichasrpg.model.Pericia;
import dev.spironello.fichasrpg.repository.SheetPlayerRepository;
import dev.spironello.fichasrpg.service.SheetPlayerService;
import jakarta.persistence.EntityManagerFactory;

/**
 * Prova que listar as fichas não gera N+1: as perícias vêm no mesmo SELECT das fichas
 * (@EntityGraph no repository). Conta as consultas pelas estatísticas do Hibernate.
 */
@DisplayName("Listagem sem N+1 (perícias em tabela própria)")
class ListagemSemNMaisUmTest extends IntegrationTestBase {

	@Autowired
	private SheetPlayerService service;
	@Autowired
	private SheetPlayerRepository repository;
	@Autowired
	private EntityManagerFactory entityManagerFactory;

	private final List<Long> criadas = new ArrayList<>();

	@AfterEach
	void limpar() {
		repository.deleteAllById(criadas);
	}

	@Test
	@DisplayName("listar 3 fichas com perícias executa 1 consulta, e não 1 + 3")
	void listarEmUmaConsulta() {
		for (int i = 1; i <= 3; i++) {
			criadas.add(service.criar(pedido("ficha" + i, 10 * i)).id());
		}
		Statistics estatisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		estatisticas.setStatisticsEnabled(true);
		estatisticas.clear();

		var fichas = service.listar();

		assertThat(fichas).hasSizeGreaterThanOrEqualTo(3);
		assertThat(fichas).filteredOn(f -> criadas.contains(f.id())).extracting(f -> f.pericias().get(Pericia.ACROBACIA)).containsExactlyInAnyOrder(10, 20, 30);
		assertThat(estatisticas.getPrepareStatementCount()).as("consultas SQL preparadas").isEqualTo(1);
	}

	private SheetPlayerRequest pedido(String nome, int acrobacia) {
		return new SheetPlayerRequest(nome, nome, "F",
				null, null, null, null, null, // idade, vida e sanidade
				null, null, null, null, null, null, null, null, null, // 9 atributos
				Map.of(Pericia.ACROBACIA, acrobacia));
	}
}
