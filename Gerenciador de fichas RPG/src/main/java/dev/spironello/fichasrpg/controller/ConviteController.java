package dev.spironello.fichasrpg.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.spironello.fichasrpg.campanha.AceiteResponse;
import dev.spironello.fichasrpg.campanha.ConviteResponse;
import dev.spironello.fichasrpg.campanha.ConviteService;
import dev.spironello.fichasrpg.campanha.PreviaConviteResponse;
import dev.spironello.fichasrpg.campanha.ResultadoDoAceite;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Convites", description = "Convite por link: o mestre gera, renova e desativa; quem recebe vê a prévia e aceita")
@RestController
public class ConviteController {

	private final ConviteService service;

	public ConviteController(ConviteService service) {
		this.service = service;
	}

	@Operation(summary = "Gera um código novo e ativa o convite; o antigo deixa de valer (só o mestre)")
	@PostMapping("/campanhas/{id}/convite")
	public ConviteResponse renovar(@PathVariable Long id) {
		return service.renovar(id);
	}

	@Operation(summary = "Desativa o convite (só o mestre)")
	@DeleteMapping("/campanhas/{id}/convite")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void desativar(@PathVariable Long id) {
		service.desativar(id);
	}

	@Operation(summary = "Prévia pública do convite: só os nomes da campanha e do mestre")
	@SecurityRequirements
	@GetMapping("/convites/{codigo}")
	public PreviaConviteResponse previa(@PathVariable String codigo) {
		return service.previa(codigo);
	}

	@Operation(summary = "Entra na campanha como jogador (201); quem já é membro recebe 200")
	@PostMapping("/convites/{codigo}/aceitar")
	public ResponseEntity<AceiteResponse> aceitar(@PathVariable String codigo) {
		ResultadoDoAceite resultado = service.aceitar(codigo);
		AceiteResponse corpo = new AceiteResponse(resultado.campanhaId());
		if (resultado.novo()) {
			return ResponseEntity.created(URI.create("/campanhas/" + resultado.campanhaId())).body(corpo);
		}
		return ResponseEntity.ok(corpo);
	}
}
