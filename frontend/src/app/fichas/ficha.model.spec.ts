import { FichaOrdem, paraEntradaDaFicha } from './ficha.model';

describe('paraEntradaDaFicha', () => {
  const ficha: FichaOrdem = {
    id: 7, sistema: 'ORDEM_PARANORMAL', jogador: 'Arthur', personagem: 'Lia', genero: 'F', idade: 34,
    retratoVersao: 1772366400250, classe: null, origem: null, nex: 5,
    atributos: { AGI: 1, FOR: 1, INT: 1, PRE: 1, VIG: 1 },
    recursos: { PV: { atual: 9, maximo: 12 }, SAN: { atual: 8, maximo: 10 }, PE: { atual: null, maximo: null } },
    pericias: { INVESTIGACAO: 'TREINADO' },
  };

  it('tira id e retratoVersao (o JSON estrito da API recusaria) e mantem o resto', () => {
    const entrada = paraEntradaDaFicha(ficha);

    expect('id' in entrada).toBe(false);
    expect('retratoVersao' in entrada).toBe(false);
    expect(entrada.sistema).toBe('ORDEM_PARANORMAL');
    expect(entrada.personagem).toBe('Lia');
    expect(entrada.classe).toBeNull();
    expect(entrada.nex).toBe(5);
    expect(entrada.recursos.PE).toEqual({ atual: null, maximo: null });
    expect(entrada.pericias).toEqual({ INVESTIGACAO: 'TREINADO' });
  });

  it('copia os objetos internos: mudar a entrada nao muda a ficha', () => {
    const entrada = paraEntradaDaFicha(ficha);
    expect(entrada.pericias).not.toBe(ficha.pericias);
    expect(entrada.atributos).not.toBe(ficha.atributos);
    expect(entrada.recursos.PV).not.toBe(ficha.recursos.PV);
  });
});
