package dev.spironello.fichasrpg.auth;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.campanha.Campanha;
import dev.spironello.fichasrpg.campanha.CampanhaRepository;
import dev.spironello.fichasrpg.campanha.ConviteService;
import dev.spironello.fichasrpg.campanha.Papel;
import dev.spironello.fichasrpg.campanha.Participacao;
import dev.spironello.fichasrpg.campanha.ParticipacaoRepository;
import dev.spironello.fichasrpg.dto.AtaqueRequest;
import dev.spironello.fichasrpg.dto.FichaDndRequest;
import dev.spironello.fichasrpg.dto.FichaOrdemRequest;
import dev.spironello.fichasrpg.dto.FichaRequest;
import dev.spironello.fichasrpg.dto.ItemRequest;
import dev.spironello.fichasrpg.dto.RecursoValor;
import dev.spironello.fichasrpg.exception.ConviteInvalidoException;
import dev.spironello.fichasrpg.model.AtributoDnd;
import dev.spironello.fichasrpg.model.AtributoOrdem;
import dev.spironello.fichasrpg.model.ClasseOrdem;
import dev.spironello.fichasrpg.model.EdicaoDnd;
import dev.spironello.fichasrpg.model.Ficha;
import dev.spironello.fichasrpg.model.FichaDnd;
import dev.spironello.fichasrpg.model.FichaOrdem;
import dev.spironello.fichasrpg.model.GrauDnd;
import dev.spironello.fichasrpg.model.GrauOrdem;
import dev.spironello.fichasrpg.model.HabilidadeFicha;
import dev.spironello.fichasrpg.model.PericiaDnd;
import dev.spironello.fichasrpg.model.PericiaOrdem;
import dev.spironello.fichasrpg.model.RecursoDnd;
import dev.spironello.fichasrpg.model.RecursoOrdem;
import dev.spironello.fichasrpg.model.TipoHabilidade;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.repository.HabilidadeFichaRepository;
import dev.spironello.fichasrpg.service.AtaquesDnd;
import dev.spironello.fichasrpg.service.AtaquesOrdem;
import dev.spironello.fichasrpg.service.ItensDnd;
import dev.spironello.fichasrpg.service.ItensOrdem;
import dev.spironello.fichasrpg.service.SistemasDeFicha;

/**
 * A conta de demonstração (2.6, spec §5.4), só com o perfil {@code demo}. Cria o visitante e a mesa dele, ou o jogador
 * de uma mesa demo, tudo numa transação. As fichas são montadas aqui, em código, passando pelas mesmas regras de
 * validação da API (os {@link SistemasDeFicha}); não há SQL de seed. O visitante não tem senha nem Google e o e-mail
 * dele é interno ({@code .invalid}), então a conta só existe pelos tokens que esta resposta devolve. A limpeza
 * ({@code LimpezaAgendada}) apaga tudo em 24 h. Os logs trazem só ids.
 */
@Service
@Profile("demo")
public class DemoService {

	private static final Logger log = LoggerFactory.getLogger(DemoService.class);

	private final UsuarioRepository usuarios;
	private final CampanhaRepository campanhas;
	private final ParticipacaoRepository participacoes;
	private final ConviteService convites;
	private final FichaRepository fichas;
	private final SistemasDeFicha sistemas;
	private final ItensOrdem itensOrdem;
	private final ItensDnd itensDnd;
	private final AtaquesOrdem ataquesOrdem;
	private final AtaquesDnd ataquesDnd;
	private final HabilidadeFichaRepository habilidades;
	private final TokenService tokens;
	private final Clock relogio;
	private final SecureRandom aleatorio = new SecureRandom();

	public DemoService(UsuarioRepository usuarios, CampanhaRepository campanhas, ParticipacaoRepository participacoes,
			ConviteService convites, FichaRepository fichas, SistemasDeFicha sistemas, ItensOrdem itensOrdem, ItensDnd itensDnd,
			AtaquesOrdem ataquesOrdem, AtaquesDnd ataquesDnd, HabilidadeFichaRepository habilidades, TokenService tokens,
			Clock relogio) {
		this.usuarios = usuarios;
		this.campanhas = campanhas;
		this.participacoes = participacoes;
		this.convites = convites;
		this.fichas = fichas;
		this.sistemas = sistemas;
		this.itensOrdem = itensOrdem;
		this.itensDnd = itensDnd;
		this.ataquesOrdem = ataquesOrdem;
		this.ataquesDnd = ataquesDnd;
		this.habilidades = habilidades;
		this.tokens = tokens;
		this.relogio = relogio;
	}

	/**
	 * Sem código: cria o visitante, a mesa (ele é o mestre) e as duas fichas. Com código: só vale o de uma mesa cujo mestre
	 * é demo e cujo convite está ativo (senão 404, como qualquer convite inválido) e cria um jogador demo nela.
	 */
	@Transactional
	public RespostaDemo entrar(String codigoConvite) {
		if (codigoConvite == null || codigoConvite.isBlank()) {
			return novaMesa();
		}
		Campanha mesa = campanhas.findByCodigoConvite(codigoConvite).filter(Campanha::isConviteAtivo)
				.filter(c -> participacoes.mestreEhDemo(c.getId(), Papel.MESTRE)).orElseThrow(ConviteInvalidoException::new);
		return novoJogador(mesa);
	}

