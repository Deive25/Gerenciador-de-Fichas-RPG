import { proporcao } from './proporcao';

describe('proporcao', () => {
  it('da o percentual inteiro do atual sobre o maximo', () => {
    expect(proporcao(9, 12)).toBe(75);
  });

  it('limita entre 0 e 100 quando o atual passa do maximo ou fica negativo', () => {
    expect(proporcao(15, 12)).toBe(100);
    expect(proporcao(-3, 12)).toBe(0);
  });

  it('atual ausente conta como 0', () => {
    expect(proporcao(null, 12)).toBe(0);
  });

  it('sem maximo, ou com maximo 0, nao ha proporcao', () => {
    expect(proporcao(5, 0)).toBeNull();
    expect(proporcao(5, null)).toBeNull();
  });
});
