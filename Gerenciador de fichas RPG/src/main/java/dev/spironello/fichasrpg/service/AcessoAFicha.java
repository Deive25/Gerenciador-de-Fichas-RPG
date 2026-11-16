package dev.spironello.fichasrpg.service;

import org.springframework.stereotype.Component;

import dev.spironello.fichasrpg.auth.UsuarioAtual;
import dev.spironello.fichasrpg.exception.FichaNaoEncontradaException;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.repository.FichaRepository;

/**
 * Único ponto que busca uma ficha por id: sempre por id E dono (o usuário logado). Para quem não é o dono a ficha
 * não existe: a exceção é a mesma de sempre (404), e a API não revela quais ids estão em uso. Os serviços das fichas
 * e dos recursos dela (itens, magias, retrato...) passam por aqui, em vez de chamar o repository direto.
 */
@Component
public class FichasDoUsuario {

	private final FichaRepository fichas;
	private final UsuarioAtual usuario;

	public FichasDoUsuario(FichaRepository fichas, UsuarioAtual usuario) {
		this.fichas = fichas;
		this.usuario = usuario;
	}

	public Ficha buscar(Long fichaId) {
		return fichas.findByIdAndDonoId(fichaId, usuario.id()).orElseThrow(() -> new FichaNaoEncontradaException(fichaId));
	}

	public void exigir(Long fichaId) {
		if (!fichas.existsByIdAndDonoId(fichaId, usuario.id())) {
			throw new FichaNaoEncontradaException(fichaId);
		}
	}
}
