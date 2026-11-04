package dev.spironello.fichasrpg.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * Traduz exceções em respostas de erro padronizadas (ProblemDetail, RFC 9457,
 * Content-Type application/problem+json). Estende ResponseEntityExceptionHandler, que já
 * cobre as exceções do Spring MVC (rota inexistente, método não permitido, id não numérico
 * etc.) no mesmo formato; aqui só tratamos o que é específico da aplicação.
 * As mensagens nunca incluem stack trace nem texto interno de exceção.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(SheetPlayerNotFoundException.class)
	public ProblemDetail handleNotFound(SheetPlayerNotFoundException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
				"Não existe ficha com o id " + ex.getId() + ".");
		problema.setTitle("Ficha não encontrada");
		return problema;
	}

	/** Bean Validation falhou: 400 com a propriedade extra "errors" (campo -> mensagem). */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> erros = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(erro -> erros.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
		// Só os NOMES dos campos: o valor rejeitado pode ser dado pessoal e não vai para o log.
		log.warn("Validação falhou nos campos {}", erros.keySet());
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Há campos inválidos na requisição. Veja a propriedade errors.");
		problema.setTitle("Dados inválidos");
		problema.setProperty("errors", erros);
		return handleExceptionInternal(ex, problema, headers, status, request);
	}

	/** Parâmetro de rota com tipo errado (ex.: GET /fichas/abc): mensagem em português. */
	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"O parâmetro '" + ex.getPropertyName() + "' deve ser um número inteiro.");
		problema.setTitle("Parâmetro inválido");
		return handleExceptionInternal(ex, problema, headers, status, request);
	}

	/** JSON ilegível: chave desconhecida, tipo errado ou sintaxe inválida. Todos viram 400. */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String detalhe = "O corpo da requisição não é um JSON válido.";
		if (ex.getCause() instanceof UnrecognizedPropertyException desconhecida) {
			detalhe = "Campo desconhecido no JSON: '" + desconhecida.getPropertyName()
					+ "'. Envie somente as chaves documentadas da ficha.";
		} else if (ex.getCause() instanceof JacksonException jackson && !jackson.getPath().isEmpty()) {
			String campo = jackson.getPath().get(jackson.getPath().size() - 1).getPropertyName();
			detalhe = "O valor do campo '" + campo + "' tem um tipo inválido.";
		}
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe);
		problema.setTitle("Corpo da requisição inválido");
		return handleExceptionInternal(ex, problema, headers, status, request);
	}

	/** Rede de segurança: qualquer outra falha vira 500 genérico; o detalhe fica só no log. */
	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex) {
		log.error("Erro inesperado ao processar a requisição", ex);
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"Ocorreu um erro inesperado. Tente novamente mais tarde.");
		problema.setTitle("Erro interno");
		return problema;
	}
}
