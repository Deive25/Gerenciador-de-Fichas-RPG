import { bonusPericia, bonusSalvaguarda, comSinal, modificador, proficiencia } from './regras-dnd';

describe('regras de D&D', () => {
  it('modificador arredonda para baixo, inclusive nos negativos', () => {
    expect(modificador(1)).toBe(-5);
    expect(modificador(8)).toBe(-1);
    expect(modificador(9)).toBe(-1);
    expect(modificador(10)).toBe(0);
    expect(modificador(11)).toBe(0);
    expect(modificador(12)).toBe(1);
    expect(modificador(15)).toBe(2);
    expect(modificador(20)).toBe(5);
    expect(modificador(30)).toBe(10);
  });

  it('proficiencia sobe a cada 4 niveis', () => {
    expect(proficiencia(1)).toBe(2);
    expect(proficiencia(4)).toBe(2);
    expect(proficiencia(5)).toBe(3);
    expect(proficiencia(8)).toBe(3);
    expect(proficiencia(9)).toBe(4);
    expect(proficiencia(13)).toBe(5);
    expect(proficiencia(17)).toBe(6);
    expect(proficiencia(20)).toBe(6);
  });

  it('bonus de pericia: sem grau, proficiente e especialista', () => {
    expect(bonusPericia(14, 5)).toBe(2);
    expect(bonusPericia(14, 5, undefined)).toBe(2);
    expect(bonusPericia(14, 5, 'PROFICIENTE')).toBe(5);
    expect(bonusPericia(14, 5, 'ESPECIALISTA')).toBe(8);
    expect(bonusPericia(9, 1, 'PROFICIENTE')).toBe(1);
    expect(bonusPericia(9, 1)).toBe(-1);
  });

  it('bonus de salvaguarda soma a proficiencia so se proficiente', () => {
    expect(bonusSalvaguarda(14, 5, false)).toBe(2);
    expect(bonusSalvaguarda(14, 5, true)).toBe(5);
    expect(bonusSalvaguarda(7, 1, true)).toBe(0);
  });

  it('comSinal usa o sinal de menos (U+2212) e +0 para zero', () => {
    expect(comSinal(2)).toBe('+2');
    expect(comSinal(0)).toBe('+0');
    expect(comSinal(-1)).toBe('−1');
    expect(comSinal(-1)).not.toBe('-1');
  });
});
