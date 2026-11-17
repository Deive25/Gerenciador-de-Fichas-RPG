package dev.spironello.fichasrpg.rolagem;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import dev.spironello.fichasrpg.CampanhasDeTeste;
import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.auth.TokenService;
import dev.spironello.fichasrpg.auth.UsuarioRepository;
import dev.spironello.fichasrpg.campanha.Papel;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.repository.FichaRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * WebSocket de verdade (porta aleatória, tokens reais do {@link TokenService}): quem é membro e está inscrito recebe a
 * rolagem que outro fez por REST; quem não é membro é recusado no SUBSCRIBE; o CONNECT sem token válido é recusado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Rolagens ao vivo (STOMP)")
class RolagensAoVivoTest extends IntegrationTestBase {

	private static final long MESTRE = 7_400_001L;
	private static final long JOGADOR = 7_400_002L;
	private static final long DE_FORA = 7_400_003L;

	@LocalServerPort
	private int porta;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private FichaRepository fichas;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private TokenService tokens;
	@Autowired
	private ObjectMapper json;

	private final HttpClient http = HttpClient.newHttpClient();
	private WebSocketStompClient stomp;
	private final ThreadPoolTaskScheduler agendador = new ThreadPoolTaskScheduler();
	private long campanha;
	private long fichaDoJogador;
	private long fichaDoMestre;

	@BeforeEach
	void prepararMesa() {
		CampanhasDeTeste.usuario(jdbc, MESTRE, "Mestre Mor");
		CampanhasDeTeste.usuario(jdbc, JOGADOR, "Lía");
		CampanhasDeTeste.usuario(jdbc, DE_FORA, "De fora");
		campanha = CampanhasDeTeste.campanha(jdbc, "Mesa ao vivo");
		CampanhasDeTeste.participar(jdbc, campanha, MESTRE, Papel.MESTRE);
		CampanhasDeTeste.participar(jdbc, campanha, JOGADOR, Papel.JOGADOR);
		FichaOrdem ficha = FichasDeTeste.ordem("Kai");
		ficha.setDonoId(JOGADOR);
		ficha.setCampanhaId(campanha);
		fichaDoJogador = fichas.saveAndFlush(ficha).getId();
		FichaOrdem npc = FichasDeTeste.ordem("Ogro Secreto");
		npc.setDonoId(MESTRE);
		npc.setCampanhaId(campanha);
		fichaDoMestre = fichas.saveAndFlush(npc).getId();

		stomp = new WebSocketStompClient(new StandardWebSocketClient());
		stomp.setMessageConverter(new JacksonJsonMessageConverter());
		// o autoReceipt e o heartbeat do cliente precisam de um agendador
		agendador.setPoolSize(1);
		agendador.initialize();
		stomp.setTaskScheduler(agendador);
	}

	@AfterEach
	void limpar() {
		stomp.stop();
		agendador.shutdown();
		CampanhasDeTeste.limpar(jdbc, fichas, MESTRE, JOGADOR, DE_FORA);
	}

	private String token(long usuarioId) {
		return tokens.emitirAcesso(usuarios.findById(usuarioId).orElseThrow());
	}

	/** Uma tentativa de conexão: a sessão (se o CONNECT passou) e o que chegou de ERROR ou de queda do transporte. */
	private static final class Conexao extends StompSessionHandlerAdapter {
		final CompletableFuture<StompSession> sessao = new CompletableFuture<>();
		final CompletableFuture<String> recusa = new CompletableFuture<>();

		@Override
		public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
			sessao.complete(session);
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			recusa.complete("ERROR: " + headers.getFirst("message"));
		}

