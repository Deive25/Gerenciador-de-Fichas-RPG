import { ATRIBUTOS, PERICIAS, faixas, rotuloResultado } from './catalogo';

const CODIGOS_DA_API = [
  'ACROBACIA', 'ATLETISMO', 'ADESTRAR_ANIMAIS', 'ATUACAO_LABIA', 'CHARME', 'ESQUIVA', 'FURTIVIDADE',
  'LUTA_CORPO_A_CORPO', 'LUTA_ARMA_BRANCA', 'INTIMIDACAO', 'INTUICAO', 'INVESTIGACAO',
  'ARMAS_DE_FOGO_LEVES', 'ARMAS_DE_FOGO_MEDIAS', 'ARMAS_DE_FOGO_PESADAS', 'PERCEPCAO',
  'USAR_BIBLIOTECAS', 'HISTORIA', 'CURA', 'MALANDRAGEM', 'OCULTISMO', 'PSICOLOGIA', 'ARREMESSAR',
];

describe('catalogo', () => {
  it('tem as 23 pericias na ordem do enum da API', () => {
    expect(PERICIAS.map((p) => p.chave)).toEqual(CODIGOS_DA_API);
    expect(PERICIAS[0].nome).toBe('Acrobacia');
    expect(PERICIAS[22].nome).toBe('Arremessar');
  });

  it('tem os 9 atributos na ordem da ficha', () => {
    expect(ATRIBUTOS.map((a) => a.chave)).toEqual([
      'forca', 'destreza', 'constituicao', 'inteligencia', 'carisma', 'sabedoria', 'tamanho', 'sorte', 'poder',
    ]);
  });

  it('cada item do catalogo tem descricao', () => {
    for (const item of [...ATRIBUTOS, ...PERICIAS]) {
      expect(item.descricao.length, item.chave).toBeGreaterThan(10);
    }
  });

  it('calcula as faixas pela divisao inteira', () => {
    expect(faixas(70)).toEqual({ extremo: 14, sucesso: 70 });
    expect(faixas(49)).toEqual({ extremo: 9, sucesso: 49 });
    expect(faixas(0)).toEqual({ extremo: 0, sucesso: 0 });
  });

  it('traduz o resultado', () => {
    expect(rotuloResultado('SUCESSO_EXTREMO')).toBe('Sucesso extremo');
    expect(rotuloResultado('SUCESSO')).toBe('Sucesso');
    expect(rotuloResultado('FALHA')).toBe('Falha');
  });
});
