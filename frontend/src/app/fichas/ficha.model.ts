/** Códigos das perícias, na ordem do enum `Pericia` da API. */
export type CodigoPericia =
  | 'ACROBACIA'
  | 'ATLETISMO'
  | 'ADESTRAR_ANIMAIS'
  | 'ATUACAO_LABIA'
  | 'CHARME'
  | 'ESQUIVA'
  | 'FURTIVIDADE'
  | 'LUTA_CORPO_A_CORPO'
  | 'LUTA_ARMA_BRANCA'
  | 'INTIMIDACAO'
  | 'INTUICAO'
  | 'INVESTIGACAO'
  | 'ARMAS_DE_FOGO_LEVES'
  | 'ARMAS_DE_FOGO_MEDIAS'
  | 'ARMAS_DE_FOGO_PESADAS'
  | 'PERCEPCAO'
  | 'USAR_BIBLIOTECAS'
  | 'HISTORIA'
  | 'CURA'
  | 'MALANDRAGEM'
  | 'OCULTISMO'
  | 'PSICOLOGIA'
  | 'ARREMESSAR';

export type ResultadoTeste = 'SUCESSO_EXTREMO' | 'SUCESSO' | 'FALHA';

export type ChaveAtributo =
  | 'forca'
  | 'destreza'
  | 'constituicao'
  | 'inteligencia'
  | 'carisma'
  | 'sabedoria'
  | 'tamanho'
  | 'sorte'
  | 'poder';

/** Ficha como a API devolve (contrato do incremento 1.6). */
export interface Ficha {
  id: number;
  jogador: string;
  personagem: string;
  genero: string;
  idade: number | null;
  vidaAtual: number | null;
  vidaMaxima: number | null;
  sanidadeAtual: number | null;
  sanidadeMaxima: number | null;
  forca: number | null;
  destreza: number | null;
  constituicao: number | null;
  inteligencia: number | null;
  carisma: number | null;
  sabedoria: number | null;
  tamanho: number | null;
  sorte: number | null;
  poder: number | null;
  /** Só as perícias preenchidas. */
  pericias: Partial<Record<CodigoPericia, number>>;
  /** Versão do retrato (ms) para o cache do navegador, ou null sem retrato. Só vem na resposta (2.1c). */
  retratoVersao: number | null;
}

/**
 * Corpo do POST e do PUT: a API não aceita `id` como dado (o da URL é que vale) nem
 * `retratoVersao` (o retrato tem rota própria). O JSON estrito da API recusa chaves a mais.
 */
export type FichaEntrada = Omit<Ficha, 'id' | 'retratoVersao'>;

/** Corpo do PUT a partir de uma ficha carregada (ex.: edição rápida de vida e sanidade). */
export function paraEntradaDaFicha(ficha: Ficha): FichaEntrada {
  const { id: _id, retratoVersao: _retratoVersao, pericias, ...resto } = ficha;
  return { ...resto, pericias: { ...pericias } };
}

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
}

export type AlvoTeste = 'PERICIA' | 'ATRIBUTO';

/** Resultado de `POST /fichas/{id}/testes`: cada d20 rolado, o que ficou e o total. */
export interface TesteResultado {
  alvo: AlvoTeste;
  codigo: string;
  nome: string;
  dados: number[];
  mantido: number;
  bonus: number;
  total: number;
}
