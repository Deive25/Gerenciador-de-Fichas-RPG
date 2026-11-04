import { Component, computed, input } from '@angular/core';
import { proporcao } from './proporcao';

/** Comprimento da circunferência do anel (r = 16): 2π·16 ≈ 100,5. */
const VOLTA = 100.5;

/**
 * Vida ou sanidade em anel. O desenho é só reforço visual (aria-hidden): o valor é sempre
 * lido no texto ao lado ("Vida 9/12"), então o anel nunca é a única forma de saber o número.
 */
@Component({
  selector: 'app-anel-status',
  templateUrl: './anel-status.html',
  styleUrl: './anel-status.scss',
})
export class AnelStatus {
  readonly rotulo = input.required<string>();
  readonly tipo = input.required<'vida' | 'sanidade'>();
  readonly atual = input<number | null>(null);
  readonly maximo = input<number | null>(null);

  readonly percentual = computed(() => proporcao(this.atual(), this.maximo()));

  readonly texto = computed(() => (this.percentual() === null ? '—' : `${this.atual() ?? 0}/${this.maximo()}`));

  /** "arco volta": o arco cobre `percentual`% da volta. */
  readonly tracos = computed(() => `${((this.percentual() ?? 0) * VOLTA) / 100} ${VOLTA}`);
}
