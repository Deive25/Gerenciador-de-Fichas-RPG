package dev.spironello.fichasrpg.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.dto.SheetPlayerRequest;
import dev.spironello.fichasrpg.dto.SheetPlayerResponse;
import dev.spironello.fichasrpg.repository.SheetPlayerRepository;

/**
 * Regras de negócio, transações e logs das fichas. O controller só cuida de HTTP e o
 * repository só cuida do banco. Os logs trazem apenas o id: nome e dados da ficha são
 * dados pessoais do jogador e não vão para o log.
 */
@Service
public class SheetPlayerService {

	private static final Logger log = LoggerFactory.getLogger(SheetPlayerService.class);

	private final SheetPlayerRepository repository;
	private final SheetPlayerMapper mapper;

	public SheetPlayerService(SheetPlayerRepository repository, SheetPlayerMapper mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	public List<SheetPlayerResponse> listar() {
		log.debug("Listando fichas");
		return repository.findAll().stream().map(mapper::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public Optional<SheetPlayerResponse> buscar(Long id) {
		log.debug("Buscando ficha id={}", id);
		return repository.findById(id).map(mapper::toResponse);
	}

	@Transactional
	public SheetPlayerResponse criar(SheetPlayerRequest request) {
		SheetPlayerResponse criada = mapper.toResponse(repository.save(mapper.toEntity(request)));
		log.info("Ficha criada id={}", criada.id());
		return criada;
	}

	@Transactional
	public Optional<SheetPlayerResponse> atualizar(Long id, SheetPlayerRequest request) {
		return repository.findById(id).map(ficha -> {
			mapper.copyTo(request, ficha);
			SheetPlayerResponse atualizada = mapper.toResponse(repository.save(ficha));
			log.info("Ficha atualizada id={}", id);
			return atualizada;
		});
	}

	@Transactional
	public void excluir(Long id) {
		repository.deleteById(id);
		log.info("Ficha excluida id={}", id);
	}
}
