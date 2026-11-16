package dev.spironello.fichasrpg.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.spironello.fichasrpg.campanha.CampanhaRequest;
import dev.spironello.fichasrpg.campanha.CampanhaResponse;
import dev.spironello.fichasrpg.campanha.CampanhaResumoResponse;
import dev.spironello.fichasrpg.campanha.CampanhaService;
import dev.spironello.fichasrpg.campanha.MembroResponse;
import dev.spironello.fichasrpg.dto.FichaRequest;
import dev.spironello.fichasrpg.dto.FichaResponse;
import dev.spironello.fichasrpg.dto.FichaResumoResponse;
import dev.spironello.fichasrpg.service.FichaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Campanhas", description = "Campanhas, membros e as fichas da mesa (mestre e jogadores)")
@RestController
@RequestMapping("/campanhas")
public class CampanhaController {

	private final CampanhaService service;
	private final FichaService fichas;

	public CampanhaController(CampanhaService service, FichaService fichas) {
		this.service = service;
		this.fichas = fichas;
	}

	@Operation(summary = "Lista as campanhas de que participa, com o papel, o mestre e quantas fichas ele vê")
	@GetMapping
	public List<CampanhaResumoResponse> listar() {
		return service.listar();
	}

	@Operation(summary = "Cria uma campanha; quem cria vira o mestre e o convite nasce ativo")
	@PostMapping
	public ResponseEntity<CampanhaResponse> criar(@Valid @RequestBody CampanhaRequest pedido) {
		CampanhaResponse criada = service.criar(pedido);
		return ResponseEntity.created(URI.create("/campanhas/" + criada.id())).body(criada);
	}

	@Operation(summary = "Busca uma campanha de que participa (o convite só vem para o mestre)")
	@GetMapping("/{id}")
	public CampanhaResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@Operation(summary = "Substitui o nome e a descrição da campanha (só o mestre)")
	@PutMapping("/{id}")
	public CampanhaResponse atualizar(@PathVariable Long id, @Valid @RequestBody CampanhaRequest pedido) {
		return service.atualizar(id, pedido);
	}

	@Operation(summary = "Exclui a campanha; as fichas voltam aos donos, sem campanha (só o mestre)")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id) {
		service.excluir(id);
	}

	@Operation(summary = "Lista os membros, o mestre primeiro (sem e-mail)")
	@GetMapping("/{id}/membros")
	public List<MembroResponse> membros(@PathVariable Long id) {
		return service.membros(id);
	}

	@Operation(summary = "Remove um jogador (o mestre) ou sai da campanha (o próprio jogador); o mestre não sai")
	@DeleteMapping("/{id}/membros/{usuarioId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removerMembro(@PathVariable Long id, @PathVariable Long usuarioId) {
		service.removerMembro(id, usuarioId);
	}

	@Operation(summary = "Lista o resumo das fichas que quem pede vê na mesa (o jogador não vê as do mestre)")
	@GetMapping("/{id}/fichas")
	public List<FichaResumoResponse> fichas(@PathVariable Long id) {
		return service.fichas(id);
	}

	@Operation(summary = "Cadastra uma ficha já dentro da campanha (o dono é quem cria)")
	@PostMapping("/{id}/fichas")
	public ResponseEntity<FichaResponse> criarFicha(@PathVariable Long id, @Valid @RequestBody FichaRequest request) {
		FichaResponse criada = fichas.criarNaCampanha(id, request);
		return ResponseEntity.created(URI.create("/fichas/" + criada.id())).body(criada);
	}
}
