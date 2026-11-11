import { ABAS_POR_SISTEMA, abaValida } from './abas-da-ficha';

describe('abas da ficha', () => {
  it('Ordem tem Ficha, Inventario, Habilidades e Descricao, nessa ordem', () => {
    expect(ABAS_POR_SISTEMA.ORDEM_PARANORMAL.map((a) => a.rotulo)).toEqual(['Ficha', 'Inventário', 'Habilidades', 'Descrição']);
  });

  it('D&D tem as mesmas 4 abas, na mesma ordem', () => {
    expect(ABAS_POR_SISTEMA.DND.map((a) => a.rotulo)).toEqual(['Ficha', 'Inventário', 'Habilidades', 'Descrição']);
  });

  it('abaValida aceita inventario em Ordem', () => {
    expect(abaValida('ORDEM_PARANORMAL', 'inventario')).toBe('inventario');
  });

  it('abaValida aceita um codigo conhecido', () => {
    expect(abaValida('DND', 'habilidades')).toBe('habilidades');
  });

  it('abaValida volta para a Ficha com valor desconhecido ou ausente', () => {
    expect(abaValida('DND', 'xyz')).toBe('ficha');
    expect(abaValida('DND', undefined)).toBe('ficha');
  });
});
