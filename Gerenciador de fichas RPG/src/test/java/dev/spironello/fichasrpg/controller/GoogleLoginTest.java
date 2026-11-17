package dev.spironello.fichasrpg.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import dev.spironello.fichasrpg.ChavesDoGoogleDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.RelogioDeTesteConfig.RelogioAjustavel;
import tools.jackson.databind.ObjectMapper;

/**
 * POST /auth/google com um Google de mentira (ChavesDoGoogleDeTeste): JWKS em memória, sem rede. Os e-mails de teste
 * terminam em @google.test e são apagados no fim; a conta do usuário de teste da base nunca é tocada.
 */
@DisplayName("Login com Google (POST /auth/google)")
class GoogleLoginTest extends IntegrationTestBase {

	private static final Instant INICIO = Instant.parse("2026-01-01T00:00:00Z");
	private static final String SENHA = "dragão vermelho na taverna";
	private static final String RECUSADO = "Não foi possível entrar com o Google. Tente de novo.";
	private static final String NAO_VERIFICADO = "Confirme o seu e-mail no Google antes de entrar.";
	private static final String OUTRA_CONTA = "Este e-mail já está ligado a outra conta do Google.";

	@Autowired
	private WebApplicationContext contexto;
	@Autowired
	private RelogioAjustavel relogio;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private ObjectMapper json;
	@Autowired
	private ChavesDoGoogleDeTeste google;

	private MockMvc mvc;

	@BeforeEach
	void montarMvc() {
		relogio.definir(INICIO);
		TestSecurityContextHolder.clearContext();
		mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
		apagar();
	}

	@AfterEach
	void limpar() {
		apagar();
		relogio.definir(INICIO);
	}

	private void apagar() {
		jdbc.update("delete from refresh_token where usuario_id in (select id from usuario where email like '%@google.test')");
		jdbc.update("delete from usuario where email like '%@google.test'");
	}

	// ---------- conta nova ----------

	@Test
	@DisplayName("conta nova: 200, e-mail em minúsculas, nome sem espaços, senha_hash nulo, google_sub gravado e o refresh funciona")
	void contaNovaCriadaComNomeEEmailNormalizado() throws Exception {
		Map<String, Object> claims = google.claimsValidas("sub-nova-1", " Ana.Souza@Google.TEST ");
		claims.put("name", "  Ana Souza  ");

		ResultActions resposta = entrar(google.assinar(claims)).andExpect(status().isOk())
				.andExpect(jsonPath("$.usuario.email").value("ana.souza@google.test"))
				.andExpect(jsonPath("$.usuario.nome").value("Ana Souza"));

		Map<String, Object> linha = jdbc.queryForMap("select senha_hash, google_sub from usuario where email = 'ana.souza@google.test'");
		assertThat(linha.get("senha_hash")).isNull();
		assertThat(linha.get("google_sub")).isEqualTo("sub-nova-1");
		renovar(lerRefresh(resposta)).andExpect(status().isOk());
	}

	@Test
	@DisplayName("nome com 150 caracteres é cortado em 100")
	void nomeLongoCortadoEm100() throws Exception {
		Map<String, Object> claims = google.claimsValidas("sub-longo", "longo@google.test");
		claims.put("name", "a".repeat(150));

		entrar(google.assinar(claims)).andExpect(status().isOk());

		assertThat(jdbc.queryForObject("select nome from usuario where email = 'longo@google.test'", String.class))
				.hasSize(100);
	}

	@Test
	@DisplayName("sem a claim name, o nome é a parte do e-mail antes do @")
	void semNameUsaParteDoEmail() throws Exception {
		Map<String, Object> claims = google.claimsValidas("sub-sem-nome", "joao@google.test");
		claims.remove("name");

		entrar(google.assinar(claims)).andExpect(status().isOk()).andExpect(jsonPath("$.usuario.nome").value("joao"));
	}

