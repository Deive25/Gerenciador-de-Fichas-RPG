import { cabeNaCena, configDosDados, pesoNaCena } from './config-dos-dados';
import { d20sDoTeste } from './dados-lancados';
import { DadoLancado } from './lancador-de-dados';

/** Os d20 de um teste, como a FichaDetalhe os monta (o mantido dourado no `indexOf(mantido)`). */
const d20 = (dados: number[], indiceMantido: number) => configDosDados(d20sDoTeste(dados, indiceMantido));

describe('configDosDados', () => {
  it('Ordem com 5 dados: so o primeiro 17 (indice 1) fica dourado', () => {
    const dados = [3, 17, 9, 17, 2];
    const config = d20(dados, dados.indexOf(17));

    expect(config.map((c) => c.rolled)).toEqual(dados);
    expect(config.every((c) => c.dice === 'd20')).toBe(true);
    const dourados = config.map((c, i) => (c.backgroundColor === '#c9a45c' ? i : -1)).filter((i) => i >= 0);
    expect(dourados).toEqual([1]);
  });

  it('o dourado usa o numero escuro e os outros, face roxa com numero claro', () => {
    const [outro, mantido] = d20([3, 17], 1);

    expect(mantido).toEqual({ dice: 'd20', rolled: 17, backgroundColor: '#c9a45c', textColor: '#15110a' });
    expect(outro).toEqual({ dice: 'd20', rolled: 3, backgroundColor: '#2b2438', textColor: '#e6e1d6' });
  });

  it('atributo 0 (2 dados, o menor mantido): dourado no menor', () => {
    const dados = [14, 6];
    const config = d20(dados, dados.indexOf(6));

    expect(config.map((c) => c.rolled)).toEqual([14, 6]);
    expect(config[0].backgroundColor).toBe('#2b2438');
    expect(config[1].backgroundColor).toBe('#c9a45c');
  });

  it('D&D com vantagem: 2 dados, o maior dourado', () => {
    const dados = [17, 6];
    const config = d20(dados, dados.indexOf(17));

    expect(config).toHaveLength(2);
    expect(config[0].backgroundColor).toBe('#c9a45c');
    expect(config[1].backgroundColor).toBe('#2b2438');
  });

  it('indice fora da lista (-1) nao deixa nenhum dado dourado', () => {
    expect(d20([5, 8], -1).some((c) => c.backgroundColor === '#c9a45c')).toBe(false);
  });

  describe('qualquer dado', () => {
    const dado = (lados: DadoLancado['lados'], valor: number, cor: DadoLancado['cor'] = 'comum'): DadoLancado => ({ lados, valor, cor });

    it.each([4, 6, 8, 12, 20] as const)('o d%i vira o tipo certo e para no valor que saiu', (lados) => {
      expect(configDosDados([dado(lados, 3)])[0]).toMatchObject({ dice: `d${lados}`, rolled: 3 });
    });

    it('o d10 vai de 0 a 9 na biblioteca: o 10 da mesa vira 0 e os outros valores ficam', () => {
      expect(configDosDados([dado(10, 10), dado(10, 1), dado(10, 9)]).map((c) => c.rolled)).toEqual([0, 1, 9]);
      expect(configDosDados([dado(10, 10)])[0].dice).toBe('d10');
    });

    it('o d100 vai de 0 a 99: o 100 da mesa vira 0 e os outros valores ficam', () => {
      expect(configDosDados([dado(100, 100), dado(100, 1), dado(100, 99), dado(100, 57)]).map((c) => c.rolled)).toEqual([0, 1, 99, 57]);
      expect(configDosDados([dado(100, 57)])[0].dice).toBe('d100');
    });

    it('o d20 e os demais nao mudam de valor, nem o 20 nem o 12', () => {
      expect(configDosDados([dado(20, 20), dado(12, 12)]).map((c) => c.rolled)).toEqual([20, 12]);
    });

    it('as tres cores: mantido dourado, comum escuro e dano vinho', () => {
      const [mantido, comum, dano] = configDosDados([dado(20, 5, 'mantido'), dado(6, 2, 'comum'), dado(8, 7, 'dano')]);
      expect(mantido).toMatchObject({ backgroundColor: '#c9a45c', textColor: '#15110a' });
      expect(comum).toMatchObject({ backgroundColor: '#2b2438', textColor: '#e6e1d6' });
      expect(dano).toMatchObject({ backgroundColor: '#5a2330', textColor: '#f0e2bf' });
    });

    it('nunca usa o diceColor (so o backgroundColor pinta a face)', () => {
      expect('diceColor' in configDosDados([dado(6, 1)])[0]).toBe(false);
    });

    it('mantem a ordem recebida (os d20 do ataque antes do dano)', () => {
      const config = configDosDados([dado(20, 14, 'comum'), dado(20, 19, 'mantido'), dado(8, 6, 'dano')]);
      expect(config.map((c) => c.dice)).toEqual(['d20', 'd20', 'd8']);
    });
  });

  describe('limite de dados na cena', () => {
    const muitos = (n: number, lados: DadoLancado['lados'] = 6) => Array.from({ length: n }, () => ({ lados }));

    it('o d100 conta como 2 dados', () => {
      expect(pesoNaCena([{ lados: 100 }, { lados: 6 }])).toBe(3);
    });

    it('20 dados cabem e 21 nao', () => {
      expect(cabeNaCena(muitos(20))).toBe(true);
      expect(cabeNaCena(muitos(21))).toBe(false);
    });

    it('10 d100 cabem (20 na cena) e 11 nao', () => {
      expect(cabeNaCena(muitos(10, 100))).toBe(true);
      expect(cabeNaCena(muitos(11, 100))).toBe(false);
    });

    it('um ataque de Ordem com 4 d20 e o dano em muitos dados pode passar do limite', () => {
      expect(cabeNaCena([...muitos(4, 20), ...muitos(17, 8)])).toBe(false);
    });
  });
});
