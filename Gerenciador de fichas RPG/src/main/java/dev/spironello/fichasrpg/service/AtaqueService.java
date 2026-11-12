package dev.spironello.fichasrpg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.dto.AtaqueRequest;
import dev.spironello.fichasrpg.dto.AtaqueResponse;
import dev.spironello.fichasrpg.dto.ModoTeste;
import dev.spironello.fichasrpg.dto.RolagemAtaqueResponse;
import dev.spironello.fichasrpg.exception.FichaNaoEncontradaException;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.repository.FichaRepository;

/**
 * Criar, editar, excluir e rolar os ataques de uma ficha. Cada sistema tem a sua tabela e as suas regras; o switch
 * sem default faz um sistema novo sem ataques virar erro de compilação (como no ItemService).
 */
@Service
public class AtaqueService {

	private static final Logger log = LoggerFactory.getLogger(AtaqueService.class);

	private final FichaRepository fichas;
	private final AtaquesOrdem ataquesOrdem;
	private final AtaquesDnd ataquesDnd;
	private final Dado dado;

	public AtaqueService(FichaRepository fichas, AtaquesOrdem ataquesOrdem, AtaquesDnd ataquesDnd, Dado dado) {
		this.fichas = fichas;
		this.ataquesOrdem = ataquesOrdem;
		this.ataquesDnd = ataquesDnd;
		this.dado = dado;
	}

	/** Os logs levam só os ids, nunca o texto digitado. */
	@Transactional
	public AtaqueResponse criar(Long fichaId, AtaqueRequest request) {
		Ficha ficha = buscarFicha(fichaId);
		AtaqueResponse criado = switch (ficha.getSistema()) {
			case ORDEM_PARANORMAL -> ataquesOrdem.criar((FichaOrdem) ficha, request);
			case DND -> ataquesDnd.criar((FichaDnd) ficha, request);
		};
		log.info("Ataque criado fichaId={} ataqueId={}", fichaId, criado.id());
		return criado;
	}

	@Transactional
	public AtaqueResponse atualizar(Long fichaId, Long ataqueId, AtaqueRequest request) {
		Ficha ficha = buscarFicha(fichaId);
		AtaqueResponse atualizado = switch (ficha.getSistema()) {
			case ORDEM_PARANORMAL -> ataquesOrdem.atualizar((FichaOrdem) ficha, ataqueId, request);
			case DND -> ataquesDnd.atualizar((FichaDnd) ficha, ataqueId, request);
		};
		log.info("Ataque atualizado fichaId={} ataqueId={}", fichaId, ataqueId);
		return atualizado;
	}

	@Transactional
	public void excluir(Long fichaId, Long ataqueId) {
		Ficha ficha = buscarFicha(fichaId);
		switch (ficha.getSistema()) {
			case ORDEM_PARANORMAL -> ataquesOrdem.excluir((FichaOrdem) ficha, ataqueId);
			case DND -> ataquesDnd.excluir((FichaDnd) ficha, ataqueId);
		}
		log.info("Ataque excluído fichaId={} ataqueId={}", fichaId, ataqueId);
	}

	/** Rola o ataque e o dano (não grava nada). Modo nulo vale NORMAL; em Ordem só NORMAL existe. */
	@Transactional(readOnly = true)
	public RolagemAtaqueResponse rolar(Long fichaId, Long ataqueId, ModoTeste modo) {
		Ficha ficha = buscarFicha(fichaId);
		RolagemAtaqueResponse resposta = switch (ficha.getSistema()) {
			case ORDEM_PARANORMAL -> ataquesOrdem.rolar((FichaOrdem) ficha, ataqueId, modo, dado);
			case DND -> ataquesDnd.rolar((FichaDnd) ficha, ataqueId, modo, dado);
		};
		log.info("Ataque rolado fichaId={} ataqueId={} critico={} total={} dano={}", fichaId, ataqueId,
				resposta.critico(), resposta.ataque().total(), resposta.dano().total());
		return resposta;
	}

	private Ficha buscarFicha(Long fichaId) {
		return fichas.findById(fichaId).orElseThrow(() -> new FichaNaoEncontradaException(fichaId));
	}
}
