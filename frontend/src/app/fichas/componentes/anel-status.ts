import {
  Component,
  ElementRef,
  booleanAttribute,
  computed,
  effect,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { proporcao } from './proporcao';

/** Comprimento da circunferência do anel (r = 16): 2π·16 ≈ 100,5. */
const VOLTA = 100.5;

/**
 * Vida, sanidade ou esforço em anel. O desenho é só reforço visual (aria-hidden): o valor é sempre
 * lido no texto ao lado ("Vida 9/12"), então o anel nunca é a única forma de saber o número.
 *
 * Com `editavel` (edição rápida, 2.1c), ganha os botões − e + e o valor vira um botão que abre um
 * campo para digitar. Quem guarda e salva o valor é o pai (RecursosFicha): aqui só saem os eventos.
 */
@Component({
  selector: 'app-anel-status',
  templateUrl: './anel-status.html',
  styleUrl: './anel-status.scss',
})
export class AnelStatus {
  readonly rotulo = input.required<string>();
  readonly tipo = input.required<'vida' | 'sanidade' | 'esforco'>();
  readonly atual = input<number | null>(null);
  readonly maximo = input<number | null>(null);
  readonly editavel = input(false, { transform: booleanAttribute });

  /** −1 ou +1. */
  readonly ajustar = output<number>();
  /** Valor digitado (o pai aplica os limites). */
  readonly definir = output<number>();

  readonly percentual = computed(() => proporcao(this.atual(), this.maximo()));

  readonly texto = computed(() => (this.percentual() === null ? '—' : `${this.atual() ?? 0}/${this.maximo()}`));

  /** "arco volta": o arco cobre `percentual`% da volta. */
  readonly tracos = computed(() => `${((this.percentual() ?? 0) * VOLTA) / 100} ${VOLTA}`);

  readonly nomeMinusculo = computed(() => this.rotulo().toLowerCase());
  readonly podeDiminuir = computed(() => (this.atual() ?? 0) > 0);
  readonly podeAumentar = computed(() => {
    const maximo = this.maximo();
    return maximo === null || (this.atual() ?? 0) < maximo;
  });

  readonly editando = signal(false);
  private readonly campo = viewChild<ElementRef<HTMLInputElement>>('campo');

  constructor() {
    // Quando o campo aparece, já vem com o foco e o texto selecionado (é só digitar).
    effect(() => {
      const campo = this.campo()?.nativeElement;
      if (campo) {
        campo.focus();
        campo.select();
      }
    });
  }

  confirmar(campo: HTMLInputElement): void {
    if (!this.editando()) {
      return;
    }
    this.editando.set(false);
    const valor = Number(campo.value);
    if (campo.value.trim() !== '' && Number.isFinite(valor) && valor !== this.atual()) {
      this.definir.emit(Math.round(valor));
    }
  }

  cancelar(): void {
    this.editando.set(false);
  }
}
