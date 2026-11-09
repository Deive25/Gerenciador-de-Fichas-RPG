package dev.spironello.fichasrpg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.dto.ItemRequest;
import dev.spironello.fichasrpg.dto.ItemResponse;
import dev.spironello.fichasrpg.exception.FichaNaoEncontradaException;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.repository.FichaRepository;

/**
 * Criar, editar e excluir os itens do inventário de uma ficha. Cada sistema tem a sua tabela e as suas regras;
 * o switch sem default faz um sistema novo sem inventário virar erro de compilação.
 */
@Service
public class ItemService {

	private static final Logger log = LoggerFactory.getLogger(ItemService.class);

	private final FichaRepository fichas;
	private final ItensOrdem itensOrdem;
	private final ItensDnd itensDnd;

	public ItemService(FichaRepository fichas, ItensOrdem itensOrdem, ItensDnd itensDnd) {
		this.fichas = fichas;
		this.itensOrdem = itensOrdem;
		this.itensDnd = itensDnd;
	}

	/** Os logs levam só os ids, nunca o texto digitado. */
	@Transactional
	public ItemResponse criar(Long fichaId, ItemRequest request) {
		Ficha ficha = buscarFicha(fichaId);
		ItemResponse criado = switch (ficha.getSistema()) {
			case ORDEM_PARANORMAL -> itensOrdem.criar((FichaOrdem) ficha, request);
			case DND -> itensDnd.criar((FichaDnd) ficha, request);
		};
		log.info("Item criado fichaId={} itemId={}", fichaId, criado.id());
		return criado;
	}

	@Transactional
	public ItemResponse atualizar(Long fichaId, Long itemId, ItemRequest request) {
		Ficha ficha = buscarFicha(fichaId);
		ItemResponse atualizado = switch (ficha.getSistema()) {
			case ORDEM_PARANORMAL -> itensOrdem.atualizar((FichaOrdem) ficha, itemId, request);
			case DND -> itensDnd.atualizar((FichaDnd) ficha, itemId, request);
		};
		log.info("Item atualizado fichaId={} itemId={}", fichaId, itemId);
		return atualizado;
	}

	@Transactional
	public void excluir(Long fichaId, Long itemId) {
		Ficha ficha = buscarFicha(fichaId);
		switch (ficha.getSistema()) {
			case ORDEM_PARANORMAL -> itensOrdem.excluir((FichaOrdem) ficha, itemId);
			case DND -> itensDnd.excluir((FichaDnd) ficha, itemId);
		}
		log.info("Item excluído fichaId={} itemId={}", fichaId, itemId);
	}

	private Ficha buscarFicha(Long fichaId) {
		return fichas.findById(fichaId).orElseThrow(() -> new FichaNaoEncontradaException(fichaId));
	}
}
