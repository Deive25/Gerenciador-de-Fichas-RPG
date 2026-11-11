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

import dev.spironello.fichasrpg.dto.RitualRequest;
import dev.spironello.fichasrpg.dto.RitualResponse;
import dev.spironello.fichasrpg.service.RitualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Rituais da ficha de Ordem Paranormal (aba Rituais): rotas próprias, fora do PUT da ficha (ADR 0018). */
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Rituais", description = "Rituais da ficha de Ordem Paranormal (círculos 1 a 4 e elemento)")
@RestController
@RequestMapping("/fichas/{id}/rituais")
public class RitualController {

	private final RitualService service;

	public RitualController(RitualService service) {
		this.service = service;
	}

	@Operation(summary = "Acrescenta um ritual à ficha (só em ficha de Ordem Paranormal)")
	@PostMapping
	public ResponseEntity<RitualResponse> criar(@PathVariable Long id, @Valid @RequestBody RitualRequest request) {
		RitualResponse criado = service.criar(id, request);
		return ResponseEntity.created(URI.create("/fichas/" + id + "/rituais/" + criado.id())).body(criado);
	}

	@Operation(summary = "Substitui os dados de um ritual da ficha")
	@PutMapping("/{ritualId}")
	public RitualResponse atualizar(@PathVariable Long id, @PathVariable Long ritualId,
			@Valid @RequestBody RitualRequest request) {
		return service.atualizar(id, ritualId, request);
	}

	@Operation(summary = "Remove um ritual da ficha")
	@DeleteMapping("/{ritualId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id, @PathVariable Long ritualId) {
		service.excluir(id, ritualId);
	}
}
