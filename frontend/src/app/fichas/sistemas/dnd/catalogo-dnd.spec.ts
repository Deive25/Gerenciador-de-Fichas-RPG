import {
  ATRIBUTOS_DND,
  NOMES_EDICAO,
  NOME_GRAU_DND,
  PERICIAS_DND,
  SELO_GRAU_DND,
  rotuloEspecie,
} from './catalogo-dnd';

describe('catalogo de D&D', () => {
  it('tem os 6 atributos, cada um com descricao', () => {
    expect(ATRIBUTOS_DND.map((a) => a.codigo)).toEqual(['FOR', 'DES', 'CON', 'INT', 'SAB', 'CAR']);
    expect(ATRIBUTOS_DND.map((a) => a.nome)).toEqual([
      'Força', 'Destreza', 'Constituição', 'Inteligência', 'Sabedoria', 'Carisma',
    ]);
    for (const a of ATRIBUTOS_DND) {
      expect(a.descricao.length).toBeGreaterThan(10);
    }
  });

  it('tem as 18 pericias, sem repeticao e todas com descricao', () => {
    expect(PERICIAS_DND.length).toBe(18);
    expect(new Set(PERICIAS_DND.map((p) => p.codigo)).size).toBe(18);
    for (const p of PERICIAS_DND) {
      expect(p.descricao.length).toBeGreaterThan(10);
    }
  });

  it('a contagem de pericias por atributo e a do backend', () => {
    const contagem: Record<string, number> = {};
    for (const p of PERICIAS_DND) {
      contagem[p.atributo] = (contagem[p.atributo] ?? 0) + 1;
    }
    expect(contagem).toEqual({ FOR: 1, DES: 3, INT: 5, SAB: 5, CAR: 4 });
  });

  it('nomes e atributos de algumas pericias conferem com o backend', () => {
    const por = (c: string) => PERICIAS_DND.find((p) => p.codigo === c);
    expect(por('PRESTIDIGITACAO')).toMatchObject({ nome: 'Prestidigitação', atributo: 'DES' });
    expect(por('LIDAR_COM_ANIMAIS')).toMatchObject({ nome: 'Lidar com Animais', atributo: 'SAB' });
    expect(por('ATLETISMO')).toMatchObject({ nome: 'Atletismo', atributo: 'FOR' });
    expect(por('PERSUASAO')).toMatchObject({ nome: 'Persuasão', atributo: 'CAR' });
  });

  it('nomes das edicoes e rotulo do campo especie', () => {
    expect(NOMES_EDICAO).toEqual({ D2014: 'D&D 5e (2014)', D2024: 'D&D 2024' });
    expect(rotuloEspecie('D2014')).toBe('Raça');
    expect(rotuloEspecie('D2024')).toBe('Espécie');
  });

  it('selos e nomes dos graus', () => {
    expect(SELO_GRAU_DND).toEqual({ PROFICIENTE: 'P', ESPECIALISTA: 'E' });
    expect(NOME_GRAU_DND).toEqual({ PROFICIENTE: 'Proficiente', ESPECIALISTA: 'Especialista' });
  });
});
