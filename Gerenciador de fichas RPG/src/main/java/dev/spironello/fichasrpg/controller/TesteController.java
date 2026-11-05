package dev.spironello.fichasrpg.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.spironello.fichasrpg.dto.TesteRequest;
import dev.spironello.fichasrpg.dto.TesteResponse;
import dev.spironello.fichasrpg.service.TesteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Testes", description = "Rolagem de testes de perícia e de atributo de uma ficha")
@RestController
@RequestMapping("/fichas/{id}/testes")
public class TesteController {

	private final TesteService service;

	public TesteController(TesteService service) {
		this.service = service;
	}

	// POST e 200 (não 201): o resultado é aleatório e nenhum recurso é criado.
	@Operation(summary = "Rola um teste de perícia ou de atributo da ficha",
			description = "Em Ordem Paranormal rola tantos d20 quanto o atributo (com atributo 0, rola 2) e fica com o maior (com atributo 0, o menor). O total soma o bônus do grau da perícia.")
	@PostMapping
	public TesteResponse testar(@PathVariable Long id, @Valid @RequestBody TesteRequest request) {
		return service.testar(id, request);
	}
}
