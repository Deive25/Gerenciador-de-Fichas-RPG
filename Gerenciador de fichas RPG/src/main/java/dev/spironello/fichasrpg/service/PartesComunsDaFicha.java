package dev.spironello.fichasrpg.service;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.dto.DescricaoFicha;
import dev.spironello.fichasrpg.dto.HabilidadeResponse;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.repository.HabilidadeFichaRepository;

/**
 * Monta as partes comuns da ficha completa (descrição e habilidades). Só a resposta completa usa isto:
 * o resumo da listagem não chama, e é por isso que a listagem continua em uma consulta. Chame dentro
 * da transação.
 */
@Component
public class PartesComunsDaFicha {

	private final HabilidadeFichaRepository habilidades;

	public PartesComunsDaFicha(HabilidadeFichaRepository habilidades) {
		this.habilidades = habilidades;
	}

	public PartesComuns de(Ficha ficha) {
		DescricaoFicha descricao = new DescricaoFicha(ficha.getAparencia(), ficha.getHistoria(),
				ficha.getAnotacoes());
		return new PartesComuns(descricao, habilidades.findByFichaIdOrderById(ficha.getId()).stream()
				.map(h -> new HabilidadeResponse(h.getId(), h.getNome(), h.getTipo(), h.getDescricao()))
				.toList());
	}
}
