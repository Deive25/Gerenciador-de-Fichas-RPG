package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.model.AtributoDnd;
import dev.spironello.fichasrpg.model.EdicaoDnd;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.GrauDnd;
import dev.spironello.fichasrpg.model.PericiaDnd;
import dev.spironello.fichasrpg.model.Sistema;
import dev.spironello.fichasrpg.repository.FichaRepository;
import jakarta.persistence.EntityManager;

/**
 * Ida e volta da ficha de D&D pelo banco. Cada teste roda numa transação (revertida no fim); o flush e o clear
 * do EntityManager obrigam a releitura a vir do PostgreSQL, não do cache da sessão.
 */
@Transactional
@DisplayName("Ficha de D&D no banco")
class FichaDndRepositoryTest extends IntegrationTestBase {

	@Autowired
	private FichaRepository fichas;

	@Autowired
	private EntityManager em;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	@DisplayName("grava e relê todos os campos, com perícias, salvaguardas, inspiração e PV temporário")
	void idaEVolta() {
		FichaDnd ficha = FichasDeTeste.dnd("Thorin");
		ficha.setEdicao(EdicaoDnd.D2014);
		ficha.setClasse("Guerreiro");
		ficha.setEspecie("Anão");
		ficha.setAntecedente("Soldado");
		ficha.setNivel(5);
		ficha.setAtributo(AtributoDnd.FOR, 16);
		ficha.setAtributo(AtributoDnd.DES, 14);
		ficha.setAtributo(AtributoDnd.CON, 30);
		ficha.setAtributo(AtributoDnd.INT, 8);
		ficha.setAtributo(AtributoDnd.SAB, 12);
		ficha.setAtributo(AtributoDnd.CAR, 1);
		ficha.setPvAtual(30);
		ficha.setPvMax(44);
		ficha.setPvTemporario(7);
		ficha.setCa(18);
		ficha.setInspiracao(true);
		ficha.setSalvaguardas(EnumSet.of(AtributoDnd.DES, AtributoDnd.INT));
		ficha.getPericias().put(PericiaDnd.FURTIVIDADE, GrauDnd.ESPECIALISTA);
		ficha.getPericias().put(PericiaDnd.ATLETISMO, GrauDnd.PROFICIENTE);

		Long id = fichas.save(ficha).getId();
		em.flush();
		em.clear();

		Ficha lida = fichas.findById(id).orElseThrow();
		assertThat(lida).isInstanceOf(FichaDnd.class).isNotSameAs(ficha);
		FichaDnd dnd = (FichaDnd) lida;
		assertThat(dnd.getSistema()).isEqualTo(Sistema.DND);
		assertThat(dnd.getJogador()).isEqualTo("Ana");
		assertThat(dnd.getPersonagem()).isEqualTo("Thorin");
		assertThat(dnd.getEdicao()).isEqualTo(EdicaoDnd.D2014);
		assertThat(dnd.getClasse()).isEqualTo("Guerreiro");
		assertThat(dnd.getEspecie()).isEqualTo("Anão");
		assertThat(dnd.getAntecedente()).isEqualTo("Soldado");
		assertThat(dnd.getNivel()).isEqualTo(5);
		assertThat(dnd.getAtributo(AtributoDnd.FOR)).isEqualTo(16);
		assertThat(dnd.getAtributo(AtributoDnd.DES)).isEqualTo(14);
		assertThat(dnd.getAtributo(AtributoDnd.CON)).isEqualTo(30);
		assertThat(dnd.getAtributo(AtributoDnd.INT)).isEqualTo(8);
		assertThat(dnd.getAtributo(AtributoDnd.SAB)).isEqualTo(12);
		assertThat(dnd.getAtributo(AtributoDnd.CAR)).isEqualTo(1);
		assertThat(dnd.getPvAtual()).isEqualTo(30);
		assertThat(dnd.getPvMax()).isEqualTo(44);
		assertThat(dnd.getPvTemporario()).isEqualTo(7);
		assertThat(dnd.getCa()).isEqualTo(18);
		assertThat(dnd.isInspiracao()).isTrue();
		for (AtributoDnd atributo : AtributoDnd.values()) {
			assertThat(dnd.isProficienteEmSalvaguarda(atributo))
					.as("salvaguarda %s", atributo)
					.isEqualTo(atributo == AtributoDnd.DES || atributo == AtributoDnd.INT);
		}
		assertThat(dnd.getPericias()).containsOnly(
				Map.entry(PericiaDnd.FURTIVIDADE, GrauDnd.ESPECIALISTA),
				Map.entry(PericiaDnd.ATLETISMO, GrauDnd.PROFICIENTE));
	}

	@Test
	@DisplayName("recursos opcionais voltam nulos e a inspiração padrão é falsa")
	void padroes() {
		Long id = fichas.save(FichasDeTeste.dnd("Lia")).getId();
		em.flush();
		em.clear();

		FichaDnd dnd = (FichaDnd) fichas.findById(id).orElseThrow();
		assertThat(dnd.getPvAtual()).isNull();
		assertThat(dnd.getPvMax()).isNull();
		assertThat(dnd.getPvTemporario()).isNull();
		assertThat(dnd.getCa()).isNull();
		assertThat(dnd.isInspiracao()).isFalse();
		assertThat(dnd.getPericias()).isEmpty();
	}

	@Test
	@DisplayName("excluir a ficha apaga as perícias (on delete cascade)")
	void excluirApagaPericias() {
		FichaDnd ficha = FichasDeTeste.dnd("Lia");
		ficha.getPericias().put(PericiaDnd.ARCANISMO, GrauDnd.PROFICIENTE);
		Long id = fichas.save(ficha).getId();
		em.flush();
		em.clear();
		assertThat(jdbc.queryForObject("select count(*) from ficha_dnd_pericia where ficha_id = ?", Integer.class, id))
				.isEqualTo(1);

		fichas.deleteById(id);
		em.flush();

		assertThat(jdbc.queryForObject("select count(*) from ficha_dnd_pericia where ficha_id = ?", Integer.class, id))
				.isZero();
		assertThat(jdbc.queryForObject("select count(*) from ficha_dnd where ficha_id = ?", Integer.class, id))
				.isZero();
	}
}
