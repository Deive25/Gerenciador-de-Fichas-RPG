/** Retângulo da imagem original que vira o retrato (em pixels da imagem). */
export interface Janela {
  x: number;
  y: number;
  largura: number;
  altura: number;
}

/** Proporção da moldura do medalhão: 4 de largura para 5 de altura. */
export const PROPORCAO = 4 / 5;
export const ZOOM_MINIMO = 1;
export const ZOOM_MAXIMO = 4;

/**
 * Qual parte da imagem cabe na moldura 4:5. Com zoom 1, é a maior janela 4:5 que cabe na imagem;
 * com zoom z, a janela fica z vezes menor (mais aproximada). A janela fica centrada em `centro`,
 * mas nunca sai da imagem: perto da borda ela é empurrada para dentro, então a imagem sempre
 * cobre a moldura inteira (sem bordas vazias).
 */
export function janelaDeRecorte(
  largura: number,
  altura: number,
  zoom: number,
  centro: { x: number; y: number },
): Janela {
  const z = Math.min(Math.max(zoom, ZOOM_MINIMO), ZOOM_MAXIMO);
  const larguraBase = Math.min(largura, altura * PROPORCAO);
  const janelaLargura = larguraBase / z;
  const janelaAltura = larguraBase / PROPORCAO / z;
  return {
    x: limitar(centro.x - janelaLargura / 2, 0, largura - janelaLargura),
    y: limitar(centro.y - janelaAltura / 2, 0, altura - janelaAltura),
    largura: janelaLargura,
    altura: janelaAltura,
  };
}

function limitar(valor: number, minimo: number, maximo: number): number {
  return Math.min(Math.max(valor, minimo), maximo);
}
