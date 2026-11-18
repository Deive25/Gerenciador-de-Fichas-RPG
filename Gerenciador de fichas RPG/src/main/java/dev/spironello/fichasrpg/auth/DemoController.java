package dev.spironello.fichasrpg.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * A porta da demonstração (2.6). Só existe com o perfil {@code demo}: sem ele a rota não está mapeada e responde 404,
 * então uma instalação que não quer visitantes não tem a rota.
 */
@Tag(name = "Autenticação")
@Profile("demo")
@RestController
public class DemoController {

	private final DemoService demo;

	public DemoController(DemoService demo) {
		this.demo = demo;
	}

	@Operation(summary = "Cria uma conta temporária de demonstração (só com o perfil demo)",
			description = "Sem corpo: cria o visitante, a 'Mesa de demonstração' (ele é o mestre) e duas fichas de exemplo. "
					+ "Com {\"codigoConvite\"} de uma mesa demo: cria um jogador demo nessa mesa. As contas demo são apagadas em 24 h.")
	@SecurityRequirements
	@PostMapping("/auth/demo")
	public RespostaDemo entrar(@RequestBody(required = false) DemoRequest pedido) {
		return demo.entrar(pedido == null ? null : pedido.codigoConvite());
	}
}
