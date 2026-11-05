import { FichaResumo, RecursoResumo } from '../ficha.model';
import { contar, estaFerido, filtrarFichas, normalizar, temSanidadeBaixa } from './regras-painel';

type Par = [number | null, number | null];

function rec(codigo: string, [atual, maximo]: Par): RecursoResumo {
  return { codigo, atual, maximo };
}

/** Ficha de Ordem com PV, SAN e PE; `vida` e `sanidade` trocam os pares [atual, máximo]. */
function ficha(parcial: Partial<FichaResumo> & { vida?: Par; sanidade?: Par } = {}): FichaResumo {
  const { vida = [12, 12], sanidade = [60, 60], ...resto } = parcial;
  return {
    id: 1, sistema: 'ORDEM_PARANORMAL', jogador: 'Arthur', personagem: 'Lia', idade: null, retratoVersao: null,
    recursos: [rec('PV', vida), rec('SAN', sanidade), rec('PE', [5, 5])],
    ...resto,
  };
}

describe('estaFerido', () => {
  it('vale com a vida atual ate a metade da maxima, inclusive', () => {
    expect(estaFerido(ficha({ vida: [6, 12] }))).toBe(true);
    expect(estaFerido(ficha({ vida: [0, 12] }))).toBe(true);
    expect(estaFerido(ficha({ vida: [7, 12] }))).toBe(false);
    expect(estaFerido(ficha({ vida: [15, 12] }))).toBe(false);
  });

  it('sem valor atual, sem maximo ou com maximo 0 nao conta como ferido', () => {
    expect(estaFerido(ficha({ vida: [null, 12] }))).toBe(false);
    expect(estaFerido(ficha({ vida: [5, null] }))).toBe(false);
    expect(estaFerido(ficha({ vida: [0, 0] }))).toBe(false);
  });

  it('sem o recurso PV nao conta como ferido', () => {
    expect(estaFerido(ficha({ recursos: [rec('SAN', [1, 60])] }))).toBe(false);
  });
});

describe('temSanidadeBaixa', () => {
  it('vale com a sanidade atual ate a metade da maxima, inclusive', () => {
    expect(temSanidadeBaixa(ficha({ sanidade: [30, 60] }))).toBe(true);
    expect(temSanidadeBaixa(ficha({ sanidade: [31, 60] }))).toBe(false);
  });

  it('sem valor atual nao conta como sanidade baixa', () => {
    expect(temSanidadeBaixa(ficha({ sanidade: [null, 60] }))).toBe(false);
  });

  it('ficha sem o recurso SAN nao entra em Sanidade baixa nem na contagem', () => {
    const semSan = ficha({ id: 9, recursos: [rec('PV', [12, 12])] });
    expect(temSanidadeBaixa(semSan)).toBe(false);
    expect(filtrarFichas([semSan], '', 'sanidade-baixa')).toEqual([]);
    expect(contar([semSan]).sanidadeBaixa).toBe(0);
  });
});

describe('normalizar', () => {
  it('tira acentos, espacos das pontas e maiusculas', () => {
    expect(normalizar('  Lía FONTAINE ')).toBe('lia fontaine');
  });
});

describe('filtrarFichas', () => {
  const lia = ficha({ id: 1, personagem: 'Lía Fontaine', jogador: 'Bia', vida: [11, 11] });
  const hale = ficha({ id: 2, personagem: 'Prof. Hale', jogador: 'Caio', vida: [4, 10] });
  const jack = ficha({ id: 3, personagem: 'Jack Moreau', jogador: 'Arthur', vida: [5, 12] });
  const todas = [lia, hale, jack];

  it('busca pelo personagem sem diferenciar acento e maiusculas', () => {
    expect(filtrarFichas(todas, 'LIA', 'todos')).toEqual([lia]);
  });

  it('busca tambem pelo jogador', () => {
    expect(filtrarFichas(todas, 'arth', 'todos')).toEqual([jack]);
  });

  it('busca vazia devolve todas, na ordem recebida', () => {
    expect(filtrarFichas(todas, '', 'todos')).toEqual([lia, hale, jack]);
  });

  it('filtra os feridos', () => {
    expect(filtrarFichas(todas, '', 'feridos')).toEqual([hale, jack]);
  });

  it('combina busca e filtro', () => {
    expect(filtrarFichas(todas, 'hale', 'feridos')).toEqual([hale]);
    expect(filtrarFichas(todas, 'lia', 'feridos')).toEqual([]);
  });

  it('filtra sanidade baixa', () => {
    const louca = ficha({ id: 4, sanidade: [21, 55] });
    expect(filtrarFichas([lia, louca], '', 'sanidade-baixa')).toEqual([louca]);
  });
});

describe('contar', () => {
  it('conta total, feridos e sanidade baixa', () => {
    const fichas = [
      ficha({ id: 1, vida: [4, 10] }),
      ficha({ id: 2, sanidade: [21, 55] }),
      ficha({ id: 3, vida: [null, null], sanidade: [null, null] }),
    ];
    expect(contar(fichas)).toEqual({ total: 3, feridos: 1, sanidadeBaixa: 1 });
  });
});
