package dev.spironello.fichasrpg.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

/** Unitário, sem Spring: o 500 genérico não pode vazar o texto interno da exceção. */
@DisplayName("ApiExceptionHandler (unitário)")
class ApiExceptionHandlerTest {

	@Test
	@DisplayName("exceção inesperada vira 500 genérico, sem o texto da exceção")
	void erroInesperado() {
		ProblemDetail problema = new ApiExceptionHandler()
				.handleUnexpected(new RuntimeException("detalhe-interno-secreto"));

		assertThat(problema.getStatus()).isEqualTo(500);
		assertThat(problema.getTitle()).isEqualTo("Erro interno");
		assertThat(problema.getDetail()).doesNotContain("detalhe-interno-secreto");
	}
}
