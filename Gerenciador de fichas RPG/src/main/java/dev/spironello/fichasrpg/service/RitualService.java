package dev.spironello.fichasrpg.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.dto.RitualRequest;
import dev.spironello.fichasrpg.dto.RitualResponse;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.exception.FichaNaoEncontradaException;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.repository.FichaRepository;

/**
 * Criar, editar e excluir os rituais de uma ficha. Só Ordem Paranormal tem rituais: em D&D o caso do switch lança
 * 400 em errors.lista, e o switch sem default faz um sistema novo virar erro de compilação (como no ItemService).
 */
@Service
public class RitualService {

	private static final Logger log = LoggerFactory.getLogger(RitualService.class);

	private final FichaRepository fichas;
	private final RituaisOrdem rituaisOrdem;

	public RitualService(FichaRepository fichas, RituaisOrdem rituaisOrdem) {
		this.fichas = fichas;
		this.rituaisOrdem = rituaisOrdem;
	}

	/** Os logs levam só os ids, nunca o texto digitado. */
	@Transactional
	public RitualResponse criar(Long fichaId, RitualRequest request) {
		Ficha ficha = buscarFicha(fichaId);
		RitualResponse criado = switch (ficha.getSistema()) {
			case ORDEM_PARANORMAL -> rituaisOrdem.criar((FichaOrdem) ficha, request);
			case DND -> throw naoExisteEmDnd();
		};
		log.info("Ritual criado fichaId={} ritualId={}", fichaId, criado.id());
		return criado;
	}

	@Transactional
	public RitualResponse atualizar(Long fichaId, Long ritualId, RitualRequest request) {
		Ficha ficha = buscarFicha(fichaId);
		RitualResponse atualizado = switch (ficha.getSistema()) {
			case ORDEM_PARANORMAL -> rituaisOrdem.atualizar((FichaOrdem) ficha, ritualId, request);
			case DND -> throw naoExisteEmDnd();
		};
		log.info("Ritual atualizado fichaId={} ritualId={}", fichaId, ritualId);
		return atualizado;
	}

	@Transactional
	public void excluir(Long fichaId, Long ritualId) {
		Ficha ficha = buscarFicha(fichaId);
		switch (ficha.getSistema()) {
			case ORDEM_PARANORMAL -> rituaisOrdem.excluir((FichaOrdem) ficha, ritualId);
			case DND -> throw naoExisteEmDnd();
		}
		log.info("Ritual excluído fichaId={} ritualId={}", fichaId, ritualId);
	}

	private Ficha buscarFicha(Long fichaId) {
		return fichas.findById(fichaId).orElseThrow(() -> new FichaNaoEncontradaException(fichaId));
	}

	private static DadosInvalidosException naoExisteEmDnd() {
		return new DadosInvalidosException(Map.of("lista", "não existe em D&D"));
	}
}
