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

/**
 * Corpo do POST e do PUT de Ordem: a API não aceita `id` como dado (o da URL é que vale) nem
 * `retratoVersao` (o retrato tem rota própria). O JSON estrito da API recusa chaves a mais.
 */
export type FichaOrdemEntrada = Omit<FichaOrdem, 'id' | 'retratoVersao'>;

/** Corpo do PUT a partir de uma ficha carregada, sem as chaves que a API recusa. */
export function paraEntradaDaFicha(ficha: FichaOrdem): FichaOrdemEntrada {
  const { id: _id, retratoVersao: _retratoVersao, atributos, recursos, pericias, ...resto } = ficha;
  return {
    ...resto,
    atributos: { ...atributos },
    recursos: { PV: { ...recursos.PV }, SAN: { ...recursos.SAN }, PE: { ...recursos.PE } },
    pericias: { ...pericias },
  };
}
