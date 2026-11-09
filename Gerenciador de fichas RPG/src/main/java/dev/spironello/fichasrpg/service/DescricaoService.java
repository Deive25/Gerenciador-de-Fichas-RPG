package dev.spironello.fichasrpg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.dto.DescricaoFicha;
import dev.spironello.fichasrpg.exception.FichaNaoEncontradaException;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.repository.FichaRepository;

/** Edição da descrição da ficha (aparência, história e anotações), igual nos dois sistemas. */
@Service
public class DescricaoService {

	private static final Logger log = LoggerFactory.getLogger(DescricaoService.class);

	private final FichaRepository fichas;

	public DescricaoService(FichaRepository fichas) {
		this.fichas = fichas;
	}

	/** Substituição completa dos três campos; texto vazio ou só com espaços vira null. Loga só o id. */
	@Transactional
	public DescricaoFicha atualizar(Long fichaId, DescricaoFicha descricao) {
		Ficha ficha = fichas.findById(fichaId).orElseThrow(() -> new FichaNaoEncontradaException(fichaId));
		ficha.setAparencia(vazioViraNulo(descricao.aparencia()));
		ficha.setHistoria(vazioViraNulo(descricao.historia()));
		ficha.setAnotacoes(vazioViraNulo(descricao.anotacoes()));
		log.info("Descrição atualizada fichaId={}", fichaId);
		return new DescricaoFicha(ficha.getAparencia(), ficha.getHistoria(), ficha.getAnotacoes());
	}

	private static String vazioViraNulo(String texto) {
		return texto == null || texto.isBlank() ? null : texto;
	}
}
