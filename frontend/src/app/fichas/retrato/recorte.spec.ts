import { janelaDeRecorte } from './recorte';

describe('janelaDeRecorte', () => {
  it('imagem quadrada, zoom 1: a maior janela 4:5 que cabe, centralizada', () => {
    expect(janelaDeRecorte(1000, 1000, 1, { x: 500, y: 500 })).toEqual({ x: 100, y: 0, largura: 800, altura: 1000 });
  });

  it('imagem deitada: a altura manda e a janela fica no centro', () => {
    expect(janelaDeRecorte(2000, 1000, 1, { x: 1000, y: 500 })).toEqual({ x: 600, y: 0, largura: 800, altura: 1000 });
  });

  it('imagem em pé: a largura manda', () => {
    expect(janelaDeRecorte(1000, 2000, 1, { x: 500, y: 1000 })).toEqual({ x: 0, y: 375, largura: 1000, altura: 1250 });
  });

  it('zoom 2 divide a janela por 2', () => {
    expect(janelaDeRecorte(1000, 1000, 2, { x: 500, y: 500 })).toEqual({ x: 300, y: 250, largura: 400, altura: 500 });
  });

  it('zoom fica entre 1 e 4', () => {
    expect(janelaDeRecorte(1000, 1000, 0.5, { x: 500, y: 500 })).toEqual(janelaDeRecorte(1000, 1000, 1, { x: 500, y: 500 }));
    expect(janelaDeRecorte(1000, 1000, 9, { x: 500, y: 500 })).toEqual({ x: 400, y: 375, largura: 200, altura: 250 });
  });

  it('a janela nunca sai da imagem: o centro e empurrado para dentro', () => {
    expect(janelaDeRecorte(1000, 1000, 2, { x: 0, y: 0 })).toEqual({ x: 0, y: 0, largura: 400, altura: 500 });
    expect(janelaDeRecorte(1000, 1000, 2, { x: 2000, y: 2000 })).toEqual({ x: 600, y: 500, largura: 400, altura: 500 });
  });
});
