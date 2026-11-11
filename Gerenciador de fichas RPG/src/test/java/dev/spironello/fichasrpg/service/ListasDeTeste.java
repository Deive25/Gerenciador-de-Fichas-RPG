package dev.spironello.fichasrpg.service;

import static org.mockito.Mockito.mock;

import java.util.List;

import dev.spironello.fichasrpg.repository.ItemDndRepository;
import dev.spironello.fichasrpg.repository.ItemOrdemRepository;

/** Montagens das listas da ficha (itens) para os testes unitários, que não sobem o Spring nem o banco. */
final class ListasDeTeste {

	/** Nenhum item. */
	static final ListasOrdem VAZIAS_ORDEM = new ListasOrdem(List.of());
	static final ListasDnd VAZIAS_DND = new ListasDnd(List.of());

	private ListasDeTeste() {
	}

	/** Componente de verdade sobre um repositório de mentira: sem itens. */
	static ItensOrdem itensOrdem() {
		return new ItensOrdem(mock(ItemOrdemRepository.class));
	}

	static ItensDnd itensDnd() {
		return new ItensDnd(mock(ItemDndRepository.class));
	}
}
