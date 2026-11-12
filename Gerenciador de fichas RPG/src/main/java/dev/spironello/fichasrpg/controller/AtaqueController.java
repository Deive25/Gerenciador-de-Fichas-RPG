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

import dev.spironello.fichasrpg.dto.AtaqueRequest;
import dev.spironello.fichasrpg.dto.AtaqueResponse;
import dev.spironello.fichasrpg.dto.RolagemAtaqueRequest;
import dev.spironello.fichasrpg.dto.RolagemAtaqueResponse;
import dev.spironello.fichasrpg.service.AtaqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Ataques da ficha (aba Ataques): rotas próprias, fora do PUT da ficha (ADR 0018), e a rolagem do ataque. */
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Ataques", description = "Ataques da ficha e a rolagem de ataque e dano (os campos dependem do sistema da ficha)")
@RestController
@RequestMapping("/fichas/{id}/ataques")
public class AtaqueController {

	private final AtaqueService service;

	public AtaqueController(AtaqueService service) {
		this.service = service;
	}

	@Operation(summary = "Acrescenta um ataque à ficha")
	@PostMapping
	public ResponseEntity<AtaqueResponse> criar(@PathVariable Long id, @Valid @RequestBody AtaqueRequest request) {
		AtaqueResponse criado = service.criar(id, request);
		return ResponseEntity.created(URI.create("/fichas/" + id + "/ataques/" + criado.id())).body(criado);
	}

	@Operation(summary = "Substitui os dados de um ataque da ficha")
	@PutMapping("/{ataqueId}")
	public AtaqueResponse atualizar(@PathVariable Long id, @PathVariable Long ataqueId,
			@Valid @RequestBody AtaqueRequest request) {
		return service.atualizar(id, ataqueId, request);
	}

	@Operation(summary = "Remove um ataque da ficha")
	@DeleteMapping("/{ataqueId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id, @PathVariable Long ataqueId) {
		service.excluir(id, ataqueId);
	}

	// POST e 200 (não 201): o resultado é aleatório e nenhum recurso é criado. O corpo é opcional (vazio = NORMAL).
	@Operation(summary = "Rola o ataque e o dano de um ataque da ficha",
			description = "Rola o d20 do ataque e depois o dano; num crítico (a margem em Ordem, o 20 natural em D&D) os dados do dano são multiplicados e as constantes não. O corpo { \"modo\": ... } é opcional e só vale em D&D (vantagem e desvantagem).")
	@PostMapping("/{ataqueId}/rolagem")
	public RolagemAtaqueResponse rolar(@PathVariable Long id, @PathVariable Long ataqueId,
			@RequestBody(required = false) RolagemAtaqueRequest request) {
		return service.rolar(id, ataqueId, request == null ? null : request.modo());
	}
}
