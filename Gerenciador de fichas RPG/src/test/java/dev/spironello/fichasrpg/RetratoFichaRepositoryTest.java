package dev.spironello.fichasrpg;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.RetratoFicha;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.repository.RetratoFichaRepository;

@DisplayName("Retrato da ficha no banco")
class RetratoFichaRepositoryTest extends IntegrationTestBase {

	@Autowired
	private FichaRepository fichas;

	@Autowired
	private RetratoFichaRepository retratos;

	@Test
	@DisplayName("grava e lê os bytes e o instante")
	void gravaELe() {
		Ficha ficha = novaFicha();
		Instant instante = Instant.parse("2026-01-01T00:00:00.123Z");
		retratos.save(new RetratoFicha(ficha.getId(), new byte[] { 1, 2, 3 }, instante));

		RetratoFicha lido = retratos.findById(ficha.getId()).orElseThrow();
		assertThat(lido.getConteudo()).containsExactly(1, 2, 3);
		assertThat(lido.getAtualizadoEm()).isEqualTo(instante);
	}

	@Test
	@DisplayName("excluir a ficha apaga o retrato junto (on delete cascade)")
	void excluirFichaApagaRetrato() {
		Ficha ficha = novaFicha();
		retratos.save(new RetratoFicha(ficha.getId(), new byte[] { 9 }, Instant.parse("2026-01-01T00:00:00Z")));

		fichas.deleteById(ficha.getId());

		assertThat(retratos.existsById(ficha.getId())).isFalse();
	}

	private Ficha novaFicha() {
		return fichas.save(FichasDeTeste.ordem("Lia"));
	}
}
