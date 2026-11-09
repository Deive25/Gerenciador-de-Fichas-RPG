package dev.spironello.fichasrpg.service;

import static org.mockito.Mockito.mock;

import java.util.List;

import dev.spironello.fichasrpg.dto.DescricaoFicha;
import dev.spironello.fichasrpg.repository.HabilidadeFichaRepository;

/** Montagens das partes comuns para os testes unitários, que não sobem o Spring nem o banco. */
final class PartesComunsDeTeste {

	/** Descrição toda nula e nenhuma habilidade. */
	static final PartesComuns VAZIAS = new PartesComuns(new DescricaoFicha(null, null, null), List.of());

	private PartesComunsDeTeste() {
	}

	/** Componente de verdade sobre um repositório de mentira: sem habilidades, descrição lida da ficha. */
	static PartesComunsDaFicha componente() {
		return new PartesComunsDaFicha(mock(HabilidadeFichaRepository.class));
	}
}
