import { contaComBonus } from './formato-rolagem';

describe('contaComBonus', () => {
  it('bonus positivo soma', () => {
    expect(contaComBonus(19, 5)).toBe('19 + 5');
  });

  it('bonus negativo usa o sinal de menos (U+2212)', () => {
    expect(contaComBonus(7, -1)).toBe('7 − 1');
  });

  it('sem bonus fica so a base', () => {
    expect(contaComBonus(12, 0)).toBe('12');
  });

  it('a base pode ser texto (a soma dos dados do dano)', () => {
    expect(contaComBonus('6 + 2', 3)).toBe('6 + 2 + 3');
  });
});
