import { Component, computed, input } from '@angular/core';
import { TesteResultado } from '../ficha.model';

/** Resultado de um teste de Ordem: cada d20 rolado, o dado que ficou e o total. */
@Component({
  selector: 'app-resultado-rolagem',
  templateUrl: './resultado-rolagem.html',
  styleUrl: './resultado-rolagem.scss',
})
export class ResultadoRolagem {
  readonly resultado = input.required<TesteResultado>();

  /** "Investigação: 19 + 5 = 24"; sem bônus (atributo, destreinada) fica "Força: 12 = 12". */
  readonly titulo = computed(() => {
    const r = this.resultado();
    return r.bonus === 0 ? `${r.nome}: ${r.mantido} = ${r.total}` : `${r.nome}: ${r.mantido} + ${r.bonus} = ${r.total}`;
  });

  /** Posição do dado que ficou: só um é marcado, mesmo com valores repetidos. */
  readonly indiceMantido = computed(() => this.resultado().dados.indexOf(this.resultado().mantido));

  readonly leitura = computed(() => {
    const r = this.resultado();
    return `Dados: ${r.dados.join(', ')}. Ficou o ${r.mantido}.`;
  });
}
