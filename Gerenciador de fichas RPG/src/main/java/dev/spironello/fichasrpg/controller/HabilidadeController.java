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

import dev.spironello.fichasrpg.dto.HabilidadeRequest;
import dev.spironello.fichasrpg.dto.HabilidadeResponse;
import dev.spironello.fichasrpg.service.HabilidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Habilidades da ficha (aba Habilidades): rotas próprias, fora do PUT da ficha (ADR 0018). */
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Habilidades", description = "Habilidades da ficha (o tipo precisa existir no sistema da ficha)")
@RestController
@RequestMapping("/fichas/{id}/habilidades")
public class HabilidadeController {

	private final HabilidadeService service;

	public HabilidadeController(HabilidadeService service) {
		this.service = service;
	}

	@Operation(summary = "Acrescenta uma habilidade à ficha")
	@PostMapping
	public ResponseEntity<HabilidadeResponse> criar(@PathVariable Long id,
			@Valid @RequestBody HabilidadeRequest request) {
		HabilidadeResponse criada = service.criar(id, request);
		return ResponseEntity.created(URI.create("/fichas/" + id + "/habilidades/" + criada.id())).body(criada);
	}

	@Operation(summary = "Substitui os dados de uma habilidade da ficha")
	@PutMapping("/{habilidadeId}")
	public HabilidadeResponse atualizar(@PathVariable Long id, @PathVariable Long habilidadeId,
			@Valid @RequestBody HabilidadeRequest request) {
		return service.atualizar(id, habilidadeId, request);
	}

	@Operation(summary = "Remove uma habilidade da ficha")
	@DeleteMapping("/{habilidadeId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id, @PathVariable Long habilidadeId) {
		service.excluir(id, habilidadeId);
	}
}
