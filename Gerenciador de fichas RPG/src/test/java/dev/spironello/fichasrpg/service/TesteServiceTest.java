package dev.spironello.fichasrpg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.dto.AlvoTeste;
import dev.spironello.fichasrpg.dto.ModoTeste;
import dev.spironello.fichasrpg.dto.TesteRequest;
import dev.spironello.fichasrpg.dto.TesteResponse;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.exception.FichaNaoEncontradaException;
import dev.spironello.fichasrpg.model.AtributoOrdem;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.GrauOrdem;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.repository.FichaRepository;

/** Testes unitários do serviço de rolagem: Mockito puro, sem Spring e sem Docker. */
@ExtendWith({ MockitoExtension.class, OutputCaptureExtension.class })
@DisplayName("TesteService (unitário)")
class TesteServiceTest {

	private static final Long ID = 7L;

	@Mock
	private FichaRepository repository;

	@Mock
	private Dado dado;

	private TesteService service;

	@BeforeEach
	void criarServico() {
		service = new TesteService(repository, new SistemasDeFicha(List.of(new SistemaOrdem(new FichaOrdemMapper(), PartesComunsDeTeste.componente()))), dado);
	}

	private FichaOrdem fichaComIntelecto3() {
		FichaOrdem ficha = FichasDeTeste.ordem("Ana");
		ficha.setAtributo(AtributoOrdem.INT, 3);
		ficha.getPericias().put(PericiaOrdem.INVESTIGACAO, GrauOrdem.TREINADO);
		return ficha;
	}

	@Test
	@DisplayName("perícia usa o atributo-base e o grau da ficha")
	void periciaUsaAtributoEGrau() {
		when(repository.findById(ID)).thenReturn(Optional.of(fichaComIntelecto3()));
		when(dado.rolar(20)).thenReturn(4, 17, 9);

		TesteResponse resposta = service.testar(ID, new TesteRequest(AlvoTeste.PERICIA, "INVESTIGACAO", null));

		assertThat(resposta).isEqualTo(new TesteResponse(AlvoTeste.PERICIA, "INVESTIGACAO", "Investigação",
				List.of(4, 17, 9), 17, 5, 22));
	}

	@Test
	@DisplayName("atributo rola sem bônus e devolve o nome do atributo")
	void atributoSemBonus() {
		when(repository.findById(ID)).thenReturn(Optional.of(fichaComIntelecto3()));
		when(dado.rolar(20)).thenReturn(4, 17, 9);

		TesteResponse resposta = service.testar(ID, new TesteRequest(AlvoTeste.ATRIBUTO, "INT", ModoTeste.NORMAL));

		assertThat(resposta.nome()).isEqualTo("Intelecto");
		assertThat(resposta.bonus()).isZero();
		assertThat(resposta.total()).isEqualTo(17);
	}

	@Test
	@DisplayName("ficha inexistente lança FichaNaoEncontradaException")
	void fichaInexistente() {
		when(repository.findById(ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.testar(ID, new TesteRequest(AlvoTeste.PERICIA, "LUTA", null)))
				.isInstanceOf(FichaNaoEncontradaException.class);
	}

	@Test
	@DisplayName("salvaguarda, modo diferente de NORMAL e código inválido viram erros por campo")
	void erros() {
		when(repository.findById(ID)).thenReturn(Optional.of(fichaComIntelecto3()));

		assertThatThrownBy(() -> service.testar(ID, new TesteRequest(AlvoTeste.SALVAGUARDA, "VOAR", ModoTeste.VANTAGEM)))
				.isInstanceOfSatisfying(DadosInvalidosException.class, e ->
						assertThat(e.getErros()).containsOnlyKeys("alvo", "modo"));
		assertThatThrownBy(() -> service.testar(ID, new TesteRequest(AlvoTeste.PERICIA, "VOAR", null)))
				.isInstanceOfSatisfying(DadosInvalidosException.class, e ->
						assertThat(e.getErros()).containsEntry("codigo", "não existe em Ordem Paranormal"));
	}

	@Test
	@DisplayName("o log traz id, código e total, e nunca a lista de dados")
	void logSemOsDados(CapturedOutput saida) {
		when(repository.findById(ID)).thenReturn(Optional.of(fichaComIntelecto3()));
		when(dado.rolar(20)).thenReturn(4, 17, 9);

		service.testar(ID, new TesteRequest(AlvoTeste.PERICIA, "INVESTIGACAO", null));

		assertThat(saida.getOut()).contains("ficha=7", "INVESTIGACAO", "total=22")
				.doesNotContain("[4, 17, 9]").doesNotContain("Ana");
	}
}
