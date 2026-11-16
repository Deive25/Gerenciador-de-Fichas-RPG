package dev.spironello.fichasrpg.auth;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Chave e (de)codificador do JWT de acesso. HS256 basta: só esta API emite e valida o token (RS256 só se justificaria
 * se outro serviço também validasse). A chave vem de JWT_SEGREDO e a API não sobe sem ela.
 */
@Configuration
public class JwtConfig {

	static final String MENSAGEM_SEGREDO = "Defina JWT_SEGREDO (≥ 32 bytes) no .env; gere com: openssl rand -base64 48";

	@Bean
	SecretKey chaveJwt(@Value("${app.jwt.segredo:}") String segredo) {
		return chave(segredo);
	}

	/** Exige pelo menos 32 bytes (256 bits) em UTF-8, o mínimo do HS256. */
	static SecretKey chave(String segredo) {
		byte[] bytes = segredo == null ? new byte[0] : segredo.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new IllegalStateException(MENSAGEM_SEGREDO);
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey chaveJwt) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
	}

	/**
	 * O validador de datas usa o Clock da aplicação (e não o relógio do sistema): assim o teste vence um token
	 * avançando o relógio de teste. A tolerância padrão é de 60 s.
	 */
	@Bean
	JwtDecoder jwtDecoder(SecretKey chaveJwt, Clock relogio) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
		JwtTimestampValidator datas = new JwtTimestampValidator();
		datas.setClock(relogio);
		OAuth2TokenValidator<Jwt> validador = new DelegatingOAuth2TokenValidator<>(datas,
				new JwtIssuerValidator(TokenService.EMISSOR));
		decoder.setJwtValidator(validador);
		return decoder;
	}
}
