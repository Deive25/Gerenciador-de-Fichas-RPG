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

export interface TestePericiaResultado {
  pericia: CodigoPericia;
  nome: string;
  valor: number;
  rolagem: number;
  resultado: ResultadoTeste;
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
