import { Component, ElementRef, computed, effect, inject, signal, viewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { ImagemCarregada, ProcessadorDeImagem } from './processador-de-imagem';
import { ZOOM_MAXIMO, ZOOM_MINIMO, janelaDeRecorte } from './recorte';

export interface DadosRecorte {
  arquivo: File;
}

/**
 * Diálogo de recorte do retrato (spec do redesign §4.1): a imagem aparece na moldura 4:5; arrastar
 * (ou as setas do teclado) posiciona e o controle de zoom aproxima. "Salvar" fecha com o JPEG
 * 400×500 já reduzido; "Cancelar" fecha sem nada.
 */
@Component({
  selector: 'app-recorte-retrato',
  imports: [MatDialogModule],
  templateUrl: './recorte-retrato.html',
  styleUrl: './recorte-retrato.scss',
})
export class RecorteRetrato {
  private readonly dados = inject<DadosRecorte>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<RecorteRetrato, Blob | undefined>>(MatDialogRef);
  private readonly processador = inject(ProcessadorDeImagem);
  private readonly previa = viewChild<ElementRef<HTMLCanvasElement>>('previa');

  readonly zoomMinimo = ZOOM_MINIMO;
  readonly zoomMaximo = ZOOM_MAXIMO;

  readonly carregada = signal<ImagemCarregada | null>(null);
  readonly erro = signal<string | null>(null);
  readonly zoom = signal(1);
  readonly centro = signal({ x: 0, y: 0 });
  readonly salvando = signal(false);

  readonly janela = computed(() => {
    const imagem = this.carregada();
    return imagem ? janelaDeRecorte(imagem.largura, imagem.altura, this.zoom(), this.centro()) : null;
  });

  private arraste: { x: number; y: number; centro: { x: number; y: number } } | null = null;

  constructor() {
    this.processador.carregar(this.dados.arquivo).then(
      (imagem) => {
        this.carregada.set(imagem);
        this.centro.set({ x: imagem.largura / 2, y: imagem.altura / 2 });
      },
      (erro: Error) => this.erro.set(erro.message),
    );
    // Redesenha a prévia sempre que a janela muda (zoom, arraste) ou a imagem termina de abrir.
    effect(() => {
      const imagem = this.carregada();
      const janela = this.janela();
      const canvas = this.previa()?.nativeElement;
      if (imagem && janela && canvas) {
        this.processador.desenharPrevia(canvas, imagem.imagem, janela);
      }
    });
  }

  aoMudarZoom(evento: Event): void {
    this.zoom.set(Number((evento.target as HTMLInputElement).value));
  }

  iniciarArraste(evento: PointerEvent): void {
    this.arraste = { x: evento.clientX, y: evento.clientY, centro: this.centro() };
    (evento.target as Element).setPointerCapture?.(evento.pointerId);
  }

  /** A imagem acompanha o dedo: arrastar para a direita mostra o que está à esquerda. */
  arrastar(evento: PointerEvent): void {
    const janela = this.janela();
    const canvas = this.previa()?.nativeElement;
    if (!this.arraste || !janela || !canvas) {
      return;
    }
    const escala = janela.largura / (canvas.clientWidth || canvas.width);
    this.moverPara({
      x: this.arraste.centro.x - (evento.clientX - this.arraste.x) * escala,
      y: this.arraste.centro.y - (evento.clientY - this.arraste.y) * escala,
    });
  }

  soltar(): void {
    this.arraste = null;
  }

  /** Setas do teclado movem 5% da janela por toque (acessível sem mouse). */
  aoTeclar(evento: KeyboardEvent): void {
    const janela = this.janela();
    const passos: Record<string, [number, number]> = {
      ArrowLeft: [-1, 0],
      ArrowRight: [1, 0],
      ArrowUp: [0, -1],
      ArrowDown: [0, 1],
    };
    const passo = passos[evento.key];
    if (!janela || !passo) {
      return;
    }
    evento.preventDefault();
    const centro = this.centro();
    this.moverPara({ x: centro.x + passo[0] * janela.largura * 0.05, y: centro.y + passo[1] * janela.altura * 0.05 });
  }

  async salvar(): Promise<void> {
    const imagem = this.carregada();
    const janela = this.janela();
    if (!imagem || !janela || this.salvando()) {
      return;
    }
    this.salvando.set(true);
    try {
      this.ref.close(await this.processador.recortar(imagem.imagem, janela));
    } catch (erro) {
      this.erro.set((erro as Error).message);
      this.salvando.set(false);
    }
  }

  cancelar(): void {
    this.ref.close(undefined);
  }

  /** Guarda o centro já "empurrado para dentro", para o arraste não acumular sobra fora da imagem. */
  private moverPara(centro: { x: number; y: number }): void {
    const imagem = this.carregada();
    if (!imagem) {
      return;
    }
    const janela = janelaDeRecorte(imagem.largura, imagem.altura, this.zoom(), centro);
    this.centro.set({ x: janela.x + janela.largura / 2, y: janela.y + janela.altura / 2 });
  }
}
