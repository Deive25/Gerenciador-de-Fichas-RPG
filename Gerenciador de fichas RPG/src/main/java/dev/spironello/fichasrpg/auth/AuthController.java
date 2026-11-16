package dev.spironello.fichasrpg.auth;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.spironello.fichasrpg.exception.SessaoExpiradaException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Autenticação", description = "Cadastro, login, renovação e saída (JWT de acesso e refresh rotacionado)")
@RestController
public class AuthController {

	private final ContaService contas;
	private final TokenService tokens;
	private final UsuarioAtual usuarioAtual;

	public AuthController(ContaService contas, TokenService tokens, UsuarioAtual usuarioAtual) {
		this.contas = contas;
		this.tokens = tokens;
		this.usuarioAtual = usuarioAtual;
	}

	@Operation(summary = "Cria uma conta e já entra nela")
	@SecurityRequirements
	@PostMapping("/auth/registro")
	public ResponseEntity<RespostaAuth> registrar(@Valid @RequestBody RegistroRequest pedido) {
		return ResponseEntity.created(URI.create("/me")).body(contas.registrar(pedido));
	}

	@Operation(summary = "Entra com e-mail e senha")
	@SecurityRequirements
	@PostMapping("/auth/login")
	public RespostaAuth entrar(@Valid @RequestBody LoginRequest pedido) {
		return contas.entrar(pedido);
	}

	/**
	 * Este método NÃO é transacional de propósito: o TokenService.renovar tem a transação dele e devolve um
	 * resultado. A exceção do 401 é lançada aqui, depois do commit, para a revogação da família por reuso ficar gravada.
	 */
	@Operation(summary = "Troca o refresh token por um par novo (o antigo deixa de valer)")
	@SecurityRequirements
	@PostMapping("/auth/refresh")
	public RespostaAuth renovar(@Valid @RequestBody RefreshRequest pedido) {
		return switch (tokens.renovar(pedido.refreshToken())) {
			case ResultadoDoRefresh.Renovado renovado -> renovado.resposta();
			case ResultadoDoRefresh.Recusado recusado -> throw new SessaoExpiradaException();
		};
	}

	@Operation(summary = "Encerra a sessão revogando o refresh apresentado (204 sempre)")
	@SecurityRequirements
	@PostMapping("/auth/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void sair(@Valid @RequestBody RefreshRequest pedido) {
		tokens.revogar(pedido.refreshToken());
	}

	@Operation(summary = "Dados do usuário logado")
	@GetMapping("/me")
	public UsuarioResponse eu() {
		return contas.eu(usuarioAtual.id());
	}
}
