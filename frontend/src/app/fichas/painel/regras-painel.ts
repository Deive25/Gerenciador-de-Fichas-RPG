import { Ficha } from '../ficha.model';

/** Filtros do painel (spec do redesign, §3.2). */
export type FiltroPainel = 'todos' | 'feridos' | 'sanidade-baixa';

export interface ContagemPainel {
  total: number;
  feridos: number;
  sanidadeBaixa: number;
}

/**
 * "Metade ou menos do máximo". Sem valor atual, sem máximo ou com máximo ≤ 0 não dá para
 * afirmar nada, então a ficha fica fora do filtro (e a barra dela mostra "—").
 */
function ateMetade(atual: number | null, maximo: number | null): boolean {
  return atual !== null && maximo !== null && maximo > 0 && atual <= maximo / 2;
}

export function estaFerido(f: Pick<Ficha, 'vidaAtual' | 'vidaMaxima'>): boolean {
  return ateMetade(f.vidaAtual, f.vidaMaxima);
}

export function temSanidadeBaixa(f: Pick<Ficha, 'sanidadeAtual' | 'sanidadeMaxima'>): boolean {
  return ateMetade(f.sanidadeAtual, f.sanidadeMaxima);
}

/** Minúsculas e sem acentos: "Lía" e "LIA" viram "lia". */
export function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
}

const PASSA_NO_FILTRO: Record<FiltroPainel, (f: Ficha) => boolean> = {
  todos: () => true,
  feridos: estaFerido,
  'sanidade-baixa': temSanidadeBaixa,
};

/** Busca (personagem ou jogador) E filtro, mantendo a ordem recebida. */
export function filtrarFichas(fichas: readonly Ficha[], busca: string, filtro: FiltroPainel): Ficha[] {
  const termo = normalizar(busca);
  return fichas.filter(
    (f) =>
      PASSA_NO_FILTRO[filtro](f) &&
      (termo === '' || normalizar(f.personagem).includes(termo) || normalizar(f.jogador).includes(termo)),
  );
}

export function contar(fichas: readonly Ficha[]): ContagemPainel {
  return {
    total: fichas.length,
    feridos: fichas.filter(estaFerido).length,
    sanidadeBaixa: fichas.filter(temSanidadeBaixa).length,
  };
}
