package dev.spironello.fichasrpg.auth;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Limite de tentativas nas rotas de entrada (2.6, spec §5.6; fecha um risco do ADR 0022): {@code POST /auth/login},
 * {@code /auth/registro} e {@code /auth/google} (10 por minuto) e {@code /auth/demo} (5 a cada 10 minutos). Janela fixa,
 * contada por IP e por rota, em memória: basta porque há uma instância só (se escalar, trocar por Redis/Bucket4j e
 * registrar no ADR). O {@code /auth/refresh} fica de fora: a rotação do refresh já o protege.
 *
 * O IP é o {@code request.getRemoteAddr()}. Com {@code server.forward-headers-strategy=native} o Tomcat troca esse IP pelo
 * do {@code X-Forwarded-For} só quando o pedido vem de um proxy interno (o do Render); um cabeçalho enviado direto por
 * um cliente de fora é ignorado, então não dá para zerar o contador falsificando-o (veja o IpDoVisitanteTest). O filtro
 * nunca lê o cabeçalho sozinho. O IP não vai para o log. Os limites vêm de propriedades (app.limite.*), para os testes
 * de login não estourarem o 429.
 *
 * Fica na cadeia do Spring Security, depois do CORS: o 429 sai com os cabeçalhos do CORS, e o navegador deixa o front
 * ler a mensagem.
 */
public class LimiteDeTentativasFilter extends OncePerRequestFilter {

	static final String MENSAGEM = "Muitas tentativas. Tente de novo em alguns minutos.";

	private static final Logger log = LoggerFactory.getLogger(LimiteDeTentativasFilter.class);
	private static final Duration INTERVALO_DA_LIMPEZA = Duration.ofMinutes(1);

	/** O limite de uma rota: no máximo {@code maximo} pedidos por janela. */
	record Regra(int maximo, Duration janela) {
	}

	/** Uma janela aberta: vale até {@code fim}, com {@code pedidos} contados. */
	private record Janela(Instant fim, int pedidos) {
		Janela mais() {
			return new Janela(fim, pedidos + 1);
		}
	}

	private final Map<String, Regra> regras;
	private final Clock relogio;
	private final ObjectMapper mapper;
	private final Map<String, Janela> janelas = new ConcurrentHashMap<>();
	private volatile Instant proximaLimpeza = Instant.MIN;

	public LimiteDeTentativasFilter(Regra entrada, Regra demo, Clock relogio, ObjectMapper mapper) {
		this.regras = Map.of(
				"/auth/login", entrada,
				"/auth/registro", entrada,
				"/auth/google", entrada,
				"/auth/demo", demo);
		this.relogio = relogio;
		this.mapper = mapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Regra regra = "POST".equals(request.getMethod()) ? regras.get(request.getRequestURI()) : null;
		if (regra == null) {
			chain.doFilter(request, response);
			return;
		}
		Instant agora = relogio.instant();
		limparSeChegouAHora(agora);
		String rota = request.getRequestURI();
		Janela janela = janelas.compute(request.getRemoteAddr() + " " + rota,
				(chave, atual) -> atual == null || !agora.isBefore(atual.fim()) ? new Janela(agora.plus(regra.janela()), 1) : atual.mais());
		if (janela.pedidos() <= regra.maximo()) {
			chain.doFilter(request, response);
			return;
		}
		if (janela.pedidos() == regra.maximo() + 1) {
			log.warn("Limite de tentativas excedido rota={}", rota);
		}
		recusar(response, agora, janela);
	}

	private void recusar(HttpServletResponse response, Instant agora, Janela janela) throws IOException {
		long segundos = Math.max(1, (long) Math.ceil(Duration.between(agora, janela.fim()).toMillis() / 1000.0));
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, MENSAGEM);
		problema.setTitle("Muitas tentativas");
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(segundos));
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getOutputStream().write(mapper.writeValueAsBytes(problema));
	}

	/** Uma vez por minuto, no pedido que passar da hora: tira as janelas vencidas, para o mapa não crescer com os IPs que passaram. */
	private void limparSeChegouAHora(Instant agora) {
		if (agora.isBefore(proximaLimpeza)) {
			return;
		}
		proximaLimpeza = agora.plus(INTERVALO_DA_LIMPEZA);
		janelas.values().removeIf(janela -> !agora.isBefore(janela.fim()));
	}

	/** Para os testes: quantas janelas estão guardadas. */
	int janelasAtivas() {
		return janelas.size();
	}

	/** Para os testes: esquece tudo (cada teste começa com os contadores zerados). */
	void reiniciar() {
		janelas.clear();
		proximaLimpeza = Instant.MIN;
	}
}
