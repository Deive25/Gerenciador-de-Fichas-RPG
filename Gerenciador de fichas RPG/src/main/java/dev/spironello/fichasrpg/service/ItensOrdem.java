package dev.spironello.fichasrpg.service;

import java.util.List;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.dto.ItemOrdemResponse;
import dev.spironello.fichasrpg.repository.ItemOrdemRepository;

/** Os itens do inventário das fichas de Ordem Paranormal. Chame dentro da transação. */
@Component
public class ItensOrdem {

	private final ItemOrdemRepository itens;

	public ItensOrdem(ItemOrdemRepository itens) {
		this.itens = itens;
	}

	/** Os itens da ficha pela ordem de criação. */
	public List<ItemOrdemResponse> listar(Long fichaId) {
		return itens.findByFichaIdOrderById(fichaId).stream()
				.map(i -> new ItemOrdemResponse(i.getId(), i.getNome(), i.getCategoria(), i.getEspacos(),
						i.getQuantidade(), i.getDescricao()))
				.toList();
	}
}
