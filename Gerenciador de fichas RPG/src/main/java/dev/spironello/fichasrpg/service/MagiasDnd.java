package dev.spironello.fichasrpg.service;

import java.util.List;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.dto.MagiaRequest;
import dev.spironello.fichasrpg.dto.MagiaResponse;
import dev.spironello.fichasrpg.exception.ItemDaFichaNaoEncontradoException;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.MagiaDnd;
import dev.spironello.fichasrpg.repository.MagiaDndRepository;

/** As magias das fichas de D&D. Chame dentro da transação. */
@Component
public class MagiasDnd {

	private final MagiaDndRepository magias;

	public MagiasDnd(MagiaDndRepository magias) {
		this.magias = magias;
	}

	/** As magias da ficha por nível (truques primeiro) e, dentro do nível, pela ordem de criação. */
	public List<MagiaResponse> listar(Long fichaId) {
		return magias.findByFichaIdOrderByNivelAscIdAsc(fichaId).stream().map(MagiasDnd::paraResponse).toList();
	}

	public MagiaResponse criar(FichaDnd ficha, MagiaRequest request) {
		MagiaDnd magia = new MagiaDnd();
		magia.setFicha(ficha);
		preencher(magia, request);
		return paraResponse(magias.save(magia));
	}

	public MagiaResponse atualizar(FichaDnd ficha, Long magiaId, MagiaRequest request) {
		MagiaDnd magia = buscar(ficha, magiaId);
		preencher(magia, request);
		return paraResponse(magia);
	}

	public void excluir(FichaDnd ficha, Long magiaId) {
		magias.delete(buscar(ficha, magiaId));
	}

	/** findByIdAndFichaId também cobre a magia de outra ficha: para esta ficha, ela não existe. */
	private MagiaDnd buscar(FichaDnd ficha, Long magiaId) {
		return magias.findByIdAndFichaId(magiaId, ficha.getId())
				.orElseThrow(() -> new ItemDaFichaNaoEncontradoException("magia", magiaId));
	}

	private void preencher(MagiaDnd magia, MagiaRequest request) {
		magia.setNome(request.nome());
		magia.setNivel(request.nivel());
		magia.setEscola(Textos.vazioParaNulo(request.escola()));
		magia.setTempo(Textos.vazioParaNulo(request.tempo()));
		magia.setAlcance(Textos.vazioParaNulo(request.alcance()));
		magia.setComponentes(Textos.vazioParaNulo(request.componentes()));
		magia.setDuracao(Textos.vazioParaNulo(request.duracao()));
		magia.setConcentracao(Boolean.TRUE.equals(request.concentracao()));
		magia.setRitual(Boolean.TRUE.equals(request.ritual()));
		magia.setPreparada(Boolean.TRUE.equals(request.preparada()));
		magia.setDescricao(Textos.vazioParaNulo(request.descricao()));
	}

	private static MagiaResponse paraResponse(MagiaDnd m) {
		return new MagiaResponse(m.getId(), m.getNome(), m.getNivel(), m.getEscola(), m.getTempo(), m.getAlcance(),
				m.getComponentes(), m.getDuracao(), m.isConcentracao(), m.isRitual(), m.isPreparada(),
				m.getDescricao());
	}
}
