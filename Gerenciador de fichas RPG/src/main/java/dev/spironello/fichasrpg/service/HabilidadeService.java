package dev.spironello.fichasrpg.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.dto.HabilidadeRequest;
import dev.spironello.fichasrpg.dto.HabilidadeResponse;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.exception.FichaNaoEncontradaException;
import dev.spironello.fichasrpg.exception.ItemDaFichaNaoEncontradoException;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.HabilidadeFicha;
import dev.spironello.fichasrpg.model.Sistema;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.repository.HabilidadeFichaRepository;

/** Criar, editar e excluir as habilidades de uma ficha; o tipo precisa existir no sistema da ficha. */
@Service
public class HabilidadeService {

	private static final Logger log = LoggerFactory.getLogger(HabilidadeService.class);

	private final FichaRepository fichas;
	private final HabilidadeFichaRepository habilidades;

	public HabilidadeService(FichaRepository fichas, HabilidadeFichaRepository habilidades) {
		this.fichas = fichas;
		this.habilidades = habilidades;
	}

	/** Os logs levam só os ids, nunca o texto digitado. */
	@Transactional
	public HabilidadeResponse criar(Long fichaId, HabilidadeRequest request) {
		Ficha ficha = buscarFicha(fichaId);
		conferirTipo(ficha, request);
		HabilidadeFicha habilidade = new HabilidadeFicha();
		habilidade.setFicha(ficha);
		preencher(habilidade, request);
		HabilidadeFicha salva = habilidades.save(habilidade);
		log.info("Habilidade criada fichaId={} habilidadeId={}", fichaId, salva.getId());
		return paraResponse(salva);
	}

	@Transactional
	public HabilidadeResponse atualizar(Long fichaId, Long habilidadeId, HabilidadeRequest request) {
		Ficha ficha = buscarFicha(fichaId);
		HabilidadeFicha habilidade = buscarHabilidade(fichaId, habilidadeId);
		conferirTipo(ficha, request);
		preencher(habilidade, request);
		log.info("Habilidade atualizada fichaId={} habilidadeId={}", fichaId, habilidadeId);
		return paraResponse(habilidade);
	}

	@Transactional
	public void excluir(Long fichaId, Long habilidadeId) {
		buscarFicha(fichaId);
		habilidades.delete(buscarHabilidade(fichaId, habilidadeId));
		log.info("Habilidade excluída fichaId={} habilidadeId={}", fichaId, habilidadeId);
	}

	private Ficha buscarFicha(Long fichaId) {
		return fichas.findById(fichaId).orElseThrow(() -> new FichaNaoEncontradaException(fichaId));
	}

	/** findByIdAndFichaId também cobre a habilidade de outra ficha: para esta ficha, ela não existe. */
	private HabilidadeFicha buscarHabilidade(Long fichaId, Long habilidadeId) {
		return habilidades.findByIdAndFichaId(habilidadeId, fichaId)
				.orElseThrow(() -> new ItemDaFichaNaoEncontradoException("habilidade", habilidadeId));
	}

	private void conferirTipo(Ficha ficha, HabilidadeRequest request) {
		Sistema sistema = ficha.getSistema();
		if (!request.tipo().permitidoEm(sistema)) {
			String nomeDoSistema = sistema == Sistema.DND ? "D&D" : "Ordem Paranormal";
			throw new DadosInvalidosException(Map.of("tipo", "não existe em " + nomeDoSistema));
		}
	}

	private void preencher(HabilidadeFicha habilidade, HabilidadeRequest request) {
		habilidade.setNome(request.nome());
		habilidade.setTipo(request.tipo());
		String descricao = request.descricao();
		habilidade.setDescricao(descricao == null || descricao.isBlank() ? null : descricao);
	}

	private HabilidadeResponse paraResponse(HabilidadeFicha h) {
		return new HabilidadeResponse(h.getId(), h.getNome(), h.getTipo(), h.getDescricao());
	}
}
