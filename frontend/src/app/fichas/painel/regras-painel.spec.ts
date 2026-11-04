import { Ficha } from '../ficha.model';
import { contar, estaFerido, filtrarFichas, normalizar, temSanidadeBaixa } from './regras-painel';

function ficha(parcial: Partial<Ficha>): Ficha {
  return {
    id: 1, jogador: 'Arthur', personagem: 'Lia', genero: 'F', idade: null,
    vidaAtual: 12, vidaMaxima: 12, sanidadeAtual: 60, sanidadeMaxima: 60,
    forca: null, destreza: null, constituicao: null, inteligencia: null, carisma: null,
    sabedoria: null, tamanho: null, sorte: null, poder: null, pericias: {}, retratoVersao: null,
    ...parcial,
  };
}

describe('estaFerido', () => {
  it('vale com a vida atual ate a metade da maxima, inclusive', () => {
    expect(estaFerido({ vidaAtual: 6, vidaMaxima: 12 })).toBe(true);
    expect(estaFerido({ vidaAtual: 0, vidaMaxima: 12 })).toBe(true);
    expect(estaFerido({ vidaAtual: 7, vidaMaxima: 12 })).toBe(false);
    expect(estaFerido({ vidaAtual: 15, vidaMaxima: 12 })).toBe(false);
  });

  it('sem valor atual, sem maximo ou com maximo 0 nao conta como ferido', () => {
    expect(estaFerido({ vidaAtual: null, vidaMaxima: 12 })).toBe(false);
    expect(estaFerido({ vidaAtual: 5, vidaMaxima: null })).toBe(false);
    expect(estaFerido({ vidaAtual: 0, vidaMaxima: 0 })).toBe(false);
  });
});

describe('temSanidadeBaixa', () => {
  it('vale com a sanidade atual ate a metade da maxima, inclusive', () => {
    expect(temSanidadeBaixa({ sanidadeAtual: 30, sanidadeMaxima: 60 })).toBe(true);
    expect(temSanidadeBaixa({ sanidadeAtual: 31, sanidadeMaxima: 60 })).toBe(false);
  });

  it('sem valor atual nao conta como sanidade baixa', () => {
    expect(temSanidadeBaixa({ sanidadeAtual: null, sanidadeMaxima: 60 })).toBe(false);
  });
});

describe('normalizar', () => {
  it('tira acentos, espacos das pontas e maiusculas', () => {
    expect(normalizar('  Lía FONTAINE ')).toBe('lia fontaine');
  });
});

describe('filtrarFichas', () => {
  const lia = ficha({ id: 1, personagem: 'Lía Fontaine', jogador: 'Bia', vidaAtual: 11, vidaMaxima: 11 });
  const hale = ficha({ id: 2, personagem: 'Prof. Hale', jogador: 'Caio', vidaAtual: 4, vidaMaxima: 10 });
  const jack = ficha({ id: 3, personagem: 'Jack Moreau', jogador: 'Arthur', vidaAtual: 5, vidaMaxima: 12 });
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
    const louca = ficha({ id: 4, sanidadeAtual: 21, sanidadeMaxima: 55 });
    expect(filtrarFichas([lia, louca], '', 'sanidade-baixa')).toEqual([louca]);
  });
});

describe('contar', () => {
  it('conta total, feridos e sanidade baixa', () => {
    const fichas = [
      ficha({ id: 1, vidaAtual: 4, vidaMaxima: 10 }),
      ficha({ id: 2, sanidadeAtual: 21, sanidadeMaxima: 55 }),
      ficha({ id: 3, vidaAtual: null, vidaMaxima: null, sanidadeAtual: null, sanidadeMaxima: null }),
    ];
    expect(contar(fichas)).toEqual({ total: 3, feridos: 1, sanidadeBaixa: 1 });
  });
});
