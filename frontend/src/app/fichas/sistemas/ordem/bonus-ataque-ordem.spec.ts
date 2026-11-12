import { AtaqueOrdem } from '../../ficha.model';
import { bonusDoAtaqueOrdem } from './catalogo-ordem';

function ataque(parcial: Partial<AtaqueOrdem>): AtaqueOrdem {
  return { id: 1, nome: 'Faca', pericia: 'LUTA', bonus: 0, dano: '1d4', tipoDano: null, alcance: null, margem: 19, multiplicador: 3, ...parcial };
}

describe('bonusDoAtaqueOrdem', () => {
  it('perícia destreinada soma 0', () => {
    expect(bonusDoAtaqueOrdem(ataque({}), {})).toBe(0);
  });

  it('soma o grau da pericia do ataque: treinado +5, veterano +10, expert +15', () => {
    expect(bonusDoAtaqueOrdem(ataque({ pericia: 'LUTA' }), { LUTA: 'TREINADO' })).toBe(5);
    expect(bonusDoAtaqueOrdem(ataque({ pericia: 'LUTA' }), { LUTA: 'VETERANO' })).toBe(10);
    expect(bonusDoAtaqueOrdem(ataque({ pericia: 'PONTARIA' }), { PONTARIA: 'EXPERT' })).toBe(15);
  });

  it('so conta a pericia do ataque: Luta treinada nao ajuda a Pontaria', () => {
    expect(bonusDoAtaqueOrdem(ataque({ pericia: 'PONTARIA' }), { LUTA: 'EXPERT' })).toBe(0);
  });

  it('soma o bonus do ataque, positivo ou negativo', () => {
    expect(bonusDoAtaqueOrdem(ataque({ bonus: 2 }), { LUTA: 'TREINADO' })).toBe(7);
    expect(bonusDoAtaqueOrdem(ataque({ bonus: -3 }), {})).toBe(-3);
  });
});
