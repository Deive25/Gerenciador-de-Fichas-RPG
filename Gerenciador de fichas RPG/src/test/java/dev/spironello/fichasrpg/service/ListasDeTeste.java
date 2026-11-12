package dev.spironello.fichasrpg.service;

import static org.mockito.Mockito.mock;

import java.util.List;

import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.repository.AtaqueDndRepository;
import dev.spironello.fichasrpg.repository.AtaqueOrdemRepository;
import dev.spironello.fichasrpg.repository.ItemDndRepository;
import dev.spironello.fichasrpg.repository.ItemOrdemRepository;
import dev.spironello.fichasrpg.repository.MagiaDndRepository;
import dev.spironello.fichasrpg.repository.RitualOrdemRepository;

/** Montagens das listas da ficha (itens, magias, espaços e rituais) para os testes unitários, que não sobem o Spring nem o banco. */
final class ListasDeTeste {

	/** Nenhum ataque, item nem ritual. */
	static final ListasOrdem VAZIAS_ORDEM = new ListasOrdem(List.of(), List.of(), List.of());
	/** Nenhum ataque, item nem magia, e os nove níveis de espaços em 0 de 0. */
	static final ListasDnd VAZIAS_DND = new ListasDnd(List.of(), List.of(), List.of(), EspacosDaFicha.de(new FichaDnd()));

	private ListasDeTeste() {
	}

	/** Componente de verdade sobre um repositório de mentira: sem ataques. */
	static AtaquesOrdem ataquesOrdem() {
		return new AtaquesOrdem(mock(AtaqueOrdemRepository.class));
	}

	static AtaquesDnd ataquesDnd() {
		return new AtaquesDnd(mock(AtaqueDndRepository.class));
	}

	/** Componente de verdade sobre um repositório de mentira: sem itens. */
	static ItensOrdem itensOrdem() {
		return new ItensOrdem(mock(ItemOrdemRepository.class));
	}

	static ItensDnd itensDnd() {
		return new ItensDnd(mock(ItemDndRepository.class));
	}

	static RituaisOrdem rituaisOrdem() {
		return new RituaisOrdem(mock(RitualOrdemRepository.class));
	}

	static MagiasDnd magiasDnd() {
		return new MagiasDnd(mock(MagiaDndRepository.class));
	}
}
