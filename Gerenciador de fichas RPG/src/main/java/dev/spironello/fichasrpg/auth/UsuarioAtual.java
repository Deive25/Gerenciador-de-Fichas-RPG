package dev.spironello.fichasrpg.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** O usuário logado: o id vem do "sub" do JWT já validado pelo resource server. */
@Component
public class UsuarioAtual {

	public Long id() {
		Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacao instanceof JwtAuthenticationToken jwt) {
			return Long.valueOf(jwt.getToken().getSubject());
		}
		throw new IllegalStateException("Nenhum usuário autenticado no contexto de segurança");
	}
}