	@Test
	@DisplayName("entra de novo pelo sub: o mesmo id, mesmo com outro e-mail no token, e o e-mail gravado não muda")
	void entraDeNovoPeloSubMesmoComOutroEmail() throws Exception {
		ResultActions primeira = entrar(google.assinar(google.claimsValidas("sub-fixo", "antes@google.test")))
				.andExpect(status().isOk());
		int id = JsonPath.read(primeira.andReturn().getResponse().getContentAsString(), "$.usuario.id");

		entrar(google.assinar(google.claimsValidas("sub-fixo", "depois@google.test"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.usuario.id").value(id))
				.andExpect(jsonPath("$.usuario.email").value("antes@google.test"));

		assertThat(jdbc.queryForObject("select count(*) from usuario where email like '%@google.test'", Long.class)).isEqualTo(1);
	}

	// ---------- ligação ----------

	@Test
	@DisplayName("liga a conta por senha: grava o sub, apaga a senha, revoga TODAS as famílias dela e só as dela")
	void ligaContaPorSenhaApagaSenhaERevogaTodasAsFamilias() throws Exception {
		cadastrar("bia@google.test", "Bia");
		String refreshA = lerRefresh(login("bia@google.test"));
		String refreshB = lerRefresh(login("bia@google.test"));
		cadastrar("outro@google.test", "Outro");
		String refreshDeOutro = lerRefresh(login("outro@google.test"));

		entrar(google.assinar(google.claimsValidas("sub-bia", "Bia@google.test"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.usuario.nome").value("Bia"));

		Map<String, Object> linha = jdbc.queryForMap("select senha_hash, google_sub from usuario where email = 'bia@google.test'");
		assertThat(linha.get("senha_hash")).isNull();
		assertThat(linha.get("google_sub")).isEqualTo("sub-bia");
		renovar(refreshA).andExpect(status().isUnauthorized());
		renovar(refreshB).andExpect(status().isUnauthorized());
		login("bia@google.test").andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos"));
		renovar(refreshDeOutro).andExpect(status().isOk());
		assertThat(jdbc.queryForObject("select senha_hash from usuario where email = 'outro@google.test'", String.class))
				.isNotNull();
	}

	@Test
	@DisplayName("e-mail já ligado a outro sub: 401 com a mensagem e nada muda")
	void emailLigadoAOutroSubRecusado() throws Exception {
		entrar(google.assinar(google.claimsValidas("sub-um", "dupla@google.test"))).andExpect(status().isOk());
		long usuarios = contar("usuario");
		long refreshes = contar("refresh_token");

		entrar(google.assinar(google.claimsValidas("sub-dois", "dupla@google.test"))).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value(OUTRA_CONTA))
				.andExpect(jsonPath("$.title").value("Login com Google recusado"));

		assertThat(jdbc.queryForObject("select google_sub from usuario where email = 'dupla@google.test'", String.class))
				.isEqualTo("sub-um");
		assertThat(contar("usuario")).isEqualTo(usuarios);
		assertThat(contar("refresh_token")).isEqualTo(refreshes);
	}

	// ---------- e-mail não verificado ----------

	@ParameterizedTest(name = "email_verified = {0}")
	@ValueSource(strings = { "false", "string true" })
	@NullSource
	@DisplayName("e-mail não verificado (false, a string \"true\" ou ausente): 401 com a mensagem e nenhuma conta criada")
	void emailNaoVerificadoRecusadoSemCriarConta(String valor) throws Exception {
		Map<String, Object> claims = google.claimsValidas("sub-nv", "nv@google.test");
		if (valor == null) {
			claims.remove("email_verified");
		} else if (valor.equals("false")) {
			claims.put("email_verified", false);
		} else {
			claims.put("email_verified", "true");
		}
		long usuarios = contar("usuario");
		long refreshes = contar("refresh_token");

		entrar(google.assinar(claims)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value(NAO_VERIFICADO));

		assertThat(contar("usuario")).isEqualTo(usuarios);
		assertThat(contar("refresh_token")).isEqualTo(refreshes);
	}

	@Test
	@DisplayName("e-mail não verificado de uma conta por senha existente: 401 e a conta não é ligada nem perde a senha")
	void naoVerificadoNaoLigaContaExistente() throws Exception {
		cadastrar("ligavel@google.test", "Ligavel");
		Map<String, Object> claims = google.claimsValidas("sub-lig", "ligavel@google.test");
		claims.put("email_verified", false);

		entrar(google.assinar(claims)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value(NAO_VERIFICADO));

		Map<String, Object> linha = jdbc.queryForMap("select senha_hash, google_sub from usuario where email = 'ligavel@google.test'");
		assertThat(linha.get("senha_hash")).isNotNull();
		assertThat(linha.get("google_sub")).isNull();
	}

	// ---------- token inválido ----------

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "outra chave", "kid desconhecido", "aud errado", "iss errado", "vencido", "HS256 nosso", "texto qualquer" })
	@DisplayName("token inválido: 401 com a mensagem genérica e nada muda no banco")
	void tokenInvalidoRecusado(String caso) throws Exception {
		cadastrar("existente@google.test", "Existente");
		String hs256 = lerAcesso(login("existente@google.test"));
		Map<String, Object> claims = google.claimsValidas("sub-inv", "inv@google.test");
		String token = switch (caso) {
			case "outra chave" -> google.assinarComOutraChave(claims);
			case "kid desconhecido" -> google.assinarComKid(claims, "kid-que-nao-existe");
			case "aud errado" -> {
				claims.put("aud", "999-outro-cliente.apps.googleusercontent.com");
				yield google.assinar(claims);
			}
			case "iss errado" -> {
				claims.put("iss", "https://accounts.exemplo.invalid");
				yield google.assinar(claims);
			}
			case "vencido" -> {
				String valido = google.assinar(claims);
				relogio.avancar(Duration.ofHours(2));
				yield valido;
			}
			case "HS256 nosso" -> hs256;
			default -> "abc";
		};
		long usuarios = contar("usuario");
		long refreshes = contar("refresh_token");

		entrar(token).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value(RECUSADO))
				.andExpect(jsonPath("$.title").value("Login com Google recusado"));

		assertThat(contar("usuario")).isEqualTo(usuarios);
		assertThat(contar("refresh_token")).isEqualTo(refreshes);
	}

	@Test
	@DisplayName("o iss sem https (accounts.google.com), a outra forma que o Google usa, é aceito")
	void issSemHttpsAceito() throws Exception {
		Map<String, Object> claims = google.claimsValidas("sub-iss", "iss@google.test");
		claims.put("iss", "accounts.google.com");

		entrar(google.assinar(claims)).andExpect(status().isOk());
	}

	// ---------- corpo ----------

	@Test
	@DisplayName("sem idToken (ausente ou em branco): 400")
	void semIdTokenDa400() throws Exception {
		mvc.perform(post("/auth/google").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.idToken").exists());
		mvc.perform(post("/auth/google").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"  \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("chave desconhecida no JSON: 400")
	void chaveDesconhecidaDa400() throws Exception {
		mvc.perform(post("/auth/google").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"x\",\"extra\":1}"))
				.andExpect(status().isBadRequest());
	}

	// ---------- rotas do 2.2 ----------

	@Test
	@DisplayName("registro com o e-mail de uma conta do Google: 409 de sempre")
	void registroComEmailDeContaGoogleDa409() throws Exception {
		entrar(google.assinar(google.claimsValidas("sub-reg", "reg@google.test"))).andExpect(status().isOk());

		mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(Map.of("nome", "Outra", "email", "reg@google.test", "senha", SENHA))))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errors.email").value("E-mail já cadastrado"));
	}

	@Test
	@DisplayName("login por senha de uma conta só do Google: o mesmo 401 de e-mail ou senha incorretos")
	void loginPorSenhaDeContaGoogleDa401() throws Exception {
		entrar(google.assinar(google.claimsValidas("sub-sosg", "sosg@google.test"))).andExpect(status().isOk());

		login("sosg@google.test").andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos"));
	}

	// ---------- o decoder do Google não vaza ----------

	@Test
	@DisplayName("só existe UM bean JwtDecoder (o nosso HS256) e o nosso token continua valendo em GET /me")
	void nossoTokenContinuaValendoEmMe() throws Exception {
		assertThat(contexto.getBeansOfType(JwtDecoder.class)).hasSize(1);
		cadastrar("me@google.test", "Eu");
		String acesso = lerAcesso(login("me@google.test"));

		mvc.perform(get("/me").header("Authorization", "Bearer " + acesso)).andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("me@google.test"));

		String doGoogle = lerAcesso(entrar(google.assinar(google.claimsValidas("sub-me", "me2@google.test"))));
		mvc.perform(get("/me").header("Authorization", "Bearer " + doGoogle)).andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("me2@google.test"));
		// O ID token do Google NÃO vale como token de acesso da API.
		String idToken = google.assinar(google.claimsValidas("sub-me", "me2@google.test"));
		mvc.perform(get("/me").header("Authorization", "Bearer " + idToken)).andExpect(status().isUnauthorized());
	}

	// ---------- apoio ----------

	private ResultActions entrar(String idToken) throws Exception {
		return mvc.perform(post("/auth/google").contentType(MediaType.APPLICATION_JSON).content(corpo(Map.of("idToken", idToken))));
	}

	private void cadastrar(String email, String nome) throws Exception {
		mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(Map.of("nome", nome, "email", email, "senha", SENHA)))).andExpect(status().isCreated());
	}

	private ResultActions login(String email) throws Exception {
		return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(Map.of("email", email, "senha", SENHA))));
	}

	private ResultActions renovar(String refresh) throws Exception {
		return mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(Map.of("refreshToken", refresh))));
	}

	private String lerRefresh(ResultActions resposta) throws Exception {
		return JsonPath.read(resposta.andReturn().getResponse().getContentAsString(), "$.refreshToken");
	}

	private String lerAcesso(ResultActions resposta) throws Exception {
		return JsonPath.read(resposta.andReturn().getResponse().getContentAsString(), "$.accessToken");
	}

	private long contar(String tabela) {
		return jdbc.queryForObject("select count(*) from " + tabela, Long.class);
	}

	private String corpo(Map<String, Object> mapa) throws Exception {
		return json.writeValueAsString(mapa);
	}
}
