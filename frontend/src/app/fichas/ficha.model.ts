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
}

/** Corpo do POST e do PUT: a API não aceita `id` como dado (o da URL é que vale). */
export type FichaEntrada = Omit<Ficha, 'id'>;

export interface TestePericiaResultado {
  pericia: CodigoPericia;
  nome: string;
  valor: number;
  rolagem: number;
  resultado: ResultadoTeste;
}
