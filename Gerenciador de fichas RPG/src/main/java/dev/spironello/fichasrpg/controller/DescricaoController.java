package dev.spironello.fichasrpg.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.spironello.fichasrpg.dto.DescricaoFicha;
import dev.spironello.fichasrpg.service.DescricaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Descrição da ficha (aparência, história e anotações): uma rota própria, fora do PUT da ficha (ADR 0018). */
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Descrição", description = "Aparência, história e anotações da ficha (até 4000 caracteres cada)")
@RestController
@RequestMapping("/fichas/{id}/descricao")
public class DescricaoController {

	private final DescricaoService service;

	public DescricaoController(DescricaoService service) {
		this.service = service;
	}

	@Operation(summary = "Substitui a descrição da ficha (texto vazio vira null)")
	@PutMapping
	public DescricaoFicha atualizar(@PathVariable Long id, @Valid @RequestBody DescricaoFicha descricao) {
		return service.atualizar(id, descricao);
	}
}
