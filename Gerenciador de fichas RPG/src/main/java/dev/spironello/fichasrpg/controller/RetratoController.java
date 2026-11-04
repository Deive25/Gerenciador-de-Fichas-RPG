package dev.spironello.fichasrpg.controller;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.spironello.fichasrpg.service.RetratoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Retrato do personagem: o corpo do PUT é a própria imagem (image/jpeg), sem JSON nem multipart.
 * O navegador já manda a imagem recortada e reduzida; o servidor valida mesmo assim.
 */
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Retrato", description = "Imagem do personagem (JPEG até 200 KB e 1000 px)")
@RestController
@RequestMapping("/fichas/{id}/retrato")
public class RetratoController {

	private final RetratoService service;

	public RetratoController(RetratoService service) {
		this.service = service;
	}

	@Operation(summary = "Envia (ou substitui) o retrato da ficha")
	@PutMapping(consumes = MediaType.IMAGE_JPEG_VALUE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void enviar(@PathVariable Long id, HttpServletRequest requisicao) throws IOException {
		service.enviar(id, requisicao.getInputStream(), requisicao.getContentLengthLong());
	}

	/**
	 * A URL que o front usa leva a versão (?v=…), então a resposta pode ficar em cache "para sempre"
	 * (immutable): um retrato novo tem outra URL. "private" porque, com o login (2.2), o retrato é
	 * da conta. nosniff impede o navegador de "adivinhar" outro tipo de conteúdo.
	 */
	@Operation(summary = "Devolve o retrato da ficha (image/jpeg)")
	@GetMapping
	public ResponseEntity<byte[]> buscar(@PathVariable Long id) {
		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_JPEG)
				.cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePrivate().immutable())
				.header("X-Content-Type-Options", "nosniff")
				.body(service.buscar(id).getConteudo());
	}

	@Operation(summary = "Remove o retrato da ficha (204 também quando não havia retrato)")
	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remover(@PathVariable Long id) {
		service.remover(id);
	}
}
