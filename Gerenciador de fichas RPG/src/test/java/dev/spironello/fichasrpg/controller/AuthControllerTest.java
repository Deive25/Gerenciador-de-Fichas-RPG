package dev.spironello.fichasrpg.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.RelogioDeTesteConfig.RelogioAjustavel;
import dev.spironello.fichasrpg.auth.Usuario;
import dev.spironello.fichasrpg.auth.UsuarioRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * Cadastro, login, renovação e saída com tokens REAIS: este teste monta o próprio MockMvc, sem o jwt() padrão da base
 * (que autentica as outras classes como o usuário de teste). Os e-mails de teste terminam em @auth.test e são apagados no fim.
 */
@DisplayName("Autenticação (/auth/* e /me)")
class AuthControllerTest extends IntegrationTestBase {

	private static final Instant INICIO = Instant.parse("2026-01-01T00:00:00Z");
	private static final String SENHA = "dragão vermelho na taverna";
	/** 72 caracteres (e 72 bytes): o limite do BCrypt. */
	private static final String SENHA_72 = "dragao vermelho dorme na taverna do velho mestre perto da ponte antiga e";

	@Autowired
	private WebApplicationContext contexto;
	@Autowired
	private RelogioAjustavel relogio;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private PasswordEncoder encoder;
	@Autowired
	private ObjectMapper json;

	private MockMvc mvc;

	@BeforeEach
	void montarMvc() {
		relogio.definir(INICIO);
		// A base deixa o usuário de teste no contexto de segurança; aqui os pedidos precisam chegar anônimos, só com o token real.
		TestSecurityContextHolder.clearContext();
		mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
	}

	@AfterEach
	void limpar() {
		jdbc.update("delete from refresh_token where usuario_id in (select id from usuario where email like '%@auth.test')");
		jdbc.update("delete from usuario where email like '%@auth.test'");
		relogio.definir(INICIO);
	}

	// ---------- cadastro ----------

