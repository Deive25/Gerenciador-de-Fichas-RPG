package dev.spironello.fichasrpg.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.dto.ItemOrdemResponse;
import dev.spironello.fichasrpg.dto.ItemRequest;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.exception.ItemDaFichaNaoEncontradoException;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.ItemOrdem;
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
		return itens.findByFichaIdOrderById(fichaId).stream().map(ItensOrdem::paraResponse).toList();
	}

	public ItemOrdemResponse criar(FichaOrdem ficha, ItemRequest request) {
		conferir(request);
		ItemOrdem item = new ItemOrdem();
		item.setFicha(ficha);
		preencher(item, request);
		return paraResponse(itens.save(item));
	}

	public ItemOrdemResponse atualizar(FichaOrdem ficha, Long itemId, ItemRequest request) {
		ItemOrdem item = buscar(ficha, itemId);
		conferir(request);
		preencher(item, request);
		return paraResponse(item);
	}

	public void excluir(FichaOrdem ficha, Long itemId) {
		itens.delete(buscar(ficha, itemId));
	}

	/** findByIdAndFichaId também cobre o item de outra ficha: para esta ficha, ele não existe. */
	private ItemOrdem buscar(FichaOrdem ficha, Long itemId) {
		return itens.findByIdAndFichaId(itemId, ficha.getId())
				.orElseThrow(() -> new ItemDaFichaNaoEncontradoException("item", itemId));
	}

	/** Confere os campos do sistema e devolve todos os erros de uma vez. */
	private void conferir(ItemRequest request) {
		Map<String, String> erros = new LinkedHashMap<>();
		if (request.categoria() == null) {
			erros.put("categoria", "é obrigatório");
		}
		if (request.espacos() == null) {
			erros.put("espacos", "é obrigatório");
		}
		if (request.peso() != null) {
			erros.put("peso", "não existe em Ordem Paranormal");
		}
		if (request.equipado() != null) {
			erros.put("equipado", "não existe em Ordem Paranormal");
		}
		if (!erros.isEmpty()) {
			throw new DadosInvalidosException(erros);
		}
	}

	private void preencher(ItemOrdem item, ItemRequest request) {
		item.setNome(request.nome());
		item.setCategoria(request.categoria());
		item.setEspacos(request.espacos());
		item.setQuantidade(request.quantidade());
		String descricao = request.descricao();
		item.setDescricao(descricao == null || descricao.isBlank() ? null : descricao);
	}

	private static ItemOrdemResponse paraResponse(ItemOrdem i) {
		return new ItemOrdemResponse(i.getId(), i.getNome(), i.getCategoria(), i.getEspacos(), i.getQuantidade(),
				i.getDescricao());
	}
}
