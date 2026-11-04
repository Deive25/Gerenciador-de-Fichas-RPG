import { Component, computed, input } from '@angular/core';

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

  readonly temMaximo = computed(() => {
    const maximo = this.maximo();
    return maximo !== null && maximo > 0;
  });

  readonly texto = computed(() => (this.temMaximo() ? `${this.atual() ?? 0}/${this.maximo()}` : '—'));

  readonly percentual = computed(() => {
    if (!this.temMaximo()) {
      return 0;
    }
    const proporcao = ((this.atual() ?? 0) / (this.maximo() as number)) * 100;
    return Math.min(100, Math.max(0, Math.round(proporcao)));
  });
}
