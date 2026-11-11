package dev.spironello.fichasrpg.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.spironello.fichasrpg.dto.EspacoDnd;
import dev.spironello.fichasrpg.dto.EspacoRequest;
import dev.spironello.fichasrpg.service.EspacosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Espaços de magia de D&D: uma rota própria, fora do PUT da ficha (ADR 0018). */
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Espaços de magia", description = "Espaços de magia da ficha de D&D (níveis 1 a 9, total e usados)")
@RestController
@RequestMapping("/fichas/{id}/espacos")
public class EspacosController {

	private final EspacosService service;

	public EspacosController(EspacosService service) {
		this.service = service;
	}

	@Operation(summary = "Muda só os níveis enviados e devolve os nove (só em ficha de D&D)")
	@PatchMapping
	public Map<Integer, EspacoDnd> atualizar(@PathVariable Long id, @RequestBody Map<String, EspacoRequest> request) {
		return service.atualizar(id, request);
	}
}
