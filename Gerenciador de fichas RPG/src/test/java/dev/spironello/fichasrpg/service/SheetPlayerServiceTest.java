package dev.spironello.fichasrpg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.RecordComponent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.spironello.fichasrpg.dto.SheetPlayerRequest;
import dev.spironello.fichasrpg.dto.SheetPlayerResponse;
import dev.spironello.fichasrpg.exception.SheetPlayerNotFoundException;
import dev.spironello.fichasrpg.model.Pericia;
import dev.spironello.fichasrpg.model.SheetPlayer;
import dev.spironello.fichasrpg.repository.SheetPlayerRepository;

/**
 * Testes unitários do serviço: só Mockito, sem Spring e sem Docker. O repository é um mock;
 * o mapper é o de verdade, porque converter todos os campos faz parte do que se quer provar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SheetPlayerService (unitário)")
class SheetPlayerServiceTest {

	private static final Long ID = 7L;

	@Mock
	private SheetPlayerRepository repository;

	private SheetPlayerService service;

	@BeforeEach
	void criarServico() {
		service = new SheetPlayerService(repository, new SheetPlayerMapper());
	}

	@Test
	@DisplayName("buscar com id inexistente lança SheetPlayerNotFoundException")
	void buscarInexistente() {
		when(repository.findById(ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscar(ID))
				.isInstanceOf(SheetPlayerNotFoundException.class)
				.hasMessageContaining("7");
	}

	@Test
	@DisplayName("excluir com id inexistente lança e não chama delete")
	void excluirInexistente() {
		when(repository.existsById(ID)).thenReturn(false);

		assertThatThrownBy(() -> service.excluir(ID))
				.isInstanceOf(SheetPlayerNotFoundException.class);

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

		assertThatThrownBy(() -> service.atualizar(ID, requestComTudoPreenchido()))
				.isInstanceOf(SheetPlayerNotFoundException.class);

		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("atualizar copia todos os campos do pedido (substituição completa), inclusive os nulos")
	void atualizarCopiaTodosOsCampos() throws Exception {
		// Ficha existente com tudo preenchido; o pedido traz outros valores e alguns nulos.
		SheetPlayer existente = new SheetPlayer();
		existente.setId(ID);
		new SheetPlayerMapper().copyTo(montarRequest(0), existente);
		existente.getPericias().put(Pericia.ADESTRAR_ANIMAIS, 999); // valor que o pedido (null) precisa apagar
		SheetPlayerRequest novo = montarRequest(100); // valores diferentes dos da ficha existente
		when(repository.findById(ID)).thenReturn(Optional.of(existente));
		when(repository.save(existente)).thenReturn(existente);

		SheetPlayerResponse resposta = service.atualizar(ID, novo);

		assertThat(resposta.id()).isEqualTo(ID);
		for (RecordComponent campo : SheetPlayerRequest.class.getRecordComponents()) {
			Object esperado = campo.getAccessor().invoke(novo);
			Object obtido = SheetPlayerResponse.class.getMethod(campo.getName()).invoke(resposta);
			if (esperado instanceof Map<?, ?> mapa) { // perícia null no pedido = ausente na resposta
				esperado = mapa.entrySet().stream().filter(e -> e.getValue() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			}
			assertThat(obtido).as("campo %s", campo.getName()).isEqualTo(esperado);
		}
		assertThat(novo.pericias()).containsEntry(Pericia.ADESTRAR_ANIMAIS, null); // o teste cobre também campos nulos
		assertThat(existente.getPericias()).doesNotContainKey(Pericia.ADESTRAR_ANIMAIS); // e o nulo removeu a entrada da ficha
	}

	@Test
	@DisplayName("criar ignora qualquer id: a entidade vai ao repository sem id e o id vem do banco")
	void criarIgnoraId() {
		when(repository.save(any(SheetPlayer.class))).thenAnswer(chamada -> {
			SheetPlayer salva = chamada.getArgument(0);
			assertThat(salva.getId()).isNull(); // nenhum id chega ao banco
			salva.setId(55L); // o banco gera o id
			return salva;
		});

		SheetPlayerResponse resposta = service.criar(requestComTudoPreenchido());

		verify(repository).save(any(SheetPlayer.class));
		assertThat(resposta.id()).isEqualTo(55L);
		assertThat(resposta.jogador()).isEqualTo("nome-jogador");
	}

	// ---------- apoio ----------

	private SheetPlayerRequest requestComTudoPreenchido() {
		return montarRequest(0);
	}

	/**
	 * Monta um pedido com todos os componentes preenchidos por reflexão (String "nome-campo",
	 * Integer = base + posição), assim o teste cobre os campos sem listá-los um a um.
	 * As perícias ADESTRAR_ANIMAIS e ATUACAO_LABIA vão com valor null de propósito.
	 */
	private SheetPlayerRequest montarRequest(int base) {
		try {
			RecordComponent[] campos = SheetPlayerRequest.class.getRecordComponents();
			Class<?>[] tipos = new Class<?>[campos.length];
			Object[] valores = new Object[campos.length];
			for (int i = 0; i < campos.length; i++) {
				tipos[i] = campos[i].getType();
				if (campos[i].getType() == String.class) {
					valores[i] = "nome-" + campos[i].getName();
				} else if (campos[i].getType() == Map.class) {
					Map<Pericia, Integer> pericias = new EnumMap<>(Pericia.class);
					for (Pericia p : Pericia.values()) {
						boolean nula = p == Pericia.ADESTRAR_ANIMAIS || p == Pericia.ATUACAO_LABIA;
						pericias.put(p, nula ? null : base + p.ordinal());
					}
					valores[i] = pericias;
				} else {
					valores[i] = Integer.valueOf(base + i);
				}
			}
			return SheetPlayerRequest.class.getDeclaredConstructor(tipos).newInstance(valores);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}
