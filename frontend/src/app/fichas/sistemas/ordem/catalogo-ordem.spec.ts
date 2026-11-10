import { ItemOrdem } from '../../ficha.model';
import {
  ATRIBUTOS_ORDEM,
  PERICIAS_ORDEM,
  ROTULO_CATEGORIA,
  bonusDoGrau,
  defesa,
  espacosUsados,
  limiteDeCarga,
  rotuloClasse,
  rotuloEspacos,
} from './catalogo-ordem';

describe('catalogo de Ordem', () => {
  it('tem os 5 atributos, cada um com descricao', () => {
    expect(ATRIBUTOS_ORDEM.map((a) => a.codigo)).toEqual(['AGI', 'FOR', 'INT', 'PRE', 'VIG']);
    expect(ATRIBUTOS_ORDEM.map((a) => a.nome)).toEqual(['Agilidade', 'Força', 'Intelecto', 'Presença', 'Vigor']);
    for (const a of ATRIBUTOS_ORDEM) {
      expect(a.descricao.length).toBeGreaterThan(10);
    }
  });

  it('tem as 28 pericias, sem repeticao e todas com descricao', () => {
    expect(PERICIAS_ORDEM.length).toBe(28);
    expect(new Set(PERICIAS_ORDEM.map((p) => p.codigo)).size).toBe(28);
    for (const p of PERICIAS_ORDEM) {
      expect(p.descricao.length).toBeGreaterThan(10);
    }
  });

  it('a contagem de pericias por atributo e a do backend', () => {
    const contagem: Record<string, number> = {};
    for (const p of PERICIAS_ORDEM) {
      contagem[p.atributo] = (contagem[p.atributo] ?? 0) + 1;
    }
    expect(contagem).toEqual({ AGI: 7, FOR: 2, INT: 9, PRE: 9, VIG: 1 });
  });

  it('nomes e atributos de algumas pericias conferem com o backend', () => {
    const por = (c: string) => PERICIAS_ORDEM.find((p) => p.codigo === c);
    expect(por('INVESTIGACAO')).toMatchObject({ nome: 'Investigação', atributo: 'INT' });
    expect(por('FORTITUDE')).toMatchObject({ nome: 'Fortitude', atributo: 'VIG' });
    expect(por('LUTA')).toMatchObject({ nome: 'Luta', atributo: 'FOR' });
  });

  it('bonus por grau: destreinado +0, treinado +5, veterano +10, expert +15', () => {
    expect(bonusDoGrau(undefined)).toBe(0);
    expect(bonusDoGrau('TREINADO')).toBe(5);
    expect(bonusDoGrau('VETERANO')).toBe(10);
    expect(bonusDoGrau('EXPERT')).toBe(15);
  });

  it('Defesa = 10 + Agilidade', () => {
    expect(defesa(0)).toBe(10);
    expect(defesa(3)).toBe(13);
  });

  it('classe nula aparece como A definir', () => {
    expect(rotuloClasse(null)).toBe('A definir');
    expect(rotuloClasse('OCULTISTA')).toBe('Ocultista');
  });
});

describe('carga de Ordem', () => {
  const item = (espacos: number, quantidade: number): ItemOrdem => ({
    id: 1, nome: 'Item', categoria: 0, espacos, quantidade, descricao: null,
  });

  it('rotula as categorias de 0 a IV', () => {
    expect(ROTULO_CATEGORIA).toEqual(['0', 'I', 'II', 'III', 'IV']);
  });

  it('o limite e 5 x FOR, e com FOR 0 e 2', () => {
    expect(limiteDeCarga(0)).toBe(2);
    expect(limiteDeCarga(1)).toBe(5);
    expect(limiteDeCarga(5)).toBe(25);
  });

  it('os espacos usados somam espacos x quantidade', () => {
    expect(espacosUsados([])).toBe(0);
    expect(espacosUsados([item(1, 3), item(2, 1)])).toBe(5);
    expect(espacosUsados([item(0, 4), item(2, 1)])).toBe(2);
  });

  it('rotula espacos no singular so para 1', () => {
    expect(rotuloEspacos(1)).toBe('1 espaço');
    expect(rotuloEspacos(0)).toBe('0 espaços');
    expect(rotuloEspacos(2)).toBe('2 espaços');
  });
});