		@Override
		public void handleTransportError(StompSession session, Throwable exception) {
			recusa.complete("TRANSPORTE: " + exception.getClass().getSimpleName());
		}
	}

	private Conexao conectar(String authorization) {
		StompHeaders cabecalhos = new StompHeaders();
		if (authorization != null) {
			cabecalhos.add("Authorization", authorization);
		}
		Conexao conexao = new Conexao();
		stomp.connectAsync("ws://localhost:" + porta + "/ws", new WebSocketHttpHeaders(), cabecalhos, conexao)
				.whenComplete((s, erro) -> {
					if (erro != null) {
						conexao.recusa.complete("FALHA: " + erro.getClass().getSimpleName());
					}
				});
		return conexao;
	}

	private StompSession conectado(long usuarioId) throws Exception {
		return conectar("Bearer " + token(usuarioId)).sessao.get(5, TimeUnit.SECONDS);
	}

	private String destino() {
		return "/topic/campanhas/" + campanha + "/rolagens";
	}

	private HttpResponse<String> requisitar(HttpRequest.Builder pedido) throws Exception {
		return http.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> rolarLivre(long usuarioId, long fichaId) throws Exception {
		return requisitar(HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/fichas/" + fichaId + "/rolagens"))
				.header("Authorization", "Bearer " + token(usuarioId)).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{\"expressao\":\"2d6+3\"}")));
	}

	private HttpResponse<String> historico(long usuarioId) throws Exception {
		return requisitar(HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/campanhas/" + campanha + "/rolagens"))
				.header("Authorization", "Bearer " + token(usuarioId)).GET());
	}

	/** Inscreve a sessão no tópico da campanha; o cliente lê o JSON de cada rolagem como JsonNode. */
	private CompletableFuture<JsonNode> inscrever(StompSession sessao) {
		CompletableFuture<JsonNode> recebida = new CompletableFuture<>();
		sessao.subscribe(destino(), new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return JsonNode.class;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				recebida.complete((JsonNode) payload);
			}
		});
		return recebida;
	}

	/**
	 * O broker simples não manda RECEIPT, então não há como saber que a inscrição já valeu: quem rola rola de novo a cada
	 * 500 ms até a primeira rolagem chegar (no máximo 5 s). Devolve a que chegou pelo tópico.
	 */
	private JsonNode rolarAteChegar(long usuarioId, long fichaId, CompletableFuture<JsonNode> recebida) throws Exception {
		for (int tentativa = 0; tentativa < 10; tentativa++) {
			assertThat(rolarLivre(usuarioId, fichaId).statusCode()).isEqualTo(200);
			try {
				return recebida.get(500, TimeUnit.MILLISECONDS);
			} catch (TimeoutException ainda) {
				// a inscrição ainda não valeu: rola de novo
			}
		}
		throw new AssertionError("a rolagem não chegou pelo tópico em 5 s");
	}

	/** A mesma rolagem, como o GET do histórico a devolve (achada pelo id). */
	private JsonNode doHistorico(long usuarioId, JsonNode aoVivo) throws Exception {
		for (JsonNode linha : json.readTree(historico(usuarioId).body())) {
			if (linha.get("id").asLong() == aoVivo.get("id").asLong()) {
				return linha;
			}
		}
		throw new AssertionError("o id " + aoVivo.get("id") + " não está no histórico");
	}

	@Test
	@DisplayName("o membro inscrito recebe em até 5 s a rolagem que outro fez por REST, com o mesmo id do histórico")
	void membroRecebeARolagemDeOutro() throws Exception {
		JsonNode aoVivo = rolarAteChegar(JOGADOR, fichaDoJogador, inscrever(conectado(MESTRE)));

		assertThat(aoVivo).as("o tópico e o GET trazem exatamente o mesmo JSON").isEqualTo(doHistorico(MESTRE, aoVivo));
		assertThat(aoVivo.get("autor").get("id").asLong()).isEqualTo(JOGADOR);
		assertThat(aoVivo.get("autor").get("nome").asString()).isEqualTo("Lía");
		assertThat(aoVivo.get("personagem").asString()).isEqualTo("Kai");
		assertThat(aoVivo.get("doMestre").asBoolean()).isFalse();
		assertThat(aoVivo.get("tipo").asString()).isEqualTo("LIVRE");
		assertThat(aoVivo.get("rotulo").asString()).isEqualTo("2d6+3");
		assertThat(aoVivo.get("detalhe").isObject()).as("o detalhe vai como objeto no tópico também").isTrue();
		assertThat(aoVivo.get("detalhe").get("total").asInt()).isEqualTo(aoVivo.get("total").asInt());
	}

	@Test
	@DisplayName("o NPC do mestre não vaza: o jogador inscrito recebe 'Mestre' (personagem nulo), e o nome não aparece no tópico nem no GET")
	void nomeDoNpcNaoVazaPeloTopicoNemPeloGet() throws Exception {
		JsonNode aoVivo = rolarAteChegar(MESTRE, fichaDoMestre, inscrever(conectado(JOGADOR)));

		assertThat(aoVivo.get("personagem").isNull()).isTrue();
		assertThat(aoVivo.get("doMestre").asBoolean()).isTrue();
		assertThat(aoVivo.get("autor").get("id").asLong()).isEqualTo(MESTRE);
		assertThat(aoVivo.has("fichaId")).as("sem fichaId, para não revelar o id do NPC").isFalse();
		assertThat(aoVivo.toString()).doesNotContain("Ogro Secreto");
		JsonNode doGet = doHistorico(JOGADOR, aoVivo);
		assertThat(doGet).isEqualTo(aoVivo);
		assertThat(doGet.toString()).doesNotContain("Ogro Secreto");
	}

	@Test
	@DisplayName("quem não é membro recebe ERROR no SUBSCRIBE")
	void naoMembroERecusado() throws Exception {
		Conexao conexao = conectar("Bearer " + token(DE_FORA));
		StompSession sessao = conexao.sessao.get(5, TimeUnit.SECONDS);

		sessao.subscribe(destino(), new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return JsonNode.class;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
			}
		});

		assertThat(conexao.recusa.get(5, TimeUnit.SECONDS)).startsWith("ERROR");
	}

	@Test
	@DisplayName("o CONNECT sem token, ou com token inválido, é recusado")
	void connectSemTokenValidoERecusado() throws Exception {
		Conexao semToken = conectar(null);
		Conexao tokenInvalido = conectar("Bearer isto-nao-e-um-jwt");
		Conexao semBearer = conectar(token(MESTRE));

		for (Conexao conexao : new Conexao[] { semToken, tokenInvalido, semBearer }) {
			assertThat(conexao.recusa.get(5, TimeUnit.SECONDS)).isNotBlank();
			assertThat(conexao.sessao).as("o CONNECT recusado nunca chega a CONNECTED").isNotCompleted();
		}
	}

	@Test
	@DisplayName("o cliente não pode publicar (SEND) no tópico")
	void sendERecusado() throws Exception {
		Conexao conexao = conectar("Bearer " + token(MESTRE));
		StompSession sessao = conexao.sessao.get(5, TimeUnit.SECONDS);

		sessao.send(destino(), "{}");

		assertThat(conexao.recusa.get(5, TimeUnit.SECONDS)).startsWith("ERROR");
	}
}
