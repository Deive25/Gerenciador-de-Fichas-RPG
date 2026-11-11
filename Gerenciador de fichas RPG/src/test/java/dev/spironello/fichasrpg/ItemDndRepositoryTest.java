package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.ItemDnd;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.repository.ItemDndRepository;
import jakarta.persistence.EntityManager;

/** Itens do inventário de D&D pelo banco. Cada teste roda numa transação revertida no fim. */
@Transactional
@DisplayName("Itens de D&D no banco")
class ItemDndRepositoryTest extends IntegrationTestBase {

	@Autowired
	private FichaRepository fichas;

	@Autowired
	private ItemDndRepository itens;

	@Autowired
	private EntityManager em;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	@DisplayName("lista só os itens da ficha, na ordem de criação")
	void listaPorFicha() {
		FichaDnd a = fichas.save(FichasDeTeste.dnd("A"));
		FichaDnd b = fichas.save(FichasDeTeste.dnd("B"));
		itens.save(FichasDeTeste.itemDnd(a, "Segundo"));
		itens.save(FichasDeTeste.itemDnd(b, "De outra"));
		itens.save(FichasDeTeste.itemDnd(a, "Terceiro"));
		em.flush();
		em.clear();

		assertThat(itens.findByFichaIdOrderById(a.getId())).extracting(ItemDnd::getNome)
				.containsExactly("Segundo", "Terceiro");
		assertThat(itens.findByFichaIdOrderById(b.getId())).hasSize(1);
	}

	@Test
	@DisplayName("findByIdAndFichaId com a ficha errada devolve vazio")
	void fichaErrada() {
		FichaDnd a = fichas.save(FichasDeTeste.dnd("A"));
		FichaDnd b = fichas.save(FichasDeTeste.dnd("B"));
		ItemDnd item = itens.save(FichasDeTeste.itemDnd(a, "Lanterna"));
		em.flush();
		em.clear();

		assertThat(itens.findByIdAndFichaId(item.getId(), b.getId())).isEmpty();
		assertThat(itens.findByIdAndFichaId(item.getId(), a.getId())).isPresent();
	}

	@Test
	@DisplayName("excluir a ficha apaga os itens (on delete cascade)")
	void excluirApagaItens() {
		FichaDnd a = fichas.save(FichasDeTeste.dnd("A"));
		itens.save(FichasDeTeste.itemDnd(a, "Um"));
		itens.save(FichasDeTeste.itemDnd(a, "Dois"));
		em.flush();
		em.clear();
		assertThat(jdbc.queryForObject("select count(*) from ficha_dnd_item where ficha_id = ?", Integer.class,
				a.getId())).isEqualTo(2);

		fichas.deleteById(a.getId());
		em.flush();

		assertThat(jdbc.queryForObject("select count(*) from ficha_dnd_item where ficha_id = ?", Integer.class,
				a.getId())).isZero();
	}

	@Test
	@DisplayName("o peso 1.50, a quantidade e o equipado fazem ida e volta")
	void idaEVolta() {
		FichaDnd ficha = fichas.save(FichasDeTeste.dnd("A"));
		ItemDnd item = FichasDeTeste.itemDnd(ficha, "Espada");
		item.setPeso(new BigDecimal("1.50"));
		item.setQuantidade(2);
		item.setEquipado(true);
		Long id = itens.save(item).getId();
		em.flush();
		em.clear();

		ItemDnd lido = itens.findById(id).orElseThrow();
		assertThat(lido.getPeso()).isEqualByComparingTo("1.50");
		assertThat(lido.getQuantidade()).isEqualTo(2);
		assertThat(lido.isEquipado()).isTrue();
	}

	@Test
	@DisplayName("as moedas da ficha começam em 0 e fazem ida e volta")
	void moedas() {
		Long id = fichas.save(FichasDeTeste.dnd("A")).getId();
		em.flush();
		em.clear();
		FichaDnd nova = (FichaDnd) fichas.findById(id).orElseThrow();
		assertThat(new int[] {nova.getPc(), nova.getPp(), nova.getPe(), nova.getPo(), nova.getPl()})
				.containsOnly(0);

		nova.setPc(1);
		nova.setPp(2);
		nova.setPe(3);
		nova.setPo(4);
		nova.setPl(5);
		em.flush();
		em.clear();

		FichaDnd lida = (FichaDnd) fichas.findById(id).orElseThrow();
		assertThat(new int[] {lida.getPc(), lida.getPp(), lida.getPe(), lida.getPo(), lida.getPl()})
				.containsExactly(1, 2, 3, 4, 5);
	}
}
