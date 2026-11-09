package dev.spironello.fichasrpg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.RecordComponent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.spironello.fichasrpg.dto.FichaOrdemRequest;
import dev.spironello.fichasrpg.dto.FichaOrdemResponse;
import dev.spironello.fichasrpg.dto.RecursoValor;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.exception.FichaNaoEncontradaException;
import dev.spironello.fichasrpg.exception.SistemaImutavelException;
import dev.spironello.fichasrpg.model.AtributoOrdem;
import dev.spironello.fichasrpg.model.ClasseOrdem;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.GrauOrdem;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.model.RecursoOrdem;
import dev.spironello.fichasrpg.model.Sistema;
import dev.spironello.fichasrpg.repository.FichaRepository;

/**
 * Testes unitários do serviço: só Mockito, sem Spring e sem Docker. O repository é um mock;
 * o mapper é o de verdade, porque converter todos os campos faz parte do que se quer provar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FichaService (unitário)")
class FichaServiceTest {

	private static final Long ID = 7L;

	@Mock
	private FichaRepository repository;

	private FichaService service;

	@BeforeEach
	void criarServico() {
		service = new FichaService(repository, new SistemasDeFicha(List.of(new SistemaOrdem(new FichaOrdemMapper()))));
	}

	/** Ficha de outro sistema, só para os testes: no 2.1d existe apenas Ordem, mas a regra do 409 já vale. */
	private static class FichaDeOutroSistema extends Ficha {
		@Override
		public Sistema getSistema() {
			return Sistema.DND;
		}
	}

	@Test
	@DisplayName("buscar com id inexistente lança FichaNaoEncontradaException")
	void buscarInexistente() {
		when(repository.findById(ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscar(ID))
				.isInstanceOf(FichaNaoEncontradaException.class)
				.hasMessageContaining("7");
	}

	@Test
	@DisplayName("excluir com id inexistente lança e não chama delete")
	void excluirInexistente() {
		when(repository.existsById(ID)).thenReturn(false);

		assertThatThrownBy(() -> service.excluir(ID))
				.isInstanceOf(FichaNaoEncontradaException.class);

		verify(repository, never()).deleteById(any());
		verify(repository, never()).delete(any());
	}

	@Test
	@DisplayName("excluir com id existente chama deleteById")
	void excluirExistente() {
		when(repository.existsById(ID)).thenReturn(true);

		service.excluir(ID);

		verify(repository).deleteById(ID);
	}

	@Test
	@DisplayName("atualizar com id inexistente lança e não salva nada")
	void atualizarInexistente() {
		when(repository.findById(ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.atualizar(ID, requestComTudoPreenchido(0)))
				.isInstanceOf(FichaNaoEncontradaException.class);

		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("atualizar uma ficha de outro sistema lança SistemaImutavelException (409) e não salva nada")
	void atualizarDeOutroSistema() {
		when(repository.findById(ID)).thenReturn(Optional.of(new FichaDeOutroSistema()));

		assertThatThrownBy(() -> service.atualizar(ID, requestComTudoPreenchido(0)))
				.isInstanceOf(SistemaImutavelException.class)
				.hasMessage("O sistema de uma ficha não muda.");

		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("atualizar copia todos os campos do pedido (substituição completa), inclusive os nulos")
	void atualizarCopiaTodosOsCampos() throws Exception {
		// Ficha existente com tudo preenchido; o pedido traz outros valores e uma perícia nula.
		FichaOrdem existente = new FichaOrdem();
		existente.setId(ID);
		FichaOrdemMapper mapper = new FichaOrdemMapper();
		mapper.copiar(requestComTudoPreenchido(0), existente);
		existente.getPericias().put(PericiaOrdem.TATICA, GrauOrdem.TREINADO); // o pedido não a traz: precisa sumir
		FichaOrdemRequest novo = requestComTudoPreenchido(1);
		when(repository.findById(ID)).thenReturn(Optional.of(existente));
		when(repository.save(existente)).thenReturn(existente);

		FichaOrdemResponse resposta = (FichaOrdemResponse) service.atualizar(ID, novo);

		assertThat(resposta.id()).isEqualTo(ID);
		assertThat(resposta.sistema()).isEqualTo(Sistema.ORDEM_PARANORMAL);
		for (RecordComponent campo : FichaOrdemRequest.class.getRecordComponents()) {
			Object esperado = campo.getAccessor().invoke(novo);
			Object obtido = FichaOrdemResponse.class.getMethod(campo.getName()).invoke(resposta);
			if (campo.getName().equals("pericias")) { // grau null no pedido = ausente na resposta
				esperado = ((Map<?, ?>) esperado).entrySet().stream().filter(e -> e.getValue() != null)
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			}
			assertThat(obtido).as("campo %s", campo.getName()).isEqualTo(esperado);
		}
		assertThat(novo.pericias()).containsEntry(PericiaOrdem.LUTA, null); // o teste cobre também grau nulo
		assertThat(existente.getPericias()).doesNotContainKey(PericiaOrdem.TATICA); // e o que não veio saiu da ficha
		assertThat(existente.getPericias()).doesNotContainKey(PericiaOrdem.LUTA);
	}

	@Test
	@DisplayName("atualizar com grau incompatível com o NEX lança DadosInvalidosException e não salva")
	void atualizarComGrauIncompativel() {
		FichaOrdem existente = new FichaOrdem();
		existente.setId(ID);
		when(repository.findById(ID)).thenReturn(Optional.of(existente));
		FichaOrdemRequest veteranoComNexBaixo = new FichaOrdemRequest("a", "b", "c", null, null, null, 30,
				atributos(1), null, Map.of(PericiaOrdem.ATLETISMO, GrauOrdem.VETERANO));

		assertThatThrownBy(() -> service.atualizar(ID, veteranoComNexBaixo))
				.isInstanceOfSatisfying(DadosInvalidosException.class, e -> assertThat(e.getErros())
						.containsEntry("pericias.ATLETISMO", "Veterano exige NEX 35% ou mais"));

		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("criar ignora qualquer id: a entidade vai ao repository sem id e o id vem do banco")
	void criarIgnoraId() {
		when(repository.save(any(Ficha.class))).thenAnswer(chamada -> {
			Ficha salva = chamada.getArgument(0);
			assertThat(salva.getId()).isNull(); // nenhum id chega ao banco
			salva.setId(55L); // o banco gera o id
			return salva;
		});

		FichaOrdemResponse resposta = (FichaOrdemResponse) service.criar(requestComTudoPreenchido(0));

		verify(repository).save(any(FichaOrdem.class));
		assertThat(resposta.id()).isEqualTo(55L);
		assertThat(resposta.jogador()).isEqualTo("nome-jogador");
	}

	@Test
	@DisplayName("criar com atributo faltando lança DadosInvalidosException e não salva")
	void criarComAtributoFaltando() {
		FichaOrdemRequest semVigor = new FichaOrdemRequest("a", "b", "c", null, null, null, 5,
				Map.of(AtributoOrdem.AGI, 1, AtributoOrdem.FOR, 1, AtributoOrdem.INT, 1, AtributoOrdem.PRE, 1),
				null, null);

		assertThatThrownBy(() -> service.criar(semVigor))
				.isInstanceOfSatisfying(DadosInvalidosException.class,
						e -> assertThat(e.getErros()).containsEntry("atributos.VIG", "é obrigatório"));

		verify(repository, never()).save(any());
	}

	// ---------- apoio ----------

	private static Map<AtributoOrdem, Integer> atributos(int valor) {
		Map<AtributoOrdem, Integer> atributos = new EnumMap<>(AtributoOrdem.class);
		for (AtributoOrdem atributo : AtributoOrdem.values()) {
			atributos.put(atributo, valor);
		}
		return atributos;
	}

	/**
	 * Pedido com todos os componentes preenchidos; os números mudam com a base, para o teste distinguir a
	 * ficha antiga da nova. A perícia LUTA vai com grau null de propósito.
	 */
	private static FichaOrdemRequest requestComTudoPreenchido(int base) {
		Map<RecursoOrdem, RecursoValor> recursos = new EnumMap<>(RecursoOrdem.class);
		for (RecursoOrdem recurso : RecursoOrdem.values()) {
			recursos.put(recurso, new RecursoValor(base + recurso.ordinal(), base + 10 + recurso.ordinal()));
		}
		Map<PericiaOrdem, GrauOrdem> pericias = new EnumMap<>(PericiaOrdem.class);
		pericias.put(PericiaOrdem.ACROBACIA, GrauOrdem.TREINADO);
		pericias.put(PericiaOrdem.CRIME, GrauOrdem.VETERANO);
		pericias.put(PericiaOrdem.LUTA, null);
		return new FichaOrdemRequest("nome-jogador", "nome-personagem", "nome-genero", 20 + base,
				ClasseOrdem.values()[base], "nome-origem", 40 + base, atributos(2 + base), recursos, pericias);
	}
}