	private RespostaDemo novaMesa() {
		Usuario visitante = novoVisitante();
		Campanha mesa = campanhas.save(new Campanha("Mesa de demonstração",
				"Uma mesa de exemplo, com uma ficha de Ordem Paranormal e uma de D&D 2024. Abra o convite em outra aba para entrar como jogador.",
				convites.novoCodigoLivre(), relogio.instant()));
		participacoes.saveAndFlush(new Participacao(mesa.getId(), visitante.getId(), Papel.MESTRE, relogio.instant()));
		fichaOrdem(visitante, mesa.getId(), investigadora(visitante));
		fichaDnd(visitante, mesa.getId(), guerreiro(visitante));
		log.info("Demo criada usuarioId={} campanhaId={}", visitante.getId(), mesa.getId());
		return RespostaDemo.de(tokens.emitirPar(visitante, UUID.randomUUID()), mesa.getId());
	}

	private RespostaDemo novoJogador(Campanha mesa) {
		Usuario visitante = novoVisitante();
		participacoes.saveAndFlush(new Participacao(mesa.getId(), visitante.getId(), Papel.JOGADOR, relogio.instant()));
		fichaOrdem(visitante, mesa.getId(), combatente(visitante));
		log.info("Jogador demo criado usuarioId={} campanhaId={}", visitante.getId(), mesa.getId());
		return RespostaDemo.de(tokens.emitirPar(visitante, UUID.randomUUID()), mesa.getId());
	}

	/** "Visitante NNNN" e um e-mail único, em minúsculas e de um domínio que nunca recebe correio (RFC 2606). */
	private Usuario novoVisitante() {
		String nome = "Visitante " + (1000 + aleatorio.nextInt(9000));
		return usuarios.saveAndFlush(Usuario.demo("demo-" + UUID.randomUUID() + "@demo.invalid", nome, relogio.instant()));
	}

	// ---------- as fichas de exemplo ----------

	private FichaOrdemRequest investigadora(Usuario dono) {
		Map<AtributoOrdem, Integer> atributos = new EnumMap<>(AtributoOrdem.class);
		atributos.put(AtributoOrdem.AGI, 3);
		atributos.put(AtributoOrdem.FOR, 1);
		atributos.put(AtributoOrdem.INT, 3);
		atributos.put(AtributoOrdem.PRE, 2);
		atributos.put(AtributoOrdem.VIG, 2);
		Map<PericiaOrdem, GrauOrdem> pericias = new EnumMap<>(PericiaOrdem.class);
		pericias.put(PericiaOrdem.INVESTIGACAO, GrauOrdem.VETERANO);
		pericias.put(PericiaOrdem.PERCEPCAO, GrauOrdem.TREINADO);
		pericias.put(PericiaOrdem.PONTARIA, GrauOrdem.TREINADO);
		pericias.put(PericiaOrdem.INTUICAO, GrauOrdem.TREINADO);
		pericias.put(PericiaOrdem.OCULTISMO, GrauOrdem.TREINADO);
		pericias.put(PericiaOrdem.FURTIVIDADE, GrauOrdem.TREINADO);
		return ordem(dono, "Helena Vargas", "F", 34, ClasseOrdem.ESPECIALISTA, "Policial", 35, atributos,
				18, 40, 11, pericias);
	}

	private FichaOrdemRequest combatente(Usuario dono) {
		Map<AtributoOrdem, Integer> atributos = new EnumMap<>(AtributoOrdem.class);
		atributos.put(AtributoOrdem.AGI, 2);
		atributos.put(AtributoOrdem.FOR, 3);
		atributos.put(AtributoOrdem.INT, 1);
		atributos.put(AtributoOrdem.PRE, 1);
		atributos.put(AtributoOrdem.VIG, 3);
		Map<PericiaOrdem, GrauOrdem> pericias = new EnumMap<>(PericiaOrdem.class);
		pericias.put(PericiaOrdem.LUTA, GrauOrdem.TREINADO);
		pericias.put(PericiaOrdem.FORTITUDE, GrauOrdem.TREINADO);
		pericias.put(PericiaOrdem.REFLEXOS, GrauOrdem.TREINADO);
		pericias.put(PericiaOrdem.ATLETISMO, GrauOrdem.TREINADO);
		return ordem(dono, "Bruno Tavares", "M", 29, ClasseOrdem.COMBATENTE, "Militar", 20, atributos,
				26, 20, 8, pericias);
	}