	@Test
	@DisplayName("cadastro: 201, Location /me e o par de tokens; o e-mail ' Ana@Auth.test ' é gravado como ana@auth.test")
	void cadastro() throws Exception {
		mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(Map.of("nome", " Ana ", "email", " Ana@Auth.test ", "senha", SENHA))))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/me"))
				.andExpect(jsonPath("$.accessToken", notNullValue()))
				.andExpect(jsonPath("$.refreshToken", notNullValue()))
				.andExpect(jsonPath("$.usuario.id", notNullValue()))
				.andExpect(jsonPath("$.usuario.nome").value("Ana"))
				.andExpect(jsonPath("$.usuario.email").value("ana@auth.test"));

		assertThat(jdbc.queryForList("select email from usuario where email like '%@auth.test'", String.class))
				.containsExactly("ana@auth.test");
	}

	@Test
	@DisplayName("cadastro com e-mail repetido (mesmo em outra caixa) dá 409 com a mensagem em errors.email")
	void cadastroRepetido() throws Exception {
		cadastrar("bia@auth.test");

		mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(Map.of("nome", "Outra", "email", "BIA@auth.test", "senha", SENHA))))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors.email").value("E-mail já cadastrado"));
	}

	@Test
	@DisplayName("cadastro inválido dá 400 por campo: nome vazio, e-mail inválido e senha com 7 caracteres")
	void cadastroInvalido() throws Exception {
		mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(Map.of("nome", "  ", "email", "isto-nao-e-email", "senha", "1234567"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nome").exists())
				.andExpect(jsonPath("$.errors.email").exists())
				.andExpect(jsonPath("$.errors.senha").exists());
	}

	@Test
	@DisplayName("senha com mais de 72 bytes em UTF-8 (37 'ç' = 74 bytes, mas só 37 caracteres) dá 400 em errors.senha")
	void senhaComMaisDe72Bytes() throws Exception {
		mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(Map.of("nome", "Caio", "email", "caio@auth.test", "senha", "ç".repeat(37)))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.senha").value("A senha pode ter no máximo 72 caracteres"));

		assertThat(jdbc.queryForObject("select count(*) from usuario where email = 'caio@auth.test'", Integer.class)).isZero();
	}

	@Test
	@DisplayName("senha com 73 caracteres dá 400; com 72 caracteres ASCII é aceita")
	void limiteDaSenha() throws Exception {
		mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(Map.of("nome", "Dan", "email", "dan@auth.test", "senha", SENHA_72 + "x"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.senha").exists());
		mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(Map.of("nome", "Dan", "email", "dan@auth.test", "senha", SENHA_72))))
				.andExpect(status().isCreated());
	}


	@Test
	@DisplayName("senha com 14 caracteres dá 400 em errors.senha 'Use pelo menos 15 caracteres' e nada é gravado")
	void senhaCurta() throws Exception {
		mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(Map.of("nome", "Zeca", "email", "zeca@auth.test", "senha", "dragao na tave"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.senha").value("Use pelo menos 15 caracteres"));

		assertThat(jdbc.queryForObject("select count(*) from usuario where email = 'zeca@auth.test'", Integer.class)).isZero();
	}

	@Test
	@DisplayName("senha comum dá 400 em errors.senha com a mensagem da regra 6")
	void senhaComum() throws Exception {
		mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(Map.of("nome", "Zeca", "email", "zeca@auth.test", "senha", "Senha@2026!!!!!!!"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.senha").value("Essa senha é comum demais. Escolha outra"));
	}

	@Test
	@DisplayName("login com uma senha antiga de 8 caracteres (conta já gravada) continua dando 200: a política vale só no cadastro")
	void loginComSenhaAntigaCurta() throws Exception {
		usuarios.saveAndFlush(new Usuario("velho@auth.test", "Velho", encoder.encode("abc12345"), INICIO));

		login("velho@auth.test", "abc12345")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.usuario.email").value("velho@auth.test"));
	}
	@Test
	@DisplayName("JSON estrito: chave desconhecida no cadastro dá 400")
	void jsonEstrito() throws Exception {
		mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(Map.of("nome", "Eva", "email", "eva@auth.test", "senha", SENHA, "admin", true))))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("o banco guarda a senha só como hash BCrypt ($2...) e o refresh só como SHA-256 em hexadecimal (64), diferente do token")
	void segredosSoComoHash() throws Exception {
		String refresh = cadastrar("fabio@auth.test").refresh();

		String senhaHash = jdbc.queryForObject("select senha_hash from usuario where email = 'fabio@auth.test'", String.class);
		assertThat(senhaHash).startsWith("$2").isNotEqualTo(SENHA);
		String hash = jdbc.queryForObject("select hash from refresh_token where usuario_id = "
				+ "(select id from usuario where email = 'fabio@auth.test')", String.class);
		assertThat(hash).hasSize(64).matches("[0-9a-f]{64}").isNotEqualTo(refresh);
	}

	// ---------- login ----------

	@Test
	@DisplayName("login: 200 com o mesmo formato do cadastro")
	void login() throws Exception {
		cadastrar("gina@auth.test");

		login("gina@auth.test", SENHA)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken", notNullValue()))
				.andExpect(jsonPath("$.refreshToken", notNullValue()))
				.andExpect(jsonPath("$.usuario.email").value("gina@auth.test"));
	}

	@Test
	@DisplayName("login com ' Arthur@Auth.TEST ' (outra caixa e espaços) entra na conta arthur@auth.test")
	void loginComOutraCaixaEEspacos() throws Exception {
		cadastrar("arthur@auth.test");

		login(" Arthur@Auth.TEST ", SENHA)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.usuario.email").value("arthur@auth.test"));
	}

	@Test
	@DisplayName("login: senha errada, e-mail inexistente e o usuário legado dão o MESMO 401 'E-mail ou senha incorretos'")
	void mesmoErroNasTresFalhas() throws Exception {
		cadastrar("hugo@auth.test");

		String senhaErrada = login("hugo@auth.test", "senha-errada-1").andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();
		String inexistente = login("ninguem@auth.test", SENHA).andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();
		String legado = login("legado@fichas-rpg.invalid", SENHA).andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();

		assertThat(JsonPath.<String>read(senhaErrada, "$.detail")).isEqualTo("E-mail ou senha incorretos");
		assertThat(inexistente).isEqualTo(senhaErrada);
		assertThat(legado).isEqualTo(senhaErrada);
	}

	@Test
	@DisplayName("login com senha de mais de 72 bytes dá o mesmo 401 (o BCrypt nem é consultado) e campo vazio dá 400")
	void loginComSenhaGrande() throws Exception {
		cadastrar("ivo@auth.test");

		login("ivo@auth.test", "ç".repeat(60)).andExpect(status().isUnauthorized());
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo(Map.of("email", "ivo@auth.test"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.senha").exists());
	}

	// ---------- refresh ----------

	@Test
	@DisplayName("refresh troca o par: o token novo é diferente e funciona; a resposta traz o usuário")
	void refreshTrocaOPar() throws Exception {
		Par inicial = cadastrar("joana@auth.test");

		Par novo = lerPar(renovar(inicial.refresh()).andExpect(status().isOk())
				.andExpect(jsonPath("$.usuario.email").value("joana@auth.test")));

		assertThat(novo.refresh()).isNotEqualTo(inicial.refresh());
		renovar(novo.refresh()).andExpect(status().isOk());
	}

	@Test
	@DisplayName("reuso: reapresentar o refresh antigo dá 401 e derruba a própria família (o refresh novo também dá 401)")
	void reusoDerrubaAFamilia() throws Exception {
		Par inicial = cadastrar("kai@auth.test");
		Par novo = lerPar(renovar(inicial.refresh()).andExpect(status().isOk()));

		renovar(inicial.refresh()).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Sessão expirada. Entre de novo."));

		renovar(novo.refresh()).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("a revogação da família por reuso continua GRAVADA depois do 401 (o 401 é lançado fora da transação)")
	void revogacaoGravadaDepoisDo401() throws Exception {
		Par inicial = cadastrar("lia@auth.test");
		lerPar(renovar(inicial.refresh()).andExpect(status().isOk()));
		Long usuarioId = jdbc.queryForObject("select id from usuario where email = 'lia@auth.test'", Long.class);
		assertThat(jdbc.queryForObject("select count(*) from refresh_token where usuario_id = ? and revogado_em is null",
				Integer.class, usuarioId)).isEqualTo(1); // o antigo foi revogado na troca; o novo está vivo

		renovar(inicial.refresh()).andExpect(status().isUnauthorized());

		assertThat(jdbc.queryForObject("select count(*) from refresh_token where usuario_id = ? and revogado_em is null",
				Integer.class, usuarioId)).isZero();
	}

	@Test
	@DisplayName("o reuso derruba SÓ a família do token: outra sessão (outro login) do mesmo usuário continua viva")
	void reusoDerrubaSoAPropriaFamilia() throws Exception {
		Par celular = cadastrar("mara@auth.test");
		Par pc = lerPar(login("mara@auth.test", SENHA).andExpect(status().isOk()));
		lerPar(renovar(celular.refresh()).andExpect(status().isOk()));

		renovar(celular.refresh()).andExpect(status().isUnauthorized()); // reuso no celular

		renovar(pc.refresh()).andExpect(status().isOk());
	}

	@Test
	@DisplayName("refresh desconhecido ou vencido (31 dias) dá 401 e não revoga nada")
	void refreshDesconhecidoOuVencido() throws Exception {
		renovar("token-que-nao-existe").andExpect(status().isUnauthorized());

		Par par = cadastrar("nina@auth.test");
		relogio.avancar(Duration.ofDays(31));
		renovar(par.refresh()).andExpect(status().isUnauthorized());

		assertThat(jdbc.queryForObject("select count(*) from refresh_token where usuario_id = "
				+ "(select id from usuario where email = 'nina@auth.test') and revogado_em is not null", Integer.class)).isZero();
	}

	@Test
	@DisplayName("o refresh vale 30 dias a partir da ÚLTIMA renovação: renovar aos 20 dias mantém a sessão aos 40")
	void refreshVale30DiasDaUltimaRenovacao() throws Exception {
		Par par = cadastrar("olga@auth.test");
		relogio.avancar(Duration.ofDays(20));
		Par renovado = lerPar(renovar(par.refresh()).andExpect(status().isOk()));

		relogio.avancar(Duration.ofDays(20)); // 40 dias desde o login, 20 desde a renovação

		renovar(renovado.refresh()).andExpect(status().isOk());
	}

	@Test
	@DisplayName("duas renovações simultâneas com o mesmo refresh: uma passa (200) e a outra é recusada (401), sem dois filhos")
	void renovacoesSimultaneas() throws Exception {
		Par par = cadastrar("paulo@auth.test");
		int tentativas = 2;
		CountDownLatch largada = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(tentativas);
		try {
			List<Future<Integer>> resultados = new ArrayList<>();
			for (int i = 0; i < tentativas; i++) {
				resultados.add(pool.submit(() -> {
					largada.await();
					return renovar(par.refresh()).andReturn().getResponse().getStatus();
				}));
			}
			largada.countDown();
			List<Integer> codigos = new ArrayList<>();
			for (Future<Integer> resultado : resultados) {
				codigos.add(resultado.get());
			}
			assertThat(codigos).containsExactlyInAnyOrder(200, 401);
		} finally {
			pool.shutdownNow();
		}
	}

	// ---------- logout ----------

	@Test
	@DisplayName("logout: 204; depois dele aquele refresh dá 401, e o token de acesso vale até vencer (JWT sem estado)")
	void logout() throws Exception {
		Par par = cadastrar("quim@auth.test");

		sair(par.refresh()).andExpect(status().isNoContent());

		renovar(par.refresh()).andExpect(status().isUnauthorized());
		mvc.perform(get("/me").header("Authorization", "Bearer " + par.acesso())).andExpect(status().isOk());
	}

	@Test
	@DisplayName("logout com token desconhecido dá 204 do mesmo jeito; sem o campo dá 400")
	void logoutDesconhecido() throws Exception {
		sair("token-que-nao-existe").andExpect(status().isNoContent());

		mvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("o logout revoga só o token apresentado: a outra sessão do mesmo usuário continua")
	void logoutNaoDerrubaOutraSessao() throws Exception {
		Par celular = cadastrar("rita@auth.test");
		Par pc = lerPar(login("rita@auth.test", SENHA).andExpect(status().isOk()));

		sair(celular.refresh()).andExpect(status().isNoContent());

		renovar(pc.refresh()).andExpect(status().isOk());
	}

	// ---------- token de acesso ----------

	@Test
	@DisplayName("o token recém-emitido vale; com o relógio avançado 17 minutos (15 de validade + 60 s de tolerância) dá 401")
	void tokenVenceComORelogio() throws Exception {
		Par par = cadastrar("sara@auth.test");

		mvc.perform(get("/me").header("Authorization", "Bearer " + par.acesso()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("sara@auth.test"));

		relogio.avancar(Duration.ofMinutes(17));

		mvc.perform(get("/me").header("Authorization", "Bearer " + par.acesso()))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", "Bearer"));
	}

	@Test
	@DisplayName("sem token, GET /me e GET /fichas dão 401 em ProblemDetail 'Faça login para continuar.' com WWW-Authenticate: Bearer")
	void semToken() throws Exception {
		for (String rota : List.of("/me", "/fichas", "/fichas/1")) {
			mvc.perform(get(rota))
					.andExpect(status().isUnauthorized())
					.andExpect(header().string("WWW-Authenticate", "Bearer"))
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
					.andExpect(jsonPath("$.detail").value("Faça login para continuar."));
		}
	}

	@Test
	@DisplayName("token adulterado ou lixo dá o mesmo 401 com WWW-Authenticate: Bearer")
	void tokenInvalido() throws Exception {
		String acesso = cadastrar("tito@auth.test").acesso();
		String adulterado = acesso.substring(0, acesso.length() - 2) + (acesso.endsWith("AA") ? "BB" : "AA");

		for (String token : List.of(adulterado, "isto.nao.e-um-jwt")) {
			mvc.perform(get("/me").header("Authorization", "Bearer " + token))
					.andExpect(status().isUnauthorized())
					.andExpect(header().string("WWW-Authenticate", "Bearer"))
					.andExpect(jsonPath("$.detail").value("Faça login para continuar."));
		}
	}

	@Test
	@DisplayName("a documentação (/v3/api-docs) é aberta e declara o esquema Bearer")
	void rotasAbertas() throws Exception {
		mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
		mvc.perform(get("/v3/api-docs")).andExpect(content().string(containsString("bearer")));
	}

	// ---------- apoio ----------

	private record Par(String acesso, String refresh) {
	}

	private Par cadastrar(String email) throws Exception {
		return lerPar(mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(Map.of("nome", "Teste", "email", email, "senha", SENHA))))
				.andExpect(status().isCreated()));
	}

	private ResultActions login(String email, String senha) throws Exception {
		return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(Map.of("email", email, "senha", senha))));
	}

	private ResultActions renovar(String refresh) throws Exception {
		return mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(Map.of("refreshToken", refresh))));
	}

	private ResultActions sair(String refresh) throws Exception {
		return mvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON)
				.content(corpo(Map.of("refreshToken", refresh))));
	}

	private Par lerPar(ResultActions resposta) throws Exception {
		String corpo = resposta.andReturn().getResponse().getContentAsString();
		return new Par(JsonPath.read(corpo, "$.accessToken"), JsonPath.read(corpo, "$.refreshToken"));
	}

	private String corpo(Map<String, Object> mapa) throws Exception {
		return json.writeValueAsString(mapa);
	}
}
