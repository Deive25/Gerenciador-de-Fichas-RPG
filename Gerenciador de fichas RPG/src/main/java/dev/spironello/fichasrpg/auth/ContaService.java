package dev.spironello.fichasrpg.auth;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.exception.CredenciaisInvalidasException;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.exception.EmailJaCadastradoException;
import dev.spironello.fichasrpg.exception.SessaoExpiradaException;

/** Cadastro, login e consulta da conta. Nenhum log traz e-mail, nome ou senha: só o id do usuário. */
@Service
public class ContaService {

	private static final Logger log = LoggerFactory.getLogger(ContaService.class);

	/** O BCrypt só lê os primeiros 72 bytes da senha; mais que isso seria ignorado em silêncio. */
	private static final int MAX_BYTES_SENHA = 72;

	private final UsuarioRepository usuarios;
	private final PoliticaDeSenha politica;
	private final TokenService tokens;
	private final PasswordEncoder encoder;
	private final Clock relogio;
	/** Hash de uma senha qualquer, calculado uma vez: o login com e-mail inexistente gasta o mesmo tempo de BCrypt. */
	private final String hashFixo;

	public ContaService(UsuarioRepository usuarios, PoliticaDeSenha politica, TokenService tokens, PasswordEncoder encoder, Clock relogio) {
		this.usuarios = usuarios;
		this.politica = politica;
		this.tokens = tokens;
		this.encoder = encoder;
		this.relogio = relogio;
		this.hashFixo = encoder.encode("senha-que-nunca-sera-usada");
	}

	@Transactional
	public RespostaAuth registrar(RegistroRequest pedido) {
		String email = normalizar(pedido.email());
		Optional<String> problema = politica.problema(pedido.senha(), pedido.nome(), email);
		if (problema.isPresent()) {
			throw new DadosInvalidosException(Map.of("senha", problema.get()));
		}
		if (usuarios.existsByEmail(email)) {
			throw new EmailJaCadastradoException();
		}
		Usuario novo = new Usuario(email, pedido.nome(), encoder.encode(pedido.senha()), relogio.instant());
		try {
			novo = usuarios.saveAndFlush(novo);
		} catch (DataIntegrityViolationException e) {
			// Duas inscrições simultâneas com o mesmo e-mail: a restrição única do banco decide.
			throw new EmailJaCadastradoException();
		}
		log.info("Usuário cadastrado id={}", novo.getId());
		return tokens.emitirPar(novo, UUID.randomUUID());
	}

	/** O mesmo 401 para e-mail inexistente, senha errada e conta sem senha (o legado). */
	@Transactional
	public RespostaAuth entrar(LoginRequest pedido) {
		Usuario usuario = usuarios.findByEmail(normalizar(pedido.email())).orElse(null);
		boolean senhaCurta = pedido.senha().getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES_SENHA;
		if (usuario == null || usuario.getSenhaHash() == null || !senhaCurta) {
			encoder.matches(senhaCurta ? pedido.senha() : "x", hashFixo);
			throw new CredenciaisInvalidasException();
		}
		if (!encoder.matches(pedido.senha(), usuario.getSenhaHash())) {
			throw new CredenciaisInvalidasException();
		}
		log.info("Login id={}", usuario.getId());
		return tokens.emitirPar(usuario, UUID.randomUUID());
	}

	@Transactional(readOnly = true)
	public UsuarioResponse eu(Long usuarioId) {
		return usuarios.findById(usuarioId).map(UsuarioResponse::de).orElseThrow(SessaoExpiradaException::new);
	}

	private static String normalizar(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}
}
