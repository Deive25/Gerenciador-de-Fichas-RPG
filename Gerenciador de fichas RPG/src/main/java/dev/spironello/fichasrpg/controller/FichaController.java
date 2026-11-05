package dev.spironello.fichasrpg.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.spironello.fichasrpg.dto.FichaRequest;
import dev.spironello.fichasrpg.dto.FichaResponse;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.service.FichaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Fichas", description = "CRUD de fichas de personagem (o campo sistema escolhe o tipo da ficha)")
@RestController
@RequestMapping("/fichas")
public class FichaController {

	private final FichaService service;

	public FichaController(FichaService service) {
		this.service = service;
	}

	@Operation(summary = "Lista o resumo de todas as fichas cadastradas")
	@GetMapping
	public List<FichaResumoResponse> listar() {
		return service.listar();
	}

	@Operation(summary = "Busca uma ficha completa pelo id")
	@GetMapping("/{id}")
	public FichaResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@Operation(summary = "Cadastra uma nova ficha")
	@PostMapping
	public ResponseEntity<FichaResponse> cadastrar(@Valid @RequestBody FichaRequest request) {
		FichaResponse criada = service.criar(request);
		return ResponseEntity.created(URI.create("/fichas/" + criada.id())).body(criada);
	}

	@Operation(summary = "Substitui os dados de uma ficha existente (o sistema não muda)")
	@PutMapping("/{id}")
	public FichaResponse atualizar(@PathVariable Long id, @Valid @RequestBody FichaRequest request) {
		return service.atualizar(id, request);
	}

	@Operation(summary = "Edição rápida: muda só os valores atuais dos recursos enviados (PV, SAN, PE em Ordem)")
	@PatchMapping("/{id}/recursos")
	public FichaResponse atualizarRecursos(@PathVariable Long id, @RequestBody Map<String, Integer> atuais) {
		return service.atualizarRecursos(id, atuais);
	}

	@Operation(summary = "Exclui a ficha com o id informado")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id) {
		service.excluir(id);
	}
}
