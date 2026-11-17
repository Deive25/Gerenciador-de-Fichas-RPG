package dev.spironello.fichasrpg;

import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * O Google de mentira dos testes (spec §7): um par RSA gerado aqui, um JWKS em memória no lugar do bean chavesDoGoogle
 * (sem rede) e métodos que assinam ID tokens com as claims pedidas. Importado pela IntegrationTestBase.
 */
@TestConfiguration
public class ChavesDoGoogleDeTeste {

	public static final String CLIENT_ID = "181222736917-1k8vp73jptcmrbgceam718qoa55p0i39.apps.googleusercontent.com";
	public static final String KID = "teste-1";

	private final Clock relogio;
	private final RSAKey chave;
	private final RSAKey outraChave;

	public ChavesDoGoogleDeTeste(Clock relogio) throws JOSEException {
		this.relogio = relogio;
		this.chave = new RSAKeyGenerator(2048).keyID(KID).generate();
		this.outraChave = new RSAKeyGenerator(2048).keyID(KID).generate();
	}

	/** @Primary: vence o bean chavesDoGoogle da produção, que apontaria para a rede. Só a parte pública vai para o JWKS. */
	@Bean
	@Primary
	JWKSource<SecurityContext> chavesDoGoogleDeTeste() {
		return new ImmutableJWKSet<>(new JWKSet(chave.toPublicJWK()));
	}

	/** Claims de um token válido: iat agora e exp daqui a 1 h, pelo Clock de teste. */
	public Map<String, Object> claimsValidas(String sub, String email) {
		Instant agora = relogio.instant();
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("iss", "https://accounts.google.com");
		claims.put("aud", CLIENT_ID);
		claims.put("sub", sub);
		claims.put("email", email);
		claims.put("email_verified", true);
		claims.put("name", "Ana Souza");
		claims.put("iat", agora.getEpochSecond());
		claims.put("exp", agora.plus(Duration.ofHours(1)).getEpochSecond());
		return claims;
	}

	public String assinar(Map<String, Object> claims) {
		return assinar(claims, chave, KID);
	}

	/** Assinado por outro par RSA, mas com o mesmo kid: o kid bate e a assinatura não. */
	public String assinarComOutraChave(Map<String, Object> claims) {
		return assinar(claims, outraChave, KID);
	}

	/** Assinado pela chave certa, mas com um kid que o JWKS não tem. */
	public String assinarComKid(Map<String, Object> claims, String kid) {
		return assinar(claims, chave, kid);
	}

	private static String assinar(Map<String, Object> claims, RSAKey chave, String kid) {
		try {
			SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).build(),
					JWTClaimsSet.parse(claims));
			jwt.sign(new RSASSASigner(chave));
			return jwt.serialize();
		} catch (JOSEException | ParseException e) {
			throw new IllegalStateException(e);
		}
	}
}
