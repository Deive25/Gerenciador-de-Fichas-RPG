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

import dev.spironello.fichasrpg.dto.ItemRequest;
import dev.spironello.fichasrpg.dto.ItemResponse;
import dev.spironello.fichasrpg.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Itens do inventário da ficha (aba Inventário): rotas próprias, fora do PUT da ficha (ADR 0018). */
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Itens", description = "Itens do inventário da ficha (os campos dependem do sistema da ficha)")
@RestController
@RequestMapping("/fichas/{id}/itens")
public class ItemController {

	private final ItemService service;

	public ItemController(ItemService service) {
		this.service = service;
	}

	@Operation(summary = "Acrescenta um item ao inventário da ficha")
	@PostMapping
	public ResponseEntity<ItemResponse> criar(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
		ItemResponse criado = service.criar(id, request);
		return ResponseEntity.created(URI.create("/fichas/" + id + "/itens/" + criado.id())).body(criado);
	}

	@Operation(summary = "Substitui os dados de um item do inventário da ficha")
	@PutMapping("/{itemId}")
	public ItemResponse atualizar(@PathVariable Long id, @PathVariable Long itemId,
			@Valid @RequestBody ItemRequest request) {
		return service.atualizar(id, itemId, request);
	}

	@Operation(summary = "Remove um item do inventário da ficha")
	@DeleteMapping("/{itemId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id, @PathVariable Long itemId) {
		service.excluir(id, itemId);
	}
}
