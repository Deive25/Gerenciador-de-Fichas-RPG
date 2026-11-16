package dev.spironello.fichasrpg.auth;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Resposta para quem chega sem token, com token inválido ou vencido: 401 em ProblemDetail (como os outros erros da
 * API) e o cabeçalho WWW-Authenticate: Bearer. Fica no filtro de segurança, antes de qualquer controller.
 */
@Component
public class ProblemaDeAutenticacao implements AuthenticationEntryPoint {

	private final ObjectMapper mapper;

	public ProblemaDeAutenticacao(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Faça login para continuar.");
		problema.setTitle("Não autenticado");
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getOutputStream().write(mapper.writeValueAsBytes(problema));
	}
}
