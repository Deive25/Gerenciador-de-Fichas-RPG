package dev.spironello.fichasrpg.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.dto.EspacoDnd;
import dev.spironello.fichasrpg.dto.EspacoRequest;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.exception.FichaNaoEncontradaException;
import dev.spironello.fichasrpg.model.EspacoMagia;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.repository.FichaRepository;

/** Edição dos espaços de magia de uma ficha de D&D: grava só os níveis enviados. */
@Service
public class EspacosService {

	private static final Logger log = LoggerFactory.getLogger(EspacosService.class);

	private final FichaRepository fichas;

	public EspacosService(FichaRepository fichas) {
		this.fichas = fichas;
	}

	/**
	 * Devolve os nove níveis depois da mudança. Confere tudo antes de gravar e junta os erros: se um nível
	 * estiver errado, nenhum muda. Loga só o id da ficha.
	 */
	@Transactional
	public Map<Integer, EspacoDnd> atualizar(Long fichaId, Map<String, EspacoRequest> pedido) {
		Ficha ficha = fichas.findById(fichaId).orElseThrow(() -> new FichaNaoEncontradaException(fichaId));
		if (!(ficha instanceof FichaDnd dnd)) {
			throw new DadosInvalidosException(Map.of("lista", "não existe em Ordem Paranormal"));
		}
		Map<String, String> erros = new LinkedHashMap<>();
		Map<Integer, EspacoMagia> validos = new LinkedHashMap<>();
		pedido.forEach((chave, espaco) -> conferir(chave, espaco, erros, validos));
		if (!erros.isEmpty()) {
			throw new DadosInvalidosException(erros);
		}
		dnd.getEspacos().putAll(validos);
		log.info("Espaços de magia atualizados fichaId={}", fichaId);
		return EspacosDaFicha.de(dnd);
	}

	/** Confere um nível do pedido. Chave fora de 1 a 9 não tem os campos conferidos: não há nível a que ligá-los. */
	private static void conferir(String chave, EspacoRequest espaco, Map<String, String> erros,
			Map<Integer, EspacoMagia> validos) {
		Integer nivel = nivel(chave);
		if (nivel == null) {
			erros.put(chave, "nível deve estar entre 1 e 9");
			return;
		}
		Integer total = espaco == null ? null : espaco.total();
		Integer usados = espaco == null ? null : espaco.usados();
		boolean totalValido = conferirNumero(chave + ".total", total, erros);
		boolean usadosValido = conferirNumero(chave + ".usados", usados, erros);
		if (totalValido && usadosValido && usados > total) {
			erros.put(chave + ".usados", "não pode passar do total");
		} else if (totalValido && usadosValido) {
			validos.put(nivel, new EspacoMagia(total, usados));
		}
	}

	/** Número obrigatório e de 0 em diante; devolve se passou. */
	private static boolean conferirNumero(String campo, Integer valor, Map<String, String> erros) {
		if (valor == null) {
			erros.put(campo, "é obrigatório");
			return false;
		}
		if (valor < 0) {
			erros.put(campo, "deve ser 0 ou mais");
			return false;
		}
		return true;
	}

	/** "1" a "9", escritos assim mesmo: "01", "+1" e "10" não são níveis. Fora disso devolve null. */
	private static Integer nivel(String chave) {
		if (chave.length() == 1 && chave.charAt(0) >= '1' && chave.charAt(0) <= '9') {
			return chave.charAt(0) - '0';
		}
		return null;
	}
}
