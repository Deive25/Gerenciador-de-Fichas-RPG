package dev.spironello.fichasrpg.auth;

import java.io.IOException;
import java.net.URL;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * As chaves públicas do Google (JWKS), com cache e com nova busca quando aparece um "kid" desconhecido. É um bean
 * separado do verificador para o teste trocá-lo por um JWKS em memória, sem rede.
 */
@Configuration
public class GoogleConfig {

	@Bean
	JWKSource<SecurityContext> chavesDoGoogle(@Value("${app.google.jwks-url}") URL url) throws IOException {
		return JWKSourceBuilder.create(url).build();
	}
}
