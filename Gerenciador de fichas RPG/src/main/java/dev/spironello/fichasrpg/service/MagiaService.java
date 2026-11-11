package dev.spironello.fichasrpg.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.dto.MagiaRequest;
import dev.spironello.fichasrpg.dto.MagiaResponse;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.exception.FichaNaoEncontradaException;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.repository.FichaRepository;

/**
 * Criar, editar e excluir as magias de uma ficha. Só D&D tem magias: no outro sistema o caso do switch lança 400
 * em errors.lista, e o switch sem default faz um sistema novo virar erro de compilação (como no ItemService).
 */
@Service
public class MagiaService {

	private static final Logger log = LoggerFactory.getLogger(MagiaService.class);

	private final FichaRepository fichas;
	private final MagiasDnd magiasDnd;

	public MagiaService(FichaRepository fichas, MagiasDnd magiasDnd) {
		this.fichas = fichas;
		this.magiasDnd = magiasDnd;
	}

	/** Os logs levam só os ids, nunca o texto digitado. */
	@Transactional
	public MagiaResponse criar(Long fichaId, MagiaRequest request) {
		Ficha ficha = buscarFicha(fichaId);
		MagiaResponse criada = switch (ficha.getSistema()) {
			case ORDEM_PARANORMAL -> throw naoExisteEmOrdem();
			case DND -> magiasDnd.criar((FichaDnd) ficha, request);
		};
		log.info("Magia criada fichaId={} magiaId={}", fichaId, criada.id());
		return criada;
	}

	@Transactional
	public MagiaResponse atualizar(Long fichaId, Long magiaId, MagiaRequest request) {
		Ficha ficha = buscarFicha(fichaId);
		MagiaResponse atualizada = switch (ficha.getSistema()) {
			case ORDEM_PARANORMAL -> throw naoExisteEmOrdem();
			case DND -> magiasDnd.atualizar((FichaDnd) ficha, magiaId, request);
		};
		log.info("Magia atualizada fichaId={} magiaId={}", fichaId, magiaId);
		return atualizada;
	}

	@Transactional
	public void excluir(Long fichaId, Long magiaId) {
		Ficha ficha = buscarFicha(fichaId);
		switch (ficha.getSistema()) {
			case ORDEM_PARANORMAL -> throw naoExisteEmOrdem();
			case DND -> magiasDnd.excluir((FichaDnd) ficha, magiaId);
		}
		log.info("Magia excluída fichaId={} magiaId={}", fichaId, magiaId);
	}

	private Ficha buscarFicha(Long fichaId) {
		return fichas.findById(fichaId).orElseThrow(() -> new FichaNaoEncontradaException(fichaId));
	}

	private static DadosInvalidosException naoExisteEmOrdem() {
		return new DadosInvalidosException(Map.of("lista", "não existe em Ordem Paranormal"));
	}
}
