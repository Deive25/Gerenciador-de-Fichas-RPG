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
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
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

	@ExceptionHandler(FichaNaoEncontradaException.class)
	public ProblemDetail handleNotFound(FichaNaoEncontradaException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
				"Não existe ficha com o id " + ex.getId() + ".");
		problema.setTitle("Ficha não encontrada");
		return problema;
	}

	/** Campanha que não existe ou de que quem pede não participa: 404 (a API não revela quais ids existem). */
	@ExceptionHandler(CampanhaNaoEncontradaException.class)
	public ProblemDetail handleCampanhaNaoEncontrada(CampanhaNaoEncontradaException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Campanha não encontrada");
		problema.setTitle("Campanha não encontrada");
		return problema;
	}

	/** O usuário a remover não participa da campanha. */
	@ExceptionHandler(MembroNaoEncontradoException.class)
	public ProblemDetail handleMembroNaoEncontrado(MembroNaoEncontradoException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Membro não encontrado");
		problema.setTitle("Membro não encontrado");
		return problema;
	}

	/** Código de convite inexistente, renovado ou desativado. */
	@ExceptionHandler(ConviteInvalidoException.class)
	public ProblemDetail handleConviteInvalido(ConviteInvalidoException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Convite inválido ou desativado");
		problema.setTitle("Convite inválido ou desativado");
		return problema;
	}

	/** Membro da campanha sem o papel exigido (ou ficha de outro jogador): 403. */
	@ExceptionHandler(SemPermissaoException.class)
	public ProblemDetail handleSemPermissao(SemPermissaoException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
				"Você não tem permissão para isso nesta campanha.");
		problema.setTitle("Sem permissão");
		return problema;
	}

	@ExceptionHandler(MestreNaoSaiException.class)
	public ProblemDetail handleMestreNaoSai(MestreNaoSaiException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"O mestre não sai da campanha. Para encerrá-la, exclua a campanha.");
		problema.setTitle("O mestre não sai da campanha");
		return problema;
	}

	/**
	 * Duas pessoas editando a mesma ficha: a versão do PUT é velha (FichaDesatualizadaException) ou dois pedidos se
	 * sobrepuseram no servidor (a trava otimista do Hibernate). Os dois viram o mesmo 409 (ADR 0025).
	 */
	@ExceptionHandler({ FichaDesatualizadaException.class, ObjectOptimisticLockingFailureException.class })
	public ProblemDetail handleFichaDesatualizada(Exception ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Esta ficha mudou enquanto você editava. Recarregue para ver a versão nova.");
		problema.setTitle("Ficha desatualizada");
		return problema;
	}

	/** Item de uma lista da ficha que não existe ou é de outra ficha (habilidade, ataque, item...). */
	@ExceptionHandler(ItemDaFichaNaoEncontradoException.class)
	public ProblemDetail handleItemNaoEncontrado(ItemDaFichaNaoEncontradoException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
				"Não existe " + ex.getLista() + " com id " + ex.getId() + " nesta ficha.");
		problema.setTitle("Item não encontrado");
		return problema;
	}

	@ExceptionHandler(RetratoNaoEncontradoException.class)
	public ProblemDetail handleRetratoNaoEncontrado(RetratoNaoEncontradoException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
				"A ficha " + ex.getFichaId() + " não tem retrato.");
		problema.setTitle("Retrato não encontrado");
		return problema;
	}

	@ExceptionHandler(RetratoGrandeDemaisException.class)
	public ProblemDetail handleRetratoGrandeDemais(RetratoGrandeDemaisException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE,
				"A imagem pode ter no máximo 200 KB.");
		problema.setTitle("Imagem grande demais");
		return problema;
	}

	@ExceptionHandler(RetratoInvalidoException.class)
	public ProblemDetail handleRetratoInvalido(RetratoInvalidoException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
		problema.setTitle("Imagem inválida");
		return problema;
	}

	/**
	 * 415 em português e dizendo o que a rota aceita (ex.: o retrato só aceita image/jpeg; as fichas,
	 * application/json). Vale para todas as rotas, por isso a mensagem não fala só de imagem.
	 */
	@Override
	protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String aceitos = ex.getSupportedMediaTypes().stream().map(MediaType::toString).distinct()
				.reduce((a, b) -> a + ", " + b).orElse("o tipo documentado");
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
				"Esta rota aceita: " + aceitos + ".");
		problema.setTitle("Tipo de conteúdo não aceito");
		return handleExceptionInternal(ex, problema, headers, status, request);
	}

	/** Bean Validation falhou: 400 com a propriedade extra "errors" (campo -> mensagem). */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> erros = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(erro -> erros.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
		return handleExceptionInternal(ex, problemaDadosInvalidos(erros), headers, status, request);
	}

	/** Regra de validação de um sistema (ex.: Veterano com NEX baixo): o mesmo 400 "Dados inválidos" do Bean Validation. */
	@ExceptionHandler(DadosInvalidosException.class)
	public ProblemDetail handleDadosInvalidos(DadosInvalidosException ex) {
		return problemaDadosInvalidos(ex.getErros());
	}

	/** O PUT trouxe um sistema diferente do da ficha. */
	@ExceptionHandler(SistemaImutavelException.class)
	public ProblemDetail handleSistemaImutavel(SistemaImutavelException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"O sistema de uma ficha não muda.");
		problema.setTitle("Sistema não muda");
		return problema;
	}

	/** Cadastro com e-mail repetido: 409 com a mensagem em errors.email (o front mostra no campo). */
	@ExceptionHandler(EmailJaCadastradoException.class)
	public ProblemDetail handleEmailJaCadastrado(EmailJaCadastradoException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "E-mail já cadastrado");
		problema.setTitle("E-mail já cadastrado");
		problema.setProperty("errors", Map.of("email", "E-mail já cadastrado"));
		return problema;
	}

	/** Login recusado: um 401 só, igual para e-mail inexistente e senha errada. */
	@ExceptionHandler(CredenciaisInvalidasException.class)
	public ProblemDetail handleCredenciaisInvalidas(CredenciaisInvalidasException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos");
		problema.setTitle("Credenciais inválidas");
		return problema;
	}

	/** Refresh inválido, vencido ou reutilizado. */
	@ExceptionHandler(SessaoExpiradaException.class)
	public ProblemDetail handleSessaoExpirada(SessaoExpiradaException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Sessão expirada. Entre de novo.");
		problema.setTitle("Sessão expirada");
		return problema;
	}

	private ProblemDetail problemaDadosInvalidos(Map<String, String> erros) {
		// Só os NOMES dos campos: o valor rejeitado pode ser dado pessoal e não vai para o log.
		log.warn("Validação falhou nos campos {}", erros.keySet());
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Há campos inválidos na requisição. Veja a propriedade errors.");
		problema.setTitle("Dados inválidos");
		problema.setProperty("errors", erros);
		return problema;
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
