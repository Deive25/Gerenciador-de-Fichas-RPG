package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.ItemOrdem;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.repository.ItemOrdemRepository;
import jakarta.persistence.EntityManager;

/** Itens do inventário de Ordem Paranormal pelo banco. Cada teste roda numa transação revertida no fim. */
@Transactional
@DisplayName("Itens de Ordem Paranormal no banco")
class ItemOrdemRepositoryTest extends IntegrationTestBase {

	@Autowired
	private FichaRepository fichas;

	@Autowired
	private ItemOrdemRepository itens;

	@Autowired
	private EntityManager em;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	@DisplayName("lista só os itens da ficha, na ordem de criação")
	void listaPorFicha() {
		FichaOrdem a = fichas.save(FichasDeTeste.ordem("A"));
		FichaOrdem b = fichas.save(FichasDeTeste.ordem("B"));
		itens.save(FichasDeTeste.itemOrdem(a, "Segundo"));
		itens.save(FichasDeTeste.itemOrdem(b, "De outra"));
		itens.save(FichasDeTeste.itemOrdem(a, "Terceiro"));
		em.flush();
		em.clear();

		assertThat(itens.findByFichaIdOrderById(a.getId())).extracting(ItemOrdem::getNome)
				.containsExactly("Segundo", "Terceiro");
		assertThat(itens.findByFichaIdOrderById(b.getId())).hasSize(1);
	}

	@Test
	@DisplayName("findByIdAndFichaId com a ficha errada devolve vazio")
	void fichaErrada() {
		FichaOrdem a = fichas.save(FichasDeTeste.ordem("A"));
		FichaOrdem b = fichas.save(FichasDeTeste.ordem("B"));
		ItemOrdem item = itens.save(FichasDeTeste.itemOrdem(a, "Lanterna"));
		em.flush();
		em.clear();

		assertThat(itens.findByIdAndFichaId(item.getId(), b.getId())).isEmpty();
		assertThat(itens.findByIdAndFichaId(item.getId(), a.getId())).isPresent();
	}

	@Test
	@DisplayName("excluir a ficha apaga os itens (on delete cascade)")
	void excluirApagaItens() {
		FichaOrdem a = fichas.save(FichasDeTeste.ordem("A"));
		itens.save(FichasDeTeste.itemOrdem(a, "Um"));
		itens.save(FichasDeTeste.itemOrdem(a, "Dois"));
		em.flush();
		em.clear();
		assertThat(jdbc.queryForObject("select count(*) from ficha_ordem_item where ficha_id = ?", Integer.class,
				a.getId())).isEqualTo(2);

		fichas.deleteById(a.getId());
		em.flush();

		assertThat(jdbc.queryForObject("select count(*) from ficha_ordem_item where ficha_id = ?", Integer.class,
				a.getId())).isZero();
	}

	@Test
	@DisplayName("categoria, espaços, quantidade e descrição fazem ida e volta (zero é valor válido)")
	void idaEVolta() {
		FichaOrdem ficha = fichas.save(FichasDeTeste.ordem("A"));
		ItemOrdem item = FichasDeTeste.itemOrdem(ficha, "Pistola");
		item.setCategoria(0);
		item.setEspacos(0);
		item.setQuantidade(3);
		item.setDescricao("Texto\ncom quebra");
		Long id = itens.save(item).getId();
		em.flush();
		em.clear();

		ItemOrdem lido = itens.findById(id).orElseThrow();
		assertThat(lido.getCategoria()).isZero();
		assertThat(lido.getEspacos()).isZero();
		assertThat(lido.getQuantidade()).isEqualTo(3);
		assertThat(lido.getDescricao()).isEqualTo("Texto\ncom quebra");
	}
}
