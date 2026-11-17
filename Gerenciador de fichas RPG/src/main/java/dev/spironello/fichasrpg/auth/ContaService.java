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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.exception.CredenciaisInvalidasException;
import dev.spironello.fichasrpg.exception.DadosInvalidosException;
import dev.spironello.fichasrpg.exception.EmailJaCadastradoException;
import dev.spironello.fichasrpg.exception.LoginGoogleRecusadoException;
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
	private final VerificadorDoGoogle google;
	private final RefreshTokenRepository refreshTokens;
	private final TransactionTemplate transacao;
	/** Hash de uma senha qualquer, calculado uma vez: o login com e-mail inexistente gasta o mesmo tempo de BCrypt. */
	private final String hashFixo;

	public ContaService(UsuarioRepository usuarios, PoliticaDeSenha politica, TokenService tokens, PasswordEncoder encoder, Clock relogio,
			VerificadorDoGoogle google, RefreshTokenRepository refreshTokens, TransactionTemplate transacao) {
		this.usuarios = usuarios;
		this.politica = politica;
		this.tokens = tokens;
		this.encoder = encoder;
		this.relogio = relogio;
		this.google = google;
		this.refreshTokens = refreshTokens;
		this.transacao = transacao;
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

	/**
	 * Entra (ou cria a conta) com o ID token do Google; as regras estão na spec §3. Este método NÃO é transacional de
	 * propósito: a decisão roda numa transação própria (TransactionTemplate) e, se duas criações simultâneas baterem na
	 * restrição única, a violação já desfez aquela transação (no PostgreSQL ela fica inutilizável), então a nova busca
	 * pelo sub acontece numa transação nova.
	 */
	public RespostaAuth entrarComGoogle(String idToken) {
		IdentidadeGoogle identidade = google.verificar(idToken);
		if (!identidade.emailVerificado()) {
			throw new LoginGoogleRecusadoException(LoginGoogleRecusadoException.EMAIL_NAO_VERIFICADO);
		}
		try {
			return transacao.execute(status -> decidirLoginGoogle(identidade));
		} catch (DataIntegrityViolationException e) {
			return transacao.execute(status -> usuarios.findByGoogleSub(identidade.sub())
					.map(existente -> entrouComGoogle(existente))
					.orElseThrow(() -> new LoginGoogleRecusadoException(LoginGoogleRecusadoException.TOKEN_INVALIDO)));
		}
	}

	private RespostaAuth decidirLoginGoogle(IdentidadeGoogle identidade) {
		// 2) A identidade é o sub: entra mesmo que o e-mail tenha mudado no Google (o e-mail gravado não é atualizado).
		Optional<Usuario> pelaIdentidade = usuarios.findByGoogleSub(identidade.sub());
		if (pelaIdentidade.isPresent()) {
			return entrouComGoogle(pelaIdentidade.get());
		}
		String email = normalizar(identidade.email());
		Optional<Usuario> peloEmail = usuarios.findByEmail(email);
		if (peloEmail.isPresent()) {
			Usuario conta = peloEmail.get();
			// 3) E-mail de uma conta já ligada a outro sub do Google.
			if (conta.getGoogleSub() != null) {
				throw new LoginGoogleRecusadoException(LoginGoogleRecusadoException.EMAIL_DE_OUTRA_CONTA);
			}
			// 3) Liga: grava o sub, apaga a senha e encerra TODAS as sessões dela (pre-account takeover, spec §3).
			conta.ligarAoGoogle(identidade.sub());
			usuarios.saveAndFlush(conta);
			refreshTokens.revogarTodasDoUsuario(conta.getId(), relogio.instant());
			log.info("Conta ligada ao Google id={}", conta.getId());
			return tokens.emitirPar(conta, UUID.randomUUID());
		}
		// 4) Conta nova, sem senha.
		Usuario nova = usuarios.saveAndFlush(
				Usuario.doGoogle(email, nomeDaConta(identidade.nome(), email), identidade.sub(), relogio.instant()));
		log.info("Conta criada pelo Google id={}", nova.getId());
		return tokens.emitirPar(nova, UUID.randomUUID());
	}

	private RespostaAuth entrouComGoogle(Usuario usuario) {
		log.info("Login Google id={}", usuario.getId());
		return tokens.emitirPar(usuario, UUID.randomUUID());
	}

	/** O name do Google sem espaços nas pontas e cortado em 100; sem name, a parte do e-mail antes do @. */
	private static String nomeDaConta(String nome, String email) {
		String base = nome == null ? "" : nome.strip();
		if (base.isEmpty()) {
			base = email.substring(0, email.indexOf('@') > 0 ? email.indexOf('@') : email.length());
		}
		return base.length() > 100 ? base.substring(0, 100) : base;
	}

	@Transactional(readOnly = true)
	public UsuarioResponse eu(Long usuarioId) {
		return usuarios.findById(usuarioId).map(UsuarioResponse::de).orElseThrow(SessaoExpiradaException::new);
	}

	private static String normalizar(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}
}
