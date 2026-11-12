import { AtaqueResultado, RolagemLivre, TesteResultado } from '../ficha.model';
import { d20sDoTeste, dadosDaRolagemLivre, dadosDoAtaque, dadosDoTeste } from './dados-lancados';

describe('dados lancados', () => {
  it('d20sDoTeste: so o indice do mantido e dourado', () => {
    expect(d20sDoTeste([3, 17, 9, 17], 1).map((d) => d.cor)).toEqual(['comum', 'mantido', 'comum', 'comum']);
  });

  it('dadosDoTeste usa o indexOf(mantido): com valores repetidos, so o primeiro e dourado', () => {
    const r: TesteResultado = { alvo: 'PERICIA', codigo: 'LUTA', nome: 'Luta', dados: [4, 19, 19], mantido: 19, bonus: 5, total: 24 };
    expect(dadosDoTeste(r)).toEqual([
      { lados: 20, valor: 4, cor: 'comum' },
      { lados: 20, valor: 19, cor: 'mantido' },
      { lados: 20, valor: 19, cor: 'comum' },
    ]);
  });

  it('dadosDoAtaque: os d20 do ataque (mantido dourado) e depois os dados do dano em vinho', () => {
    const r: AtaqueResultado = {
      ataqueId: 3, nome: 'Espada longa',
      ataque: { dados: [14, 19], mantido: 19, bonus: 5, total: 24 },
      dano: { expressao: '1d8+3', dados: [{ lados: 8, valor: 6 }, { lados: 8, valor: 2 }], bonus: 3, total: 11 },
      critico: false,
    };
    expect(dadosDoAtaque(r)).toEqual([
      { lados: 20, valor: 14, cor: 'comum' },
      { lados: 20, valor: 19, cor: 'mantido' },
      { lados: 8, valor: 6, cor: 'dano' },
      { lados: 8, valor: 2, cor: 'dano' },
    ]);
  });

  it('dadosDaRolagemLivre: todos comuns, cada um com os seus lados', () => {
    const r: RolagemLivre = {
      expressao: '2d6+1d4+3', bonus: 3, total: 11,
      dados: [{ lados: 6, valor: 4 }, { lados: 6, valor: 1 }, { lados: 4, valor: 3 }],
    };
    expect(dadosDaRolagemLivre(r)).toEqual([
      { lados: 6, valor: 4, cor: 'comum' },
      { lados: 6, valor: 1, cor: 'comum' },
      { lados: 4, valor: 3, cor: 'comum' },
    ]);
  });
});
