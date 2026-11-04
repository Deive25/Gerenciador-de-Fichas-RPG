import { Injectable } from '@angular/core';
import { Janela } from './recorte';

/** Tamanho final do retrato enviado à API (proporção 4:5). */
export const LARGURA_RETRATO = 400;
export const ALTURA_RETRATO = 500;
const QUALIDADE_JPEG = 0.85;

export interface ImagemCarregada {
  imagem: CanvasImageSource;
  largura: number;
  altura: number;
}

/**
 * Tudo o que mexe com canvas e decodificação de imagem fica aqui, num serviço que os testes
 * substituem: o jsdom não desenha nem decodifica imagens. A conta do recorte (recorte.ts) é pura e
 * testada à parte.
 */
@Injectable({ providedIn: 'root' })
export class ProcessadorDeImagem {
  async carregar(arquivo: File): Promise<ImagemCarregada> {
    try {
      const imagem = await createImageBitmap(arquivo);
      return { imagem, largura: imagem.width, altura: imagem.height };
    } catch {
      // Ex.: HEIC em navegadores que não o abrem, ou um arquivo que não é imagem.
      throw new Error('Não foi possível abrir esta imagem; tente JPG ou PNG');
    }
  }

  /** Desenha a janela escolhida na prévia do diálogo. */
  desenharPrevia(canvas: HTMLCanvasElement, imagem: CanvasImageSource, janela: Janela): void {
    const contexto = canvas.getContext('2d');
    contexto?.drawImage(imagem, janela.x, janela.y, janela.largura, janela.altura, 0, 0, canvas.width, canvas.height);
  }

  /** Recorta a janela e reduz para 400×500 em JPEG (qualidade 0,85): costuma dar 30–60 KB. */
  recortar(imagem: CanvasImageSource, janela: Janela): Promise<Blob> {
    const canvas = document.createElement('canvas');
    canvas.width = LARGURA_RETRATO;
    canvas.height = ALTURA_RETRATO;
    const contexto = canvas.getContext('2d');
    if (!contexto) {
      return Promise.reject(new Error('Este navegador não consegue recortar imagens.'));
    }
    contexto.drawImage(imagem, janela.x, janela.y, janela.largura, janela.altura, 0, 0, LARGURA_RETRATO, ALTURA_RETRATO);
    return new Promise((resolver, rejeitar) =>
      canvas.toBlob(
        (blob) => (blob ? resolver(blob) : rejeitar(new Error('Não foi possível gerar o JPEG.'))),
        'image/jpeg',
        QUALIDADE_JPEG,
      ),
    );
  }
}
