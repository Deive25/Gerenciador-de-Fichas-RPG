package dev.spironello.fichasrpg.service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.dto.FichaDndRequest;
import dev.spironello.fichasrpg.dto.FichaDndResponse;
import dev.spironello.fichasrpg.dto.MoedasDnd;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.dto.RecursoResumo;
import dev.spironello.fichasrpg.dto.RecursoValor;
import dev.spironello.fichasrpg.model.AtributoDnd;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.GrauDnd;
import dev.spironello.fichasrpg.model.PericiaDnd;
import dev.spironello.fichasrpg.model.RecursoDnd;

/** Converte entre {@link FichaDnd} e os DTOs, campo a campo (manual de propósito, como o mapper de Ordem). */
@Component
public class FichaDndMapper {

	/** Cria uma entidade nova a partir do pedido. O id nunca é copiado: é gerado pelo banco. */
	public FichaDnd criar(FichaDndRequest request) {
		FichaDnd ficha = new FichaDnd();
		copiar(request, ficha);
		return ficha;
	}

	/** Substituição completa: copia todos os campos do pedido, inclusive os nulos. Não toca no id. */
	public void copiar(FichaDndRequest request, FichaDnd ficha) {
		ficha.setJogador(request.jogador());
		ficha.setPersonagem(request.personagem());
		ficha.setGenero(request.genero());
		ficha.setIdade(request.idade());
		ficha.setEdicao(request.edicao());
		ficha.setClasse(request.classe());
		ficha.setEspecie(request.especie());
		ficha.setAntecedente(request.antecedente());
		ficha.setNivel(request.nivel());
		request.atributos().forEach(ficha::setAtributo);
		RecursoValor pv = recurso(request, RecursoDnd.PV);
		ficha.setPvAtual(pv.atual());
		ficha.setPvMax(pv.maximo());
		// O PV temporário não tem máximo (a validação já recusou um): só o atual é guardado.
		ficha.setPvTemporario(recurso(request, RecursoDnd.PV_TEMP).atual());
		ficha.setCa(request.ca());
		ficha.setInspiracao(Boolean.TRUE.equals(request.inspiracao()));
		ficha.setSalvaguardas(request.salvaguardas());
		// Substituição completa das perícias; grau null = sem proficiência (não gera entrada).
		Map<PericiaDnd, GrauDnd> pericias = ficha.getPericias();
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
	public FichaDndResponse resposta(FichaDnd ficha, PartesComuns comuns, ListasDnd listas) {
		Map<AtributoDnd, Integer> atributos = new EnumMap<>(AtributoDnd.class);
		Set<AtributoDnd> salvaguardas = EnumSet.noneOf(AtributoDnd.class);
		for (AtributoDnd atributo : AtributoDnd.values()) {
			atributos.put(atributo, ficha.getAtributo(atributo));
			if (ficha.isProficienteEmSalvaguarda(atributo)) {
				salvaguardas.add(atributo);
			}
		}
		Map<RecursoDnd, RecursoValor> recursos = new EnumMap<>(RecursoDnd.class);
		recursos.put(RecursoDnd.PV, new RecursoValor(ficha.getPvAtual(), ficha.getPvMax()));
		recursos.put(RecursoDnd.PV_TEMP, new RecursoValor(ficha.getPvTemporario(), null));
		Map<PericiaDnd, GrauDnd> pericias = new EnumMap<>(PericiaDnd.class);
		pericias.putAll(ficha.getPericias());
		return new FichaDndResponse(
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
				new MoedasDnd(ficha.getPc(), ficha.getPp(), ficha.getPe(), ficha.getPo(), ficha.getPl()),
				ficha.getEdicao(),
				ficha.getClasse(),
				ficha.getEspecie(),
				ficha.getAntecedente(),
				ficha.getNivel(),
				atributos,
				recursos,
				ficha.getCa(),
				ficha.isInspiracao(),
				salvaguardas,
				pericias);
	}

	/** Item da listagem: só o PV nos recursos e sem ler as perícias (a listagem continua em uma consulta). */
	public FichaResumoResponse resumo(FichaDnd ficha) {
		return new FichaResumoResponse(
				ficha.getId(),
				ficha.getSistema(),
				ficha.getJogador(),
				ficha.getPersonagem(),
				ficha.getIdade(),
				ficha.getRetratoVersao(),
				List.of(new RecursoResumo(RecursoDnd.PV.name(), ficha.getPvAtual(), ficha.getPvMax())));
	}

	/** Recurso do pedido; ausente (ou null) vira "sem valores", porque o PUT substitui a ficha inteira. */
	private static RecursoValor recurso(FichaDndRequest request, RecursoDnd recurso) {
		RecursoValor valor = request.recursos() == null ? null : request.recursos().get(recurso);
		return valor == null ? new RecursoValor(null, null) : valor;
	}
}
