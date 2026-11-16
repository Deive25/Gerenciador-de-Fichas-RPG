package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
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
import org.springframework.jdbc.core.JdbcTemplate;

import dev.spironello.fichasrpg.campanha.Papel;
import dev.spironello.fichasrpg.campanha.Participacao;
import dev.spironello.fichasrpg.dto.FichaDndRequest;
import dev.spironello.fichasrpg.dto.FichaOrdemRequest;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.model.AtributoDnd;
import dev.spironello.fichasrpg.model.AtributoOrdem;
import dev.spironello.fichasrpg.model.EdicaoDnd;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.GrauDnd;
import dev.spironello.fichasrpg.model.GrauOrdem;
import dev.spironello.fichasrpg.model.PericiaDnd;
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
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private EntityManagerFactory entityManagerFactory;

	private final List<Long> criadas = new ArrayList<>();

	@AfterEach
	void limpar() {
		repository.deleteAllById(criadas);
		jdbc.update("delete from campanha");
		jdbc.update("delete from usuario where id = ?", 3_400_001L);
	}

	@Test
	@DisplayName("listar 2 fichas de Ordem e 2 de D&D, todas com perícias, habilidades, ataques, itens, magias, espaços e rituais, executa 1 consulta, e não 1 + 4")
	void listarEmUmaConsulta() {
		for (int i = 1; i <= 2; i++) {
			criadas.add(service.criar(pedido("ficha" + i)).id());
			criadas.add(service.criar(pedidoDnd("dnd" + i)).id());
		}
		// Habilidades, ataques, itens, magias, espaços e rituais em todas as fichas: o resumo não pode lê-los (nem a descrição) para continuar em 1 consulta.
		for (Long id : criadas) {
			habilidades.save(FichasDeTeste.habilidade(repository.getReferenceById(id), "Ataque", TipoHabilidade.CLASSE, null));
			Ficha ficha = repository.findById(id).orElseThrow();
			if (ficha instanceof FichaOrdem ordem) {
				ataquesOrdem.save(FichasDeTeste.ataqueOrdem(ordem, "Faca"));
				itensOrdem.save(FichasDeTeste.itemOrdem(ordem, "Pistola"));
				rituais.save(FichasDeTeste.ritualOrdem(ordem, "Cicatrizacao", 1));
			} else {
				ataquesDnd.save(FichasDeTeste.ataqueDnd((FichaDnd) ficha, "Espada"));
				itensDnd.save(FichasDeTeste.itemDnd((FichaDnd) ficha, "Corda"));
				magias.save(FichasDeTeste.magiaDnd((FichaDnd) ficha, "Luz", 0));
				jdbc.update("insert into ficha_dnd_espaco (ficha_id, nivel, total, usados) values (?, 1, 4, 1)", id);
			}
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

	private static final long MESTRE = 3_400_001L;

	@Test
	@DisplayName("GET /campanhas/{id}/fichas (mestre e jogador) executa 1 consulta, com fichas das duas edições cheias de listas, e não 1 + N")
	void listarDaCampanhaEmUmaConsulta() {
		CampanhasDeTeste.usuario(jdbc, MESTRE, "Mestre");
		long campanha = CampanhasDeTeste.campanha(jdbc, "Mesa");
		CampanhasDeTeste.participar(jdbc, campanha, MESTRE, Papel.MESTRE);
		CampanhasDeTeste.participar(jdbc, campanha, AutenticacaoDeTesteConfig.USUARIO_DE_TESTE_ID, Papel.JOGADOR);
		for (int i = 1; i <= 2; i++) {
			criadas.add(service.criarNaCampanha(campanha, pedido("ficha" + i)).id());
			criadas.add(service.criarNaCampanha(campanha, pedidoDnd("dnd" + i)).id());
		}
		Ficha doMestre = FichasDeTeste.ordem("do-mestre");
		doMestre.setDonoId(MESTRE);
		doMestre.setCampanhaId(campanha);
		criadas.add(repository.save(doMestre).getId());
		for (Long id : criadas) {
			habilidades.save(FichasDeTeste.habilidade(repository.getReferenceById(id), "Ataque", TipoHabilidade.CLASSE, null));
			Ficha ficha = repository.findById(id).orElseThrow();
			if (ficha instanceof FichaOrdem ordem) {
				ataquesOrdem.save(FichasDeTeste.ataqueOrdem(ordem, "Faca"));
				itensOrdem.save(FichasDeTeste.itemOrdem(ordem, "Pistola"));
				rituais.save(FichasDeTeste.ritualOrdem(ordem, "Cicatrizacao", 1));
			} else {
				ataquesDnd.save(FichasDeTeste.ataqueDnd((FichaDnd) ficha, "Espada"));
				itensDnd.save(FichasDeTeste.itemDnd((FichaDnd) ficha, "Corda"));
				magias.save(FichasDeTeste.magiaDnd((FichaDnd) ficha, "Luz", 0));
			}
		}
		Participacao comoMestre = new Participacao(campanha, MESTRE, Papel.MESTRE, Instant.now());
		Participacao comoJogador = new Participacao(campanha, AutenticacaoDeTesteConfig.USUARIO_DE_TESTE_ID, Papel.JOGADOR, Instant.now());
		Statistics estatisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		estatisticas.setStatisticsEnabled(true);

		estatisticas.clear();
		List<FichaResumoResponse> doMestreVe = service.listarDaCampanha(comoMestre);
		assertThat(doMestreVe).extracting(FichaResumoResponse::personagem).containsExactly("ficha1", "dnd1", "ficha2", "dnd2", "do-mestre");
		assertThat(estatisticas.getPrepareStatementCount()).as("consultas SQL do mestre").isEqualTo(1);

		estatisticas.clear();
		List<FichaResumoResponse> jogadorVe = service.listarDaCampanha(comoJogador);
		assertThat(jogadorVe).extracting(FichaResumoResponse::personagem).containsExactly("ficha1", "dnd1", "ficha2", "dnd2");
		assertThat(estatisticas.getPrepareStatementCount()).as("consultas SQL do jogador").isEqualTo(1);
	}

	private FichaOrdemRequest pedido(String nome) {
		Map<AtributoOrdem, Integer> atributos = new EnumMap<>(AtributoOrdem.class);
		for (AtributoOrdem atributo : AtributoOrdem.values()) {
			atributos.put(atributo, 1);
		}
		return new FichaOrdemRequest(nome, nome, "F", null, null, null, 40, atributos, null,
				Map.of(PericiaOrdem.ACROBACIA, GrauOrdem.TREINADO, PericiaOrdem.LUTA, GrauOrdem.VETERANO), null);
	}

	private FichaDndRequest pedidoDnd(String nome) {
		Map<AtributoDnd, Integer> atributos = new EnumMap<>(AtributoDnd.class);
		for (AtributoDnd atributo : AtributoDnd.values()) {
			atributos.put(atributo, 10);
		}
		return new FichaDndRequest(nome, nome, "F", null, EdicaoDnd.D2024, null, null, null, 3, atributos, null, null,
				null, Set.of(AtributoDnd.FOR),
				Map.of(PericiaDnd.ATLETISMO, GrauDnd.PROFICIENTE, PericiaDnd.ARCANISMO, GrauDnd.ESPECIALISTA), null, null);
	}
}