	private FichaOrdemRequest ordem(Usuario dono, String personagem, String genero, int idade, ClasseOrdem classe, String origem,
			int nex, Map<AtributoOrdem, Integer> atributos, int pv, int san, int pe, Map<PericiaOrdem, GrauOrdem> pericias) {
		Map<RecursoOrdem, RecursoValor> recursos = new EnumMap<>(RecursoOrdem.class);
		recursos.put(RecursoOrdem.PV, new RecursoValor(pv, pv));
		recursos.put(RecursoOrdem.SAN, new RecursoValor(san, san));
		recursos.put(RecursoOrdem.PE, new RecursoValor(pe, pe));
		return new FichaOrdemRequest(dono.getNome(), personagem, genero, idade, classe, origem, nex, atributos, recursos, pericias, null);
	}

	private FichaDndRequest guerreiro(Usuario dono) {
		Map<AtributoDnd, Integer> atributos = new EnumMap<>(AtributoDnd.class);
		atributos.put(AtributoDnd.FOR, 16);
		atributos.put(AtributoDnd.DES, 12);
		atributos.put(AtributoDnd.CON, 15);
		atributos.put(AtributoDnd.INT, 10);
		atributos.put(AtributoDnd.SAB, 13);
		atributos.put(AtributoDnd.CAR, 8);
		Map<RecursoDnd, RecursoValor> recursos = new EnumMap<>(RecursoDnd.class);
		recursos.put(RecursoDnd.PV, new RecursoValor(28, 28));
		Map<PericiaDnd, GrauDnd> pericias = new EnumMap<>(PericiaDnd.class);
		pericias.put(PericiaDnd.ATLETISMO, GrauDnd.PROFICIENTE);
		pericias.put(PericiaDnd.INTIMIDACAO, GrauDnd.PROFICIENTE);
		pericias.put(PericiaDnd.PERCEPCAO, GrauDnd.PROFICIENTE);
		pericias.put(PericiaDnd.SOBREVIVENCIA, GrauDnd.PROFICIENTE);
		return new FichaDndRequest(dono.getNome(), "Thorin Punho de Ferro", "M", 120, EdicaoDnd.D2024, "Guerreiro", "Anão", "Soldado",
				3, atributos, recursos, 18, false, EnumSet.of(AtributoDnd.FOR, AtributoDnd.CON), pericias, null, null);
	}

	/** Cria a ficha de Ordem na mesa e preenche a lista de itens, o ataque e a habilidade. */
	private void fichaOrdem(Usuario dono, Long campanhaId, FichaOrdemRequest pedido) {
		FichaOrdem ficha = (FichaOrdem) gravar(dono, campanhaId, pedido);
		boolean investigadora = pedido.classe() == ClasseOrdem.ESPECIALISTA;
		itensOrdem.criar(ficha, new ItemRequest(investigadora ? "Kit de perícia" : "Colete balístico", 1,
				investigadora ? "Lupa, luvas e sacos de evidência." : "Reduz o dano balístico.", investigadora ? 0 : 1, 1, null, null));
		ataquesOrdem.criar(ficha, new AtaqueRequest(investigadora ? "Pistola" : "Facão", 0, investigadora ? "1d12" : "1d6+3",
				investigadora ? "Balístico" : "Corte", investigadora ? "Curto" : "Corpo a corpo",
				investigadora ? PericiaOrdem.PONTARIA : PericiaOrdem.LUTA, null, null, null, null));
		habilidade(ficha, TipoHabilidade.CLASSE, investigadora ? "Eclético" : "Ataque Especial",
				investigadora ? "Gasta PE para usar uma perícia como se fosse treinado nela." : "Gasta PE para somar ao ataque ou ao dano.");
	}

	/** Cria a ficha de D&D na mesa e preenche o item, o ataque e a habilidade. */
	private void fichaDnd(Usuario dono, Long campanhaId, FichaDndRequest pedido) {
		FichaDnd ficha = (FichaDnd) gravar(dono, campanhaId, pedido);
		itensDnd.criar(ficha, new ItemRequest("Espada longa", 1, "Arma marcial versátil.", null, null, new BigDecimal("1.50"), true));
		ataquesDnd.criar(ficha, new AtaqueRequest("Espada longa", 0, "1d8+3", "Cortante", "Corpo a corpo", null, null, null,
				AtributoDnd.FOR, true));
		habilidade(ficha, TipoHabilidade.CLASSE, "Retomar o Fôlego", "Ação bônus: recupera 1d10 + nível de PV. Volta com um descanso curto.");
	}

	/** Valida e converte o pedido pelo sistema da ficha (as regras da API), e a põe na mesa, de posse do visitante. */
	private Ficha gravar(Usuario dono, Long campanhaId, FichaRequest pedido) {
		Ficha ficha = sistemas.de(pedido.sistema()).criar(pedido);
		ficha.setDonoId(dono.getId());
		ficha.setCampanhaId(campanhaId);
		return fichas.saveAndFlush(ficha);
	}

	private void habilidade(Ficha ficha, TipoHabilidade tipo, String nome, String descricao) {
		HabilidadeFicha habilidade = new HabilidadeFicha();
		habilidade.setFicha(ficha);
		habilidade.setTipo(tipo);
		habilidade.setNome(nome);
		habilidade.setDescricao(descricao);
		habilidades.save(habilidade);
	}
}
