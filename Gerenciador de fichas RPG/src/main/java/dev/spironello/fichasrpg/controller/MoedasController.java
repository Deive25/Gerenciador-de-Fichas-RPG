package dev.spironello.fichasrpg.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.spironello.fichasrpg.dto.MoedasDnd;
import dev.spironello.fichasrpg.dto.MoedasRequest;
import dev.spironello.fichasrpg.service.MoedasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Moedas de D&D: uma rota própria, fora do PUT da ficha (ADR 0018). */
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Moedas", description = "Moedas da ficha de D&D (pc, pp, pe, po e pl)")
@RestController
@RequestMapping("/fichas/{id}/moedas")
public class MoedasController {

	private final MoedasService service;

	public MoedasController(MoedasService service) {
		this.service = service;
	}

	@Operation(summary = "Muda só as moedas enviadas e devolve as cinco (só em ficha de D&D)")
	@PatchMapping
	public MoedasDnd atualizar(@PathVariable Long id, @Valid @RequestBody MoedasRequest request) {
		return service.atualizar(id, request);
	}
}
