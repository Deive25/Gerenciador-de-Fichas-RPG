import { Ficha, paraEntradaDaFicha } from './ficha.model';

describe('paraEntradaDaFicha', () => {
  it('tira id e retratoVersao (o JSON estrito da API recusaria) e mantem o resto', () => {
    const ficha: Ficha = {
      id: 7, jogador: 'Arthur', personagem: 'Lia', genero: 'F', idade: 34,
      vidaAtual: 9, vidaMaxima: 12, sanidadeAtual: 48, sanidadeMaxima: 60,
      forca: 45, destreza: null, constituicao: null, inteligencia: null, carisma: null,
      sabedoria: null, tamanho: null, sorte: null, poder: null,
      pericias: { INVESTIGACAO: 70 }, retratoVersao: 1772366400250,
    };

    const entrada = paraEntradaDaFicha(ficha);

    expect('id' in entrada).toBe(false);
    expect('retratoVersao' in entrada).toBe(false);
    expect(entrada.personagem).toBe('Lia');
    expect(entrada.vidaAtual).toBe(9);
    expect(entrada.pericias).toEqual({ INVESTIGACAO: 70 });
    expect(entrada.pericias).not.toBe(ficha.pericias);
  });
});
