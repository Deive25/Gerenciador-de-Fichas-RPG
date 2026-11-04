package dev.spironello.fichasrpg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.spironello.fichasrpg.dto.TestePericiaResponse;
import dev.spironello.fichasrpg.exception.SheetPlayerNotFoundException;
import dev.spironello.fichasrpg.model.Pericia;
import dev.spironello.fichasrpg.model.ResultadoTeste;
import dev.spironello.fichasrpg.model.SheetPlayer;
import dev.spironello.fichasrpg.repository.SheetPlayerRepository;

/**
 * Unitário: repository em Mockito e um {@link Dado} fixo (lambda). Não há mock de Random:
 * a aleatoriedade é substituída por uma rolagem conhecida.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TestePericiaService (unitário)")
class TestePericiaServiceTest {

	private static final Long ID = 3L;

	@Mock
	private SheetPlayerRepository repository;

	private TestePericiaService servico(int rolagemFixa) {
		Dado dadoFixo = () -> rolagemFixa;
		return new TestePericiaService(repository, dadoFixo);
	}

	@Test
	@DisplayName("ficha com a perícia: usa o valor da ficha e a rolagem do dado")
	void fichaComValor() {
		SheetPlayer ficha = new SheetPlayer();
		ficha.getPericias().put(Pericia.ACROBACIA, 40);
		when(repository.findById(ID)).thenReturn(Optional.of(ficha));

		TestePericiaResponse resposta = servico(7).testar(ID, Pericia.ACROBACIA);

		assertThat(resposta).isEqualTo(
				new TestePericiaResponse(Pericia.ACROBACIA, "Acrobacia", 40, 7, ResultadoTeste.SUCESSO_EXTREMO));
	}

	@Test
	@DisplayName("ficha sem a perícia: o valor é 0 e o teste sempre falha")
	void fichaSemAPericia() {
		SheetPlayer ficha = new SheetPlayer();
		ficha.getPericias().put(Pericia.ACROBACIA, 40);
		when(repository.findById(ID)).thenReturn(Optional.of(ficha));

		TestePericiaResponse resposta = servico(1).testar(ID, Pericia.CHARME);

		assertThat(resposta.valor()).isZero();
		assertThat(resposta.rolagem()).isEqualTo(1);
		assertThat(resposta.resultado()).isEqualTo(ResultadoTeste.FALHA);
	}

	@Test
	@DisplayName("ficha inexistente lança SheetPlayerNotFoundException")
	void fichaInexistente() {
		when(repository.findById(ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servico(50).testar(ID, Pericia.ACROBACIA))
				.isInstanceOf(SheetPlayerNotFoundException.class)
				.hasMessageContaining("3");
	}
}
