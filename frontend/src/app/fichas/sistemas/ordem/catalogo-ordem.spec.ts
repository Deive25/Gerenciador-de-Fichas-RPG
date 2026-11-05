import { ATRIBUTOS_ORDEM, PERICIAS_ORDEM, bonusDoGrau, defesa, rotuloClasse } from './catalogo-ordem';

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
