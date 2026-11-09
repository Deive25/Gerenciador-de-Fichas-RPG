import { FichaDnd, FichaOrdem, paraEntradaDaFicha } from './ficha.model';

describe('paraEntradaDaFicha', () => {
  const ficha: FichaOrdem = {
    id: 7, sistema: 'ORDEM_PARANORMAL', jogador: 'Arthur', personagem: 'Lia', genero: 'F', idade: 34,
    retratoVersao: 1772366400250, classe: null, origem: null, nex: 5,
    atributos: { AGI: 1, FOR: 1, INT: 1, PRE: 1, VIG: 1 },
    recursos: { PV: { atual: 9, maximo: 12 }, SAN: { atual: 8, maximo: 10 }, PE: { atual: null, maximo: null } },
    pericias: { INVESTIGACAO: 'TREINADO' },
    descricao: { aparencia: 'Alta', historia: null, anotacoes: null },
    habilidades: [{ id: 1, nome: 'Ataque Especial', tipo: 'CLASSE', descricao: null }],
  };

  it('tira id, retratoVersao, descricao e habilidades (o JSON estrito da API recusaria) e mantem o resto', () => {
    const entrada = paraEntradaDaFicha(ficha);

    expect('id' in entrada).toBe(false);
    expect('retratoVersao' in entrada).toBe(false);
    expect('descricao' in entrada).toBe(false);
    expect('habilidades' in entrada).toBe(false);
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

  it('de uma ficha de D&D tira id e retratoVersao e copia os objetos internos', () => {
    const dnd: FichaDnd = {
      id: 3, sistema: 'DND', jogador: 'A', personagem: 'B', genero: 'F', idade: null, retratoVersao: 99,
      edicao: 'D2014', classe: null, especie: 'Anão', antecedente: null, nivel: 5,
      atributos: { FOR: 10, DES: 10, CON: 10, INT: 10, SAB: 10, CAR: 10 },
      recursos: { PV: { atual: 5, maximo: 9 }, PV_TEMP: { atual: 2, maximo: null } },
      ca: 14, inspiracao: true, salvaguardas: ['FOR'], pericias: { ATLETISMO: 'ESPECIALISTA' },
      descricao: { aparencia: null, historia: 'Órfão', anotacoes: null },
      habilidades: [{ id: 2, nome: 'Ataque Furtivo', tipo: 'CLASSE', descricao: null }],
    };
    const entrada = paraEntradaDaFicha(dnd);
    expect('id' in entrada).toBe(false);
    expect('retratoVersao' in entrada).toBe(false);
    expect('descricao' in entrada).toBe(false);
    expect('habilidades' in entrada).toBe(false);
    expect(entrada.sistema).toBe('DND');
    if (entrada.sistema !== 'DND') return;
    expect(entrada.edicao).toBe('D2014');
    expect(entrada.recursos.PV_TEMP).toEqual({ atual: 2, maximo: null });
    expect(entrada.salvaguardas).toEqual(['FOR']);
    expect(entrada.salvaguardas).not.toBe(dnd.salvaguardas);
    expect(entrada.pericias).toEqual({ ATLETISMO: 'ESPECIALISTA' });
    expect(entrada.pericias).not.toBe(dnd.pericias);
    expect(entrada.atributos).not.toBe(dnd.atributos);
    expect(entrada.recursos.PV).not.toBe(dnd.recursos.PV);
  });
});
