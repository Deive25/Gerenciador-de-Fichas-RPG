/** Sistemas de RPG que a API conhece (2.1d). */
export type Sistema = 'ORDEM_PARANORMAL' | 'DND';

export const NOMES_SISTEMA: Record<Sistema, string> = {
  ORDEM_PARANORMAL: 'Ordem Paranormal',
  DND: 'D&D',
};

/** Um recurso (PV, SAN, PE...) no resumo da listagem; atual e máximo podem faltar. */
export interface RecursoResumo {
  codigo: string;
  atual: number | null;
  maximo: number | null;
}

/** O que `GET /fichas` devolve por ficha: o bastante para o cartão do painel. */
export interface FichaResumo {
  id: number;
  sistema: Sistema;
  jogador: string;
  personagem: string;
  idade: number | null;
  retratoVersao: number | null;
  recursos: RecursoResumo[];
}

// ---- Descrição e habilidades (2.1g), comuns aos dois sistemas ----

/** Texto livre da ficha: cada campo pode faltar (a API guarda texto vazio como null). */
export interface DescricaoFicha {
  aparencia: string | null;
  historia: string | null;
  anotacoes: string | null;
}

/** Os tipos de cada sistema são um subconjunto destes; a API recusa (400) o de outro sistema. */
export type TipoHabilidade =
  | 'CLASSE'
  | 'TRILHA'
  | 'ORIGEM'
  | 'PARANORMAL'
  | 'SUBCLASSE'
  | 'ESPECIE'
  | 'ANTECEDENTE'
  | 'TALENTO'
  | 'OUTRA';

export interface Habilidade {
  id: number;
  nome: string;
  tipo: TipoHabilidade;
  descricao: string | null;
}

/** Corpo do POST e do PUT de uma habilidade: o id vem da URL. */
export type HabilidadeEntrada = Omit<Habilidade, 'id'>;

// ---- Inventário (2.1h) ----

/** Item de Ordem: categoria de 0 a 4 (mostrada como 0, I, II, III, IV); espaços e quantidade são por unidade. */
export interface ItemOrdem {
  id: number;
  nome: string;
  categoria: number;
  espacos: number;
  quantidade: number;
  descricao: string | null;
}

/** Item de D&D: peso em kg, por unidade. */
export interface ItemDnd {
  id: number;
  nome: string;
  quantidade: number;
  peso: number;
  equipado: boolean;
  descricao: string | null;
}

/** Corpo do POST e do PUT de um item: o id vem da URL. */
export type ItemOrdemEntrada = Omit<ItemOrdem, 'id'>;
export type ItemDndEntrada = Omit<ItemDnd, 'id'>;

/** Moedas de D&D: cobre, prata, electro, ouro e platina. */
export interface Moedas {
  pc: number;
  pp: number;
  pe: number;
  po: number;
  pl: number;
}

// ---- Ordem Paranormal (2.1d) ----

export type AtributoOrdem = 'AGI' | 'FOR' | 'INT' | 'PRE' | 'VIG';

export type ClasseOrdem = 'COMBATENTE' | 'ESPECIALISTA' | 'OCULTISTA';

/** Grau de treinamento; perícia sem grau é destreinada (+0). */
export type GrauOrdem = 'TREINADO' | 'VETERANO' | 'EXPERT';

export type PericiaOrdem =
  | 'ACROBACIA'
  | 'ADESTRAMENTO'
  | 'ARTES'
  | 'ATLETISMO'
  | 'ATUALIDADES'
  | 'CIENCIAS'
  | 'CRIME'
  | 'DIPLOMACIA'
  | 'ENGANACAO'
  | 'FORTITUDE'
  | 'FURTIVIDADE'
  | 'INICIATIVA'
  | 'INTIMIDACAO'
  | 'INTUICAO'
  | 'INVESTIGACAO'
  | 'LUTA'
  | 'MEDICINA'
  | 'OCULTISMO'
  | 'PERCEPCAO'
  | 'PILOTAGEM'
  | 'PONTARIA'
  | 'PROFISSAO'
  | 'REFLEXOS'
  | 'RELIGIAO'
  | 'SOBREVIVENCIA'
  | 'TATICA'
  | 'TECNOLOGIA'
  | 'VONTADE';

/** Um recurso da ficha: atual e máximo podem faltar (ex.: PE das fichas convertidas). */
export interface RecursoValor {
  atual: number | null;
  maximo: number | null;
}

export type CodigoRecurso = 'PV' | 'SAN' | 'PE';

/** Ficha de Ordem Paranormal como `GET /fichas/{id}` devolve. */
export interface FichaOrdem {
  id: number;
  sistema: 'ORDEM_PARANORMAL';
  jogador: string;
  personagem: string;
  genero: string;
  idade: number | null;
  /** Versão do retrato (ms) para o cache do navegador, ou null sem retrato. */
  retratoVersao: number | null;
  classe: ClasseOrdem | null;
  origem: string | null;
  nex: number;
  atributos: Record<AtributoOrdem, number>;
  recursos: Record<CodigoRecurso, RecursoValor>;
  /** Só as perícias treinadas. */
  pericias: Partial<Record<PericiaOrdem, GrauOrdem>>;
  descricao: DescricaoFicha;
  habilidades: Habilidade[];
  itens: ItemOrdem[];
}

// ---- D&D 5e (2014) e D&D 2024 (2.1e) ----

