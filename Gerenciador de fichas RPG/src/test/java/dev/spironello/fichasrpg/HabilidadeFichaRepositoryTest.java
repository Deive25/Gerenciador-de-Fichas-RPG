package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.HabilidadeFicha;
import dev.spironello.fichasrpg.model.TipoHabilidade;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.repository.HabilidadeFichaRepository;
import jakarta.persistence.EntityManager;

/** Habilidades e descrição da ficha pelo banco. Cada teste roda numa transação revertida no fim. */
@Transactional
@DisplayName("Habilidades da ficha no banco")
class HabilidadeFichaRepositoryTest extends IntegrationTestBase {

	@Autowired
	private FichaRepository fichas;

	@Autowired
	private HabilidadeFichaRepository habilidades;

	@Autowired
	private EntityManager em;

	@Autowired
	private JdbcTemplate jdbc;

	private HabilidadeFicha habilidade(Ficha ficha, String nome, TipoHabilidade tipo) {
		HabilidadeFicha h = new HabilidadeFicha();
		h.setFicha(ficha);
		h.setNome(nome);
		h.setTipo(tipo);
		return habilidades.save(h);
	}

	@Test
	@DisplayName("lista só as habilidades da ficha, na ordem de criação")
	void listaPorFicha() {
		FichaOrdem a = fichas.save(FichasDeTeste.ordem("A"));
		FichaDnd b = fichas.save(FichasDeTeste.dnd("B"));
		habilidade(a, "Segunda", TipoHabilidade.TRILHA);
		habilidade(b, "De outra", TipoHabilidade.TALENTO);
		habilidade(a, "Terceira", TipoHabilidade.OUTRA);
		em.flush();
		em.clear();

		assertThat(habilidades.findByFichaIdOrderById(a.getId())).extracting(HabilidadeFicha::getNome)
				.containsExactly("Segunda", "Terceira");
		assertThat(habilidades.findByFichaIdOrderById(b.getId())).hasSize(1);
	}

	@Test
	@DisplayName("findByIdAndFichaId com a ficha errada devolve vazio")
	void fichaErrada() {
		FichaOrdem a = fichas.save(FichasDeTeste.ordem("A"));
		FichaOrdem b = fichas.save(FichasDeTeste.ordem("B"));
		HabilidadeFicha h = habilidade(a, "Perícia", TipoHabilidade.CLASSE);
		h.setDescricao("Texto\ncom quebra");
		em.flush();
		em.clear();

		assertThat(habilidades.findByIdAndFichaId(h.getId(), b.getId())).isEmpty();
		HabilidadeFicha lida = habilidades.findByIdAndFichaId(h.getId(), a.getId()).orElseThrow();
		assertThat(lida.getTipo()).isEqualTo(TipoHabilidade.CLASSE);
		assertThat(lida.getDescricao()).isEqualTo("Texto\ncom quebra");
	}

	@Test
	@DisplayName("excluir a ficha apaga as habilidades (on delete cascade)")
	void excluirApagaHabilidades() {
		FichaOrdem a = fichas.save(FichasDeTeste.ordem("A"));
		habilidade(a, "Uma", TipoHabilidade.ORIGEM);
		habilidade(a, "Duas", TipoHabilidade.PARANORMAL);
		em.flush();
		em.clear();
		assertThat(jdbc.queryForObject("select count(*) from ficha_habilidade where ficha_id = ?", Integer.class,
				a.getId())).isEqualTo(2);

		fichas.deleteById(a.getId());
		em.flush();

		assertThat(jdbc.queryForObject("select count(*) from ficha_habilidade where ficha_id = ?", Integer.class,
				a.getId())).isZero();
	}

	@Test
	@DisplayName("a descrição da ficha faz ida e volta pelo banco")
	void descricaoIdaEVolta() {
		FichaDnd ficha = FichasDeTeste.dnd("Lia");
		ficha.setAparencia("Alta\ne magra");
		ficha.setHistoria("Nasceu em Waterdeep.");
		ficha.setAnotacoes("x".repeat(4000));
		Long id = fichas.save(ficha).getId();
		em.flush();
		em.clear();

		Ficha lida = fichas.findById(id).orElseThrow();
		assertThat(lida.getAparencia()).isEqualTo("Alta\ne magra");
		assertThat(lida.getHistoria()).isEqualTo("Nasceu em Waterdeep.");
		assertThat(lida.getAnotacoes()).hasSize(4000);

		Long outro = fichas.save(FichasDeTeste.ordem("Sem")).getId();
		em.flush();
		em.clear();
		Ficha vazia = fichas.findById(outro).orElseThrow();
		assertThat(vazia.getAparencia()).isNull();
		assertThat(vazia.getHistoria()).isNull();
		assertThat(vazia.getAnotacoes()).isNull();
	}
}
