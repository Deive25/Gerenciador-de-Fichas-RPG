import { ABAS_POR_SISTEMA, abaValida } from './abas-da-ficha';

describe('abas da ficha', () => {
  it('Ordem tem Ficha, Inventario, Rituais, Habilidades e Descricao, nessa ordem', () => {
    expect(ABAS_POR_SISTEMA.ORDEM_PARANORMAL.map((a) => a.rotulo)).toEqual([
      'Ficha', 'Inventário', 'Rituais', 'Habilidades', 'Descrição',
    ]);
  });

  it('D&D tem Ficha, Inventario, Magias, Habilidades e Descricao, nessa ordem', () => {
    expect(ABAS_POR_SISTEMA.DND.map((a) => a.rotulo)).toEqual(['Ficha', 'Inventário', 'Magias', 'Habilidades', 'Descrição']);
  });

  it('abaValida aceita magias em D&D e rituais em Ordem', () => {
    expect(abaValida('DND', 'magias')).toBe('magias');
    expect(abaValida('ORDEM_PARANORMAL', 'rituais')).toBe('rituais');
  });

  it('rituais em D&D e magias em Ordem abrem a Ficha', () => {
    expect(abaValida('DND', 'rituais')).toBe('ficha');
    expect(abaValida('ORDEM_PARANORMAL', 'magias')).toBe('ficha');
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
