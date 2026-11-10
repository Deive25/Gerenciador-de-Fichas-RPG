package dev.spironello.fichasrpg.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.dto.MoedasDnd;
import dev.spironello.fichasrpg.dto.MoedasRequest;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.exception.FichaNaoEncontradaException;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.repository.FichaRepository;

/** Edição das moedas de uma ficha de D&D: grava só as moedas enviadas. */
@Service
public class MoedasService {

	private static final Logger log = LoggerFactory.getLogger(MoedasService.class);

	private final FichaRepository fichas;

	public MoedasService(FichaRepository fichas) {
		this.fichas = fichas;
	}

	/** Devolve as cinco moedas depois da mudança. Loga só o id da ficha. */
	@Transactional
	public MoedasDnd atualizar(Long fichaId, MoedasRequest request) {
		Ficha ficha = fichas.findById(fichaId).orElseThrow(() -> new FichaNaoEncontradaException(fichaId));
		if (!(ficha instanceof FichaDnd dnd)) {
			throw new DadosInvalidosException(Map.of("lista", "não existe em Ordem Paranormal"));
		}
		if (request.pc() != null) {
			dnd.setPc(request.pc());
		}
		if (request.pp() != null) {
			dnd.setPp(request.pp());
		}
		if (request.pe() != null) {
			dnd.setPe(request.pe());
		}
		if (request.po() != null) {
			dnd.setPo(request.po());
		}
		if (request.pl() != null) {
			dnd.setPl(request.pl());
		}
		log.info("Moedas atualizadas fichaId={}", fichaId);
		return new MoedasDnd(dnd.getPc(), dnd.getPp(), dnd.getPe(), dnd.getPo(), dnd.getPl());
	}
}
