import { AfterViewInit, Component, DestroyRef, ElementRef, inject, input, output, viewChild } from '@angular/core';
import { DadoLancado, LancadorDeDados } from './lancador-de-dados';

/** Quanto os dados parados ficam visíveis antes de a camada fechar, para dar tempo de ler o valor. */
const PAUSA_COM_OS_DADOS_PARADOS_MS = 600;
/** Se a animação não terminar até aqui, fecha mesmo assim (física travada em aparelho lento). */
const LIMITE_MS = 5000;

/**
 * Camada sobre a tela inteira onde os dados são lançados. Emite `terminou` uma vez só: quando os dados
 * param (mais uma pausa para ler), no toque, no Esc, no limite de 5 s ou se a animação falhar.
 * Sempre libera a cena (`encerrar`) ao fechar.
 */
@Component({
  selector: 'app-camada-dos-dados',
  templateUrl: './camada-dos-dados.html',
  styleUrl: './camada-dos-dados.scss',
  host: {
    // Quem anuncia o resultado é o role="status" do cartão; a camada é só visual.
    'aria-hidden': 'true',
    '(click)': 'pular()',
    '(document:keydown.escape)': 'pular()',
  },
})
export class CamadaDosDados implements AfterViewInit {
  readonly lancador = input.required<LancadorDeDados>();
  readonly dados = input.required<DadoLancado[]>();
  readonly terminou = output<void>();

  private readonly palco = viewChild.required<ElementRef<HTMLElement>>('palco');
  private fechada = false;
  private timers: ReturnType<typeof setTimeout>[] = [];

  constructor() {
    // Saiu da ficha (ou trocou de ficha) durante a animação: libera a cena, sem emitir.
    inject(DestroyRef).onDestroy(() => this.fechar(false));
  }

  ngAfterViewInit(): void {
    this.timers.push(setTimeout(() => this.fechar(true), LIMITE_MS));
    try {
      this.lancador()
        .lancar(this.palco().nativeElement, this.dados())
        .then(
          () => this.timers.push(setTimeout(() => this.fechar(true), PAUSA_COM_OS_DADOS_PARADOS_MS)),
          (erro: unknown) => this.falhar(erro),
        );
    } catch (erro) {
      this.falhar(erro);
    }
  }

  /** Toque ou Esc: corta a animação e mostra o resultado na hora. */
  pular(): void {
    this.fechar(true);
  }

  private falhar(erro: unknown): void {
    console.error('Falha na animação do dado; mostrando o resultado direto.', erro);
    this.fechar(true);
  }

  private fechar(emitir: boolean): void {
    if (this.fechada) {
      return;
    }
    this.fechada = true;
    this.timers.forEach(clearTimeout);
    this.lancador().encerrar();
    if (emitir) {
      this.terminou.emit();
    }
  }
}
