package dev.spironello.fichasrpg.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emite o JWT de acesso (15 min, HS256) e cuida do refresh (30 dias, opaco, rotacionado por família). Todas as datas
 * vêm do Clock da aplicação, para o teste vencer um token sem esperar. Os logs trazem só ids, nunca o token.
 */
@Service
public class TokenService {

	static final String EMISSOR = "fichas-rpg";
	static final Duration VALIDADE_ACESSO = Duration.ofMinutes(15);
	static final Duration VALIDADE_REFRESH = Duration.ofDays(30);

	private static final Logger log = LoggerFactory.getLogger(TokenService.class);

	private final JwtEncoder encoder;
	private final RefreshTokenRepository refreshTokens;
	private final UsuarioRepository usuarios;
	private final Clock relogio;
	private final SecureRandom aleatorio = new SecureRandom();

	public TokenService(JwtEncoder encoder, RefreshTokenRepository refreshTokens, UsuarioRepository usuarios,
			Clock relogio) {
		this.encoder = encoder;
		this.refreshTokens = refreshTokens;
		this.usuarios = usuarios;
		this.relogio = relogio;
	}

	public String emitirAcesso(Usuario usuario) {
		Instant agora = relogio.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(EMISSOR)
				.subject(String.valueOf(usuario.getId()))
				.claim("email", usuario.getEmail())
				.issuedAt(agora)
				.expiresAt(agora.plus(VALIDADE_ACESSO))
				.build();
		JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
	}

	/** Login e cadastro criam uma família nova (o chamador passa UUID.randomUUID()); a renovação reaproveita a família. */
	@Transactional
	public RespostaAuth emitirPar(Usuario usuario, UUID familia) {
		return novoPar(usuario, familia).resposta();
	}

	/**
	 * Troca o refresh por um par novo. Roda numa transação com SELECT ... FOR UPDATE no token. Se o token já foi usado
	 * (revogado), é reuso: a família inteira é revogada e o resultado é Recusado, que o controller transforma em 401
	 * FORA desta transação (lançar aqui desfaria a revogação). Vencido ou desconhecido: Recusado, sem revogar nada.
	 */
	@Transactional
	public ResultadoDoRefresh renovar(String refresh) {
		Instant agora = relogio.instant();
		Optional<RefreshToken> encontrado = refreshTokens.findByHash(sha256(refresh));
		if (encontrado.isEmpty()) {
			return new ResultadoDoRefresh.Recusado();
		}
		RefreshToken antigo = encontrado.get();
		if (antigo.revogado()) {
			int revogados = refreshTokens.revogarFamilia(antigo.getFamilia(), agora);
			log.warn("Reuso de refresh detectado usuarioId={} familia={} revogados={}", antigo.getUsuarioId(),
					antigo.getFamilia(), revogados);
			return new ResultadoDoRefresh.Recusado();
		}
		if (!antigo.getExpiraEm().isAfter(agora)) {
			return new ResultadoDoRefresh.Recusado();
		}
		Optional<Usuario> usuario = usuarios.findById(antigo.getUsuarioId());
		if (usuario.isEmpty()) {
			return new ResultadoDoRefresh.Recusado();
		}
		NovoPar novo = novoPar(usuario.get(), antigo.getFamilia());
		antigo.revogar(agora);
		antigo.substituidoPor(novo.refreshId());
		log.info("Refresh renovado usuarioId={}", antigo.getUsuarioId());
		return new ResultadoDoRefresh.Renovado(novo.resposta());
	}

	/** Revoga só o token apresentado. Desconhecido ou já revogado: não faz nada (o logout responde 204 sempre). */
	@Transactional
	public void revogar(String refresh) {
		refreshTokens.findByHash(sha256(refresh)).ifPresent(token -> {
			if (!token.revogado()) {
				token.revogar(relogio.instant());
				log.info("Refresh revogado no logout usuarioId={}", token.getUsuarioId());
			}
		});
	}

	private NovoPar novoPar(Usuario usuario, UUID familia) {
		Instant agora = relogio.instant();
		byte[] bytes = new byte[32];
		aleatorio.nextBytes(bytes);
		String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		RefreshToken salvo = refreshTokens.save(
				new RefreshToken(usuario.getId(), familia, sha256(refresh), agora, agora.plus(VALIDADE_REFRESH)));
		RespostaAuth resposta = new RespostaAuth(emitirAcesso(usuario), refresh, UsuarioResponse.de(usuario));
		return new NovoPar(resposta, salvo.getId());
	}

	/** SHA-256 em hexadecimal (64 caracteres). Basta porque o refresh tem 256 bits aleatórios (não há o que adivinhar). */
	static String sha256(String texto) {
		try {
			byte[] resumo = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(resumo);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private record NovoPar(RespostaAuth resposta, Long refreshId) {
	}
}
