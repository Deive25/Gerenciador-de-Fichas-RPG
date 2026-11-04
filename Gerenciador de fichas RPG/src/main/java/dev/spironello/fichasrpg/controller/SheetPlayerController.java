package dev.spironello.fichasrpg.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import dev.spironello.fichasrpg.dto.SheetPlayerRequest;
import dev.spironello.fichasrpg.dto.SheetPlayerResponse;
import dev.spironello.fichasrpg.service.SheetPlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@CrossOrigin(origins="http://localhost:4200")
@Tag(name = "Fichas", description = "CRUD de fichas de personagem")
@RestController
@RequestMapping("/fichas")
public class SheetPlayerController {

	private final SheetPlayerService service;

	public SheetPlayerController(SheetPlayerService service) {
		this.service = service;
	}

	@Operation(summary = "Lista todas as fichas cadastradas")
	@GetMapping
	public List<SheetPlayerResponse> listar() {
		return service.listar();
	}

	@Operation(summary = "Busca uma ficha pelo id")
	@GetMapping("/{id}")
	public SheetPlayerResponse listarFicha(@PathVariable Long id) {
		return service.buscar(id);
	}

	@Operation(summary = "Cadastra uma nova ficha")
	@PostMapping
	public ResponseEntity<SheetPlayerResponse> cadastrar(@Valid @RequestBody SheetPlayerRequest request) {
		SheetPlayerResponse criada = service.criar(request);
		return ResponseEntity.created(URI.create("/fichas/" + criada.id())).body(criada);
	}

	@Operation(summary = "Substitui os dados de uma ficha existente")
	@PutMapping("/{id}")
	public SheetPlayerResponse atualizar(@PathVariable Long id, @Valid @RequestBody SheetPlayerRequest request) {
		return service.atualizar(id, request);
	}

	@Operation(summary = "Exclui a ficha com o id informado")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deletarSheetPlayer(@PathVariable Long id) {
		service.excluir(id);
	}
}