export type EdicaoDnd = 'D2014' | 'D2024';

export type AtributoDnd = 'FOR' | 'DES' | 'CON' | 'INT' | 'SAB' | 'CAR';

/** Proficiente soma a proficiência; especialista soma o dobro. Perícia sem grau não tem proficiência. */
export type GrauDnd = 'PROFICIENTE' | 'ESPECIALISTA';

export type PericiaDnd =
  | 'ACROBACIA'
  | 'ARCANISMO'
  | 'ATLETISMO'
  | 'ATUACAO'
  | 'ENGANACAO'
  | 'FURTIVIDADE'
  | 'HISTORIA'
  | 'INTIMIDACAO'
  | 'INTUICAO'
  | 'INVESTIGACAO'
  | 'LIDAR_COM_ANIMAIS'
  | 'MEDICINA'
  | 'NATUREZA'
  | 'PERCEPCAO'
  | 'PERSUASAO'
  | 'PRESTIDIGITACAO'
  | 'RELIGIAO'
  | 'SOBREVIVENCIA';

/** Ficha de D&D como `GET /fichas/{id}` devolve; as duas edições usam a mesma forma. */
export interface FichaDnd {
  id: number;
  sistema: 'DND';
  jogador: string;
  personagem: string;
  genero: string;
  idade: number | null;
  retratoVersao: number | null;
  edicao: EdicaoDnd;
  classe: string | null;
  /** "Raça" em D2014, "Espécie" em D2024. */
  especie: string | null;
  antecedente: string | null;
  nivel: number;
  atributos: Record<AtributoDnd, number>;
  /** PV_TEMP só usa `atual`; o máximo vem sempre null. */
  recursos: Record<'PV' | 'PV_TEMP', RecursoValor>;
  ca: number | null;
  inspiracao: boolean;
  /** Atributos em que a ficha é proficiente na salvaguarda. */
  salvaguardas: AtributoDnd[];
  /** Só as perícias com proficiência ou especialização. */
  pericias: Partial<Record<PericiaDnd, GrauDnd>>;
  descricao: DescricaoFicha;
  habilidades: Habilidade[];
  itens: ItemDnd[];
  moedas: Moedas;
}

/** O que `GET /fichas/{id}` devolve: o campo `sistema` diz qual das fichas é. */
export type Ficha = FichaOrdem | FichaDnd;

export type AlvoTeste = 'PERICIA' | 'ATRIBUTO' | 'SALVAGUARDA';

/** Vantagem rola 2d20 e fica o maior; desvantagem, o menor (só D&D). */
export type ModoTeste = 'NORMAL' | 'VANTAGEM' | 'DESVANTAGEM';

/** O que a ficha pede ao rolar: o modo só existe em D&D e, sem ele, é Normal. */
export type PedidoTeste = { alvo: AlvoTeste; codigo: string; modo?: ModoTeste };

/** Resultado de `POST /fichas/{id}/testes`: cada d20 rolado, o que ficou e o total. */
export interface TesteResultado {
  alvo: AlvoTeste;
  codigo: string;
  nome: string;
  dados: number[];
  mantido: number;
  /** Pode ser negativo em D&D. */
  bonus: number;
  total: number;
}

/**
 * Corpo do POST e do PUT: a API não aceita `id` como dado (o da URL é que vale) nem
 * `retratoVersao` (o retrato tem rota própria) nem `descricao`, `habilidades`, `itens` e `moedas` (rotas próprias, ADR 0018).
 * O JSON estrito da API recusa chaves a mais.
 */
type SoDaLeitura = 'id' | 'retratoVersao' | 'descricao' | 'habilidades' | 'itens' | 'moedas';
export type FichaOrdemEntrada = Omit<FichaOrdem, SoDaLeitura>;
export type FichaDndEntrada = Omit<FichaDnd, SoDaLeitura>;
export type FichaEntrada = FichaOrdemEntrada | FichaDndEntrada;

/** Corpo do PUT a partir de uma ficha carregada, sem as chaves que a API recusa. */
export function paraEntradaDaFicha(ficha: FichaOrdem): FichaOrdemEntrada;
export function paraEntradaDaFicha(ficha: FichaDnd): FichaDndEntrada;
export function paraEntradaDaFicha(ficha: Ficha): FichaEntrada;
export function paraEntradaDaFicha(ficha: Ficha): FichaEntrada {
  if (ficha.sistema === 'DND') {
    const {
      id: _id,
      retratoVersao: _retratoVersao,
      descricao: _descricao,
      habilidades: _habilidades,
      itens: _itens,
      moedas: _moedas,
      atributos, recursos, salvaguardas, pericias, ...resto } = ficha;
    return {
      ...resto,
      atributos: { ...atributos },
      recursos: { PV: { ...recursos.PV }, PV_TEMP: { ...recursos.PV_TEMP } },
      salvaguardas: [...salvaguardas],
      pericias: { ...pericias },
    };
  }
  const {
      id: _id,
      retratoVersao: _retratoVersao,
      descricao: _descricao,
      habilidades: _habilidades,
      itens: _itens,
      atributos, recursos, pericias, ...resto } = ficha;
  return {
    ...resto,
    atributos: { ...atributos },
    recursos: { PV: { ...recursos.PV }, SAN: { ...recursos.SAN }, PE: { ...recursos.PE } },
    pericias: { ...pericias },
  };
}
