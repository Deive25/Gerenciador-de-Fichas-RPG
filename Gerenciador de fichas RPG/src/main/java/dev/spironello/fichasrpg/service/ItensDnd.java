package dev.spironello.fichasrpg.service;

import java.util.List;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.dto.ItemDndResponse;
import dev.spironello.fichasrpg.repository.ItemDndRepository;

/** Os itens do inventário das fichas de D&D. Chame dentro da transação. */
@Component
public class ItensDnd {

	private final ItemDndRepository itens;

	public ItensDnd(ItemDndRepository itens) {
		this.itens = itens;
	}

	/** Os itens da ficha pela ordem de criação. */
	public List<ItemDndResponse> listar(Long fichaId) {
		return itens.findByFichaIdOrderById(fichaId).stream()
				.map(i -> new ItemDndResponse(i.getId(), i.getNome(), i.getQuantidade(), i.getPeso(),
						i.isEquipado(), i.getDescricao()))
				.toList();
	}
}
