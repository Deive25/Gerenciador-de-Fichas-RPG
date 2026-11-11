package dev.spironello.fichasrpg.service;

import java.util.List;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.dto.RitualRequest;
import dev.spironello.fichasrpg.dto.RitualResponse;
import dev.spironello.fichasrpg.exception.ItemDaFichaNaoEncontradoException;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.RitualOrdem;
import dev.spironello.fichasrpg.repository.RitualOrdemRepository;

/** Os rituais das fichas de Ordem Paranormal. Chame dentro da transação. */
@Component
public class RituaisOrdem {

	private final RitualOrdemRepository rituais;

	public RituaisOrdem(RitualOrdemRepository rituais) {
		this.rituais = rituais;
	}

	/** Os rituais da ficha por círculo e, dentro do círculo, pela ordem de criação. */
	public List<RitualResponse> listar(Long fichaId) {
		return rituais.findByFichaIdOrderByCirculoAscIdAsc(fichaId).stream().map(RituaisOrdem::paraResponse)
				.toList();
	}

	public RitualResponse criar(FichaOrdem ficha, RitualRequest request) {
		RitualOrdem ritual = new RitualOrdem();
		ritual.setFicha(ficha);
		preencher(ritual, request);
		return paraResponse(rituais.save(ritual));
	}

	public RitualResponse atualizar(FichaOrdem ficha, Long ritualId, RitualRequest request) {
		RitualOrdem ritual = buscar(ficha, ritualId);
		preencher(ritual, request);
		return paraResponse(ritual);
	}

	public void excluir(FichaOrdem ficha, Long ritualId) {
		rituais.delete(buscar(ficha, ritualId));
	}

	/** findByIdAndFichaId também cobre o ritual de outra ficha: para esta ficha, ele não existe. */
	private RitualOrdem buscar(FichaOrdem ficha, Long ritualId) {
		return rituais.findByIdAndFichaId(ritualId, ficha.getId())
				.orElseThrow(() -> new ItemDaFichaNaoEncontradoException("ritual", ritualId));
	}

	private void preencher(RitualOrdem ritual, RitualRequest request) {
		ritual.setNome(request.nome());
		ritual.setCirculo(request.circulo());
		ritual.setElemento(request.elemento());
		ritual.setExecucao(Textos.vazioParaNulo(request.execucao()));
		ritual.setAlcance(Textos.vazioParaNulo(request.alcance()));
		ritual.setAlvo(Textos.vazioParaNulo(request.alvo()));
		ritual.setDuracao(Textos.vazioParaNulo(request.duracao()));
		ritual.setResistencia(Textos.vazioParaNulo(request.resistencia()));
		ritual.setDescricao(Textos.vazioParaNulo(request.descricao()));
	}

	private static RitualResponse paraResponse(RitualOrdem r) {
		return new RitualResponse(r.getId(), r.getNome(), r.getCirculo(), r.getElemento(), r.getExecucao(),
				r.getAlcance(), r.getAlvo(), r.getDuracao(), r.getResistencia(), r.getDescricao());
	}
}
