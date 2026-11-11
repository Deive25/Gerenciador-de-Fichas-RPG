package dev.spironello.fichasrpg.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.dto.ItemDndResponse;
import dev.spironello.fichasrpg.dto.ItemRequest;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.exception.ItemDaFichaNaoEncontradoException;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.ItemDnd;
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
		return itens.findByFichaIdOrderById(fichaId).stream().map(ItensDnd::paraResponse).toList();
	}

	public ItemDndResponse criar(FichaDnd ficha, ItemRequest request) {
		conferir(request);
		ItemDnd item = new ItemDnd();
		item.setFicha(ficha);
		preencher(item, request);
		return paraResponse(itens.save(item));
	}

	public ItemDndResponse atualizar(FichaDnd ficha, Long itemId, ItemRequest request) {
		ItemDnd item = buscar(ficha, itemId);
		conferir(request);
		preencher(item, request);
		return paraResponse(item);
	}

	public void excluir(FichaDnd ficha, Long itemId) {
		itens.delete(buscar(ficha, itemId));
	}

	/** findByIdAndFichaId também cobre o item de outra ficha: para esta ficha, ele não existe. */
	private ItemDnd buscar(FichaDnd ficha, Long itemId) {
		return itens.findByIdAndFichaId(itemId, ficha.getId())
				.orElseThrow(() -> new ItemDaFichaNaoEncontradoException("item", itemId));
	}

	/** Confere os campos do sistema e devolve todos os erros de uma vez. */
	private void conferir(ItemRequest request) {
		Map<String, String> erros = new LinkedHashMap<>();
		if (request.peso() == null) {
			erros.put("peso", "é obrigatório");
		}
		if (request.categoria() != null) {
			erros.put("categoria", "não existe em D&D");
		}
		if (request.espacos() != null) {
			erros.put("espacos", "não existe em D&D");
		}
		if (!erros.isEmpty()) {
			throw new DadosInvalidosException(erros);
		}
	}

	private void preencher(ItemDnd item, ItemRequest request) {
		item.setNome(request.nome());
		item.setQuantidade(request.quantidade());
		item.setPeso(request.peso());
		item.setEquipado(Boolean.TRUE.equals(request.equipado()));
		String descricao = request.descricao();
		item.setDescricao(descricao == null || descricao.isBlank() ? null : descricao);
	}

	private static ItemDndResponse paraResponse(ItemDnd i) {
		return new ItemDndResponse(i.getId(), i.getNome(), i.getQuantidade(), i.getPeso(), i.isEquipado(),
				i.getDescricao());
	}
}
