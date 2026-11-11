package dev.spironello.fichasrpg.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.spironello.fichasrpg.dto.MagiaRequest;
import dev.spironello.fichasrpg.dto.MagiaResponse;
import dev.spironello.fichasrpg.service.MagiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Magias da ficha de D&D (aba Magias): rotas próprias, fora do PUT da ficha (ADR 0018). */
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Magias", description = "Magias da ficha de D&D (truques e níveis 1 a 9)")
@RestController
@RequestMapping("/fichas/{id}/magias")
public class MagiaController {

	private final MagiaService service;

	public MagiaController(MagiaService service) {
		this.service = service;
	}

	@Operation(summary = "Acrescenta uma magia à ficha (só em ficha de D&D)")
	@PostMapping
	public ResponseEntity<MagiaResponse> criar(@PathVariable Long id, @Valid @RequestBody MagiaRequest request) {
		MagiaResponse criada = service.criar(id, request);
		return ResponseEntity.created(URI.create("/fichas/" + id + "/magias/" + criada.id())).body(criada);
	}

	@Operation(summary = "Substitui os dados de uma magia da ficha")
	@PutMapping("/{magiaId}")
	public MagiaResponse atualizar(@PathVariable Long id, @PathVariable Long magiaId,
			@Valid @RequestBody MagiaRequest request) {
		return service.atualizar(id, magiaId, request);
	}

	@Operation(summary = "Remove uma magia da ficha")
	@DeleteMapping("/{magiaId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id, @PathVariable Long magiaId) {
		service.excluir(id, magiaId);
	}
}
