package dev.spironello.fichasrpg.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.spironello.fichasrpg.dto.RolagemRequest;
import dev.spironello.fichasrpg.dto.RolagemResponse;
import dev.spironello.fichasrpg.service.RolagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Rolagens", description = "Rolagem livre de dados (aba Dados), nos dois sistemas")
@RestController
@RequestMapping("/fichas/{id}/rolagens")
public class RolagemController {

	private final RolagemService service;

	public RolagemController(RolagemService service) {
		this.service = service;
	}

	// POST e 200 (não 201): o resultado é aleatório e nada é gravado.
	@Operation(summary = "Rola uma notação de dados (ex.: 2d6+1d4+3)",
			description = "Até 8 termos NdM (d4, d6, d8, d10, d12, d20 ou d100) e constantes de 0 a 99, ligados por + ou -; até 20 dados; só as constantes podem ser subtraídas. Não grava nada.")
	@PostMapping
	public RolagemResponse rolar(@PathVariable Long id, @Valid @RequestBody RolagemRequest request) {
		return service.rolar(id, request);
	}
}
