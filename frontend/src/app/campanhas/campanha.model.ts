/** Papel de quem está logado numa campanha. */
export type Papel = 'MESTRE' | 'JOGADOR';

export const NOMES_PAPEL: Record<Papel, string> = { MESTRE: 'Mestre', JOGADOR: 'Jogador' };

/** O `{id, nome}` que a API usa para dono, campanha e mestre. */
export interface Referencia {
  id: number;
  nome: string;
}

/** O que `GET /campanhas` devolve por campanha; `fichas` conta só as que a pessoa vê na mesa. */
export interface CampanhaResumo {
  id: number;
  nome: string;
  papel: Papel;
  mestre: Referencia;
  fichas: number;
}

/** O convite de uma campanha: o código e se o link está valendo. */
export interface Convite {
  codigo: string;
  ativo: boolean;
}

/** `GET /campanhas/{id}`: `convite` só vem para o mestre (o jogador recebe null). */
export interface Campanha {
  id: number;
  nome: string;
  descricao: string | null;
  meuPapel: Papel;
  mestre: Referencia;
  convite: Convite | null;
}

/** Corpo do POST e do PUT de uma campanha (o PUT substitui nome e descrição). */
export interface CampanhaEntrada {
  nome: string;
  descricao: string | null;
}

export interface Membro {
  usuarioId: number;
  nome: string;
  papel: Papel;
  entrouEm: string;
}

/** Prévia pública do convite (`GET /convites/{codigo}`): só nomes. */
export interface PreviaConvite {
  campanhaId: number;
  campanha: string;
  mestre: string;
}
