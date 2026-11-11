package dev.spironello.fichasrpg.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.dto.FichaOrdemRequest;
import dev.spironello.fichasrpg.dto.FichaOrdemResponse;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.dto.RecursoResumo;
import dev.spironello.fichasrpg.dto.RecursoValor;
import dev.spironello.fichasrpg.model.AtributoOrdem;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.GrauOrdem;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.model.RecursoOrdem;

/**
 * Converte entre a entidade {@link FichaOrdem} e os DTOs, campo a campo. Manual de propósito: são poucos
 * campos e assim não há dependência nova (MapStruct) nem mágica de reflexão. Atributos e recursos são
 * colunas na entidade e mapas no JSON.
 */
@Component
public class FichaOrdemMapper {

	/** Cria uma entidade nova a partir do pedido. O id nunca é copiado: é gerado pelo banco. */
	public FichaOrdem criar(FichaOrdemRequest request) {
		FichaOrdem ficha = new FichaOrdem();
		copiar(request, ficha);
		return ficha;
	}

	/** Substituição completa: copia todos os campos do pedido, inclusive os nulos. Não toca no id. */
	public void copiar(FichaOrdemRequest request, FichaOrdem ficha) {
		ficha.setJogador(request.jogador());
		ficha.setPersonagem(request.personagem());
		ficha.setGenero(request.genero());
		ficha.setIdade(request.idade());
		ficha.setClasse(request.classe());
		ficha.setOrigem(request.origem());
		ficha.setNex(request.nex());
		request.atributos().forEach(ficha::setAtributo);
		RecursoValor pv = recurso(request, RecursoOrdem.PV);
		ficha.setPvAtual(pv.atual());
		ficha.setPvMax(pv.maximo());
		RecursoValor san = recurso(request, RecursoOrdem.SAN);
		ficha.setSanAtual(san.atual());
		ficha.setSanMax(san.maximo());
		RecursoValor pe = recurso(request, RecursoOrdem.PE);
		ficha.setPeAtual(pe.atual());
		ficha.setPeMax(pe.maximo());
		// Substituição completa das perícias: esvazia o mapa e põe só as que vieram com grau.
		// Grau null no pedido = perícia destreinada: não gera entrada (a linha some do banco).
		Map<PericiaOrdem, GrauOrdem> pericias = ficha.getPericias();
		pericias.clear();
		if (request.pericias() != null) {
			request.pericias().forEach((pericia, grau) -> {
				if (grau != null) {
					pericias.put(pericia, grau);
				}
			});
		}
	}

	/** Lê as perícias da ficha: chame dentro da transação (a coleção é lazy). */
	public FichaOrdemResponse resposta(FichaOrdem ficha, PartesComuns comuns, ListasOrdem listas) {
		// Cópias em EnumMap: ordem estável (a do enum) e sem expor a coleção gerenciada pelo Hibernate.
		Map<AtributoOrdem, Integer> atributos = new EnumMap<>(AtributoOrdem.class);
		for (AtributoOrdem atributo : AtributoOrdem.values()) {
			atributos.put(atributo, ficha.getAtributo(atributo));
		}
		Map<RecursoOrdem, RecursoValor> recursos = new EnumMap<>(RecursoOrdem.class);
		recursos.put(RecursoOrdem.PV, new RecursoValor(ficha.getPvAtual(), ficha.getPvMax()));
		recursos.put(RecursoOrdem.SAN, new RecursoValor(ficha.getSanAtual(), ficha.getSanMax()));
		recursos.put(RecursoOrdem.PE, new RecursoValor(ficha.getPeAtual(), ficha.getPeMax()));
		Map<PericiaOrdem, GrauOrdem> pericias = new EnumMap<>(PericiaOrdem.class);
		pericias.putAll(ficha.getPericias());
		return new FichaOrdemResponse(
				ficha.getId(),
				ficha.getSistema(),
				ficha.getJogador(),
				ficha.getPersonagem(),
				ficha.getGenero(),
				ficha.getIdade(),
				ficha.getRetratoVersao(),
				comuns.descricao(),
				comuns.habilidades(),
				listas.itens(),
				ficha.getClasse(),
				ficha.getOrigem(),
				ficha.getNex(),
				atributos,
				recursos,
				pericias);
	}

	/** Item da listagem. Não lê as perícias: é o que mantém a listagem em uma consulta só. */
	public FichaResumoResponse resumo(FichaOrdem ficha) {
		return new FichaResumoResponse(
				ficha.getId(),
				ficha.getSistema(),
				ficha.getJogador(),
				ficha.getPersonagem(),
				ficha.getIdade(),
				ficha.getRetratoVersao(),
				List.of(
						new RecursoResumo(RecursoOrdem.PV.name(), ficha.getPvAtual(), ficha.getPvMax()),
						new RecursoResumo(RecursoOrdem.SAN.name(), ficha.getSanAtual(), ficha.getSanMax()),
						new RecursoResumo(RecursoOrdem.PE.name(), ficha.getPeAtual(), ficha.getPeMax())));
	}

	/** Recurso do pedido; ausente (ou null) vira "sem valores", porque o PUT substitui a ficha inteira. */
	private static RecursoValor recurso(FichaOrdemRequest request, RecursoOrdem recurso) {
		RecursoValor valor = request.recursos() == null ? null : request.recursos().get(recurso);
		return valor == null ? new RecursoValor(null, null) : valor;
	}
}
