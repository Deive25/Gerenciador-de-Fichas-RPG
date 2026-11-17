package dev.spironello.fichasrpg.auth;

import java.time.Clock;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import dev.spironello.fichasrpg.exception.LoginGoogleRecusadoException;

/**
 * Confere o ID token do Google: assinatura RS256 pelas chaves do JWKS, validade (com o Clock da aplicação), emissor e
 * público (aud = o nosso client ID). O decoder é PRIVADO e de propósito não é um bean JwtDecoder: o resource server
 * pegaria um segundo decoder e poderia usá-lo para os nossos tokens HS256. Token HS256 ou "alg: none" cai no seletor RS256.
 */
@Component
public class VerificadorDoGoogle {

	private static final Set<String> EMISSORES = Set.of("https://accounts.google.com", "accounts.google.com");

	private final JwtDecoder decoder;

	public VerificadorDoGoogle(JWKSource<SecurityContext> chavesDoGoogle, @Value("${app.google.client-id}") String clientId,
			Clock relogio) {
		NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSource(chavesDoGoogle).jwsAlgorithm(SignatureAlgorithm.RS256).build();
		JwtTimestampValidator datas = new JwtTimestampValidator();
		datas.setClock(relogio);
		nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(datas,
				new JwtClaimValidator<Object>("iss", iss -> iss != null && EMISSORES.contains(iss.toString())),
				new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains(clientId))));
		this.decoder = nimbus;
	}

	/** Devolve as claims ou lança o 401 genérico. Nada do token ou do erro interno vai para a resposta ou para o log. */
	public IdentidadeGoogle verificar(String idToken) {
		Jwt jwt;
		try {
			jwt = decoder.decode(idToken);
		} catch (JwtException | IllegalArgumentException e) {
			throw new LoginGoogleRecusadoException(LoginGoogleRecusadoException.TOKEN_INVALIDO);
		}
		String sub = jwt.getSubject();
		String email = jwt.getClaimAsString("email");
		if (sub == null || sub.isBlank() || email == null || email.isBlank()) {
			throw new LoginGoogleRecusadoException(LoginGoogleRecusadoException.TOKEN_INVALIDO);
		}
		// Só o booleano true vale: a string "true" ou a ausência da claim contam como não verificado.
		boolean verificado = Boolean.TRUE.equals(jwt.getClaims().get("email_verified"));
		Object nome = jwt.getClaims().get("name");
		return new IdentidadeGoogle(sub, email, verificado, nome instanceof String s ? s : null);
	}
}
