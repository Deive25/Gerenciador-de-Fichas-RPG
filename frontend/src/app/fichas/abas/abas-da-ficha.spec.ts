import { ABAS_POR_SISTEMA, abaValida } from './abas-da-ficha';

describe('abas da ficha', () => {
  it('os dois sistemas tem Ficha, Habilidades e Descricao, nessa ordem', () => {
    for (const sistema of ['ORDEM_PARANORMAL', 'DND'] as const) {
      expect(ABAS_POR_SISTEMA[sistema].map((a) => a.rotulo)).toEqual(['Ficha', 'Habilidades', 'Descrição']);
    }
  });

  it('abaValida aceita um codigo conhecido', () => {
    expect(abaValida('DND', 'habilidades')).toBe('habilidades');
  });

  it('abaValida volta para a Ficha com valor desconhecido ou ausente', () => {
    expect(abaValida('DND', 'xyz')).toBe('ficha');
    expect(abaValida('DND', undefined)).toBe('ficha');
  });
});
