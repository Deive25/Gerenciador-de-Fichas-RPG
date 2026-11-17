package dev.spironello.fichasrpg.rolagem;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Tag(name = "Rolagens da mesa", description = "Histórico das rolagens de uma campanha (as novas chegam ao vivo pelo WebSocket)")
@RestController
@RequestMapping("/campanhas/{id}/rolagens")
public class HistoricoDeRolagensController {

	private final HistoricoDeRolagensService service;

	public HistoricoDeRolagensController(HistoricoDeRolagensService service) {
		this.service = service;
	}

	@Operation(summary = "Lista as rolagens da campanha, da mais nova para a mais antiga (qualquer membro)",
			description = "Cursor pelo id: 'antes' traz só as rolagens de id menor. 'limite' vai de 1 a 100 (padrão 50). "
					+ "A rolagem de uma ficha do mestre vem com personagem nulo e doMestre verdadeiro.")
	@GetMapping
	public List<RolagemDaMesaResponse> historico(@PathVariable Long id, @RequestParam(required = false) Long antes,
			@RequestParam(defaultValue = "50") @Min(value = 1, message = "deve estar entre 1 e 100")
			@Max(value = 100, message = "deve estar entre 1 e 100") int limite) {
		return service.listar(id, antes, limite);
	}
}
