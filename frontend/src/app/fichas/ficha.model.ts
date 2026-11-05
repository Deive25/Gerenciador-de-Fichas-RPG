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
}

/** O que `GET /fichas/{id}` devolve: o campo `sistema` diz qual das fichas é. */
export type Ficha = FichaOrdem | FichaDnd;

export type AlvoTeste = 'PERICIA' | 'ATRIBUTO' | 'SALVAGUARDA';

/** Vantagem rola 2d20 e fica o maior; desvantagem, o menor (só D&D). */
export type ModoTeste = 'NORMAL' | 'VANTAGEM' | 'DESVANTAGEM';

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
 * `retratoVersao` (o retrato tem rota própria). O JSON estrito da API recusa chaves a mais.
 */
export type FichaOrdemEntrada = Omit<FichaOrdem, 'id' | 'retratoVersao'>;
export type FichaDndEntrada = Omit<FichaDnd, 'id' | 'retratoVersao'>;
export type FichaEntrada = FichaOrdemEntrada | FichaDndEntrada;

/** Corpo do PUT a partir de uma ficha carregada, sem as chaves que a API recusa. */
export function paraEntradaDaFicha(ficha: FichaOrdem): FichaOrdemEntrada;
export function paraEntradaDaFicha(ficha: FichaDnd): FichaDndEntrada;
export function paraEntradaDaFicha(ficha: Ficha): FichaEntrada;
export function paraEntradaDaFicha(ficha: Ficha): FichaEntrada {
  if (ficha.sistema === 'DND') {
    const { id: _id, retratoVersao: _retratoVersao, atributos, recursos, salvaguardas, pericias, ...resto } = ficha;
    return {
      ...resto,
      atributos: { ...atributos },
      recursos: { PV: { ...recursos.PV }, PV_TEMP: { ...recursos.PV_TEMP } },
      salvaguardas: [...salvaguardas],
      pericias: { ...pericias },
    };
  }
  const { id: _id, retratoVersao: _retratoVersao, atributos, recursos, pericias, ...resto } = ficha;
  return {
    ...resto,
    atributos: { ...atributos },
    recursos: { PV: { ...recursos.PV }, SAN: { ...recursos.SAN }, PE: { ...recursos.PE } },
    pericias: { ...pericias },
  };
}
