package dev.spironello.fichasrpg.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.spironello.fichasrpg.dto.TestePericiaRequest;
import dev.spironello.fichasrpg.dto.TestePericiaResponse;
import dev.spironello.fichasrpg.service.TestePericiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@CrossOrigin(origins="http://localhost:4200")
@Tag(name = "Testes de perícia", description = "Rolagem de d100 contra o valor de uma perícia da ficha")
@RestController
@RequestMapping("/sheetPlayer/{id}/testes-de-pericia")
public class TestePericiaController {

	private final TestePericiaService service;

	public TestePericiaController(TestePericiaService service) {
		this.service = service;
	}

	// POST e 200 (não 201): o resultado é aleatório e nenhum recurso é criado.
	@Operation(summary = "Rola um d100 contra uma perícia da ficha",
			description = "Sucesso extremo se a rolagem for até um quinto do valor, sucesso até o valor, falha acima. Perícia sem valor na ficha conta como 0.")
	@PostMapping
	public TestePericiaResponse testar(@PathVariable Long id, @Valid @RequestBody TestePericiaRequest request) {
		return service.testar(id, request.pericia());
	}
}
