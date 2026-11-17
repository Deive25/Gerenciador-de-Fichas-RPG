package dev.spironello.fichasrpg.auth;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Segurança da API. Sessão stateless e CSRF desligado: o token vai no cabeçalho Authorization, que o navegador não
 * envia sozinho, então o CSRF não se aplica. As rotas de /auth e a documentação são abertas; o resto exige o JWT.
 * O CORS fica só aqui (a origem vem de app.cors.origens), e não em cada controller.
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain filtros(HttpSecurity http, ProblemaDeAutenticacao problema) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(rotas -> rotas
						.requestMatchers("/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						// a sonda de vida do deploy (não consulta o banco); o health completo e o resto do actuator exigem login
						.requestMatchers(HttpMethod.GET, "/actuator/health/liveness").permitAll()
						// a prévia do convite é pública (só o GET); aceitar exige token
						.requestMatchers(HttpMethod.GET, "/convites/*").permitAll()
						// o handshake do WebSocket é HTTP sem token; a autenticação acontece no CONNECT do STOMP (AutenticacaoStomp)
						.requestMatchers("/ws", "/ws/**").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(erros -> erros.authenticationEntryPoint(problema))
				.oauth2ResourceServer(recurso -> recurso.jwt(Customizer.withDefaults())
						.authenticationEntryPoint(problema));
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/** Mesma política que os 12 @CrossOrigin tinham, agora num lugar só. O 2.6 só troca a propriedade. */
	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.origens:http://localhost:4200}") List<String> origens) {
		CorsConfiguration configuracao = new CorsConfiguration();
		configuracao.setAllowedOrigins(origens);
		configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuracao.setAllowedHeaders(List.of("*"));
		configuracao.setMaxAge(1800L);
		UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
		fonte.registerCorsConfiguration("/**", configuracao);
		return fonte;
	}
}
