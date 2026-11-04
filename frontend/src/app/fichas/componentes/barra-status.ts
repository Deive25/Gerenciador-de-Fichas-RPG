import { Component, computed, input } from '@angular/core';
import { proporcao } from './proporcao';

/** Barra de vida ou sanidade: "atual/máximo" e uma barra proporcional (0–100%). */
@Component({
  selector: 'app-barra-status',
  templateUrl: './barra-status.html',
  styleUrl: './barra-status.scss',
})
export class BarraStatus {
  readonly rotulo = input.required<string>();
  readonly tipo = input.required<'vida' | 'sanidade'>();
  readonly atual = input<number | null>(null);
  readonly maximo = input<number | null>(null);

  private readonly proporcao = computed(() => proporcao(this.atual(), this.maximo()));

  readonly temMaximo = computed(() => this.proporcao() !== null);

  readonly texto = computed(() => (this.temMaximo() ? `${this.atual() ?? 0}/${this.maximo()}` : '—'));

  readonly percentual = computed(() => this.proporcao() ?? 0);
}
