import { Component, computed, input } from '@angular/core';
import { RolagemLivre } from '../ficha.model';

/** Resultado de uma rolagem livre: a expressão e o total ("2d6+1d4+3 = 11"), cada dado ("d6: 4") e o modificador. */
@Component({
  selector: 'app-resultado-livre',
  templateUrl: './resultado-livre.html',
  styleUrl: './resultado-livre.scss',
})
export class ResultadoLivre {
  readonly resultado = input.required<RolagemLivre>();

  readonly titulo = computed(() => `${this.resultado().expressao} = ${this.resultado().total}`);

  /** "+3" ou "−1" (sinal U+2212); vazio quando não há modificador. */
  readonly modificador = computed(() => {
    const { bonus } = this.resultado();
    return bonus === 0 ? null : bonus < 0 ? `−${-bonus}` : `+${bonus}`;
  });

  readonly leitura = computed(() => {
    const r = this.resultado();
    const dados = r.dados.map((d) => `d${d.lados} ${d.valor}`).join(', ');
    return `Dados: ${dados}.` + (this.modificador() ? ` Modificador ${this.modificador()}.` : '');
  });
}
