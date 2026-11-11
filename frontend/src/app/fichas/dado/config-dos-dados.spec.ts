import { configDosDados } from './config-dos-dados';

describe('configDosDados', () => {
  it('Ordem com 5 dados: so o primeiro 17 (indice 1) fica dourado', () => {
    const dados = [3, 17, 9, 17, 2];
    const config = configDosDados(dados, dados.indexOf(17));

    expect(config.map((c) => c.rolled)).toEqual(dados);
    expect(config.every((c) => c.dice === 'd20')).toBe(true);
    const dourados = config.map((c, i) => (c.backgroundColor === '#c9a45c' ? i : -1)).filter((i) => i >= 0);
    expect(dourados).toEqual([1]);
  });

  it('o dourado usa o numero escuro e os outros, face roxa com numero claro', () => {
    const [outro, mantido] = configDosDados([3, 17], 1);

    expect(mantido).toEqual({ dice: 'd20', rolled: 17, backgroundColor: '#c9a45c', textColor: '#15110a' });
    expect(outro).toEqual({ dice: 'd20', rolled: 3, backgroundColor: '#2b2438', textColor: '#e6e1d6' });
  });

  it('atributo 0 (2 dados, o menor mantido): dourado no menor', () => {
    const dados = [14, 6];
    const config = configDosDados(dados, dados.indexOf(6));

    expect(config.map((c) => c.rolled)).toEqual([14, 6]);
    expect(config[0].backgroundColor).toBe('#2b2438');
    expect(config[1].backgroundColor).toBe('#c9a45c');
  });

  it('D&D com vantagem: 2 dados, o maior dourado', () => {
    const dados = [17, 6];
    const config = configDosDados(dados, dados.indexOf(17));

    expect(config).toHaveLength(2);
    expect(config[0].backgroundColor).toBe('#c9a45c');
    expect(config[1].backgroundColor).toBe('#2b2438');
  });

  it('indice fora da lista (-1) nao deixa nenhum dado dourado', () => {
    expect(configDosDados([5, 8], -1).some((c) => c.backgroundColor === '#c9a45c')).toBe(false);
  });
});
