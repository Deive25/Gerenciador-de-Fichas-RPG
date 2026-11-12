import { Component, computed, input } from '@angular/core';
import { TesteResultado } from '../ficha.model';
import { contaComBonus } from './formato-rolagem';

/** Resultado de um teste (Ordem ou D&D): cada d20 rolado, o dado que ficou e o total. */
@Component({
  selector: 'app-resultado-rolagem',
  templateUrl: './resultado-rolagem.html',
  styleUrl: './resultado-rolagem.scss',
})
export class ResultadoRolagem {
  readonly resultado = input.required<TesteResultado>();
  /** Só em D&D: avisa "20 natural" ou "1 natural" (informativo; não muda o total). */
  readonly natural = input(false);

  /**
   * "Investigação: 19 + 5 = 24"; bônus negativo vira "Força: 7 − 1 = 6" (sinal U+2212, e não
   * "7 + -1"); sem bônus (atributo de Ordem, destreinada) fica "Força: 12 = 12".
   */
  readonly titulo = computed(() => {
    const r = this.resultado();
    return `${r.nome}: ${contaComBonus(r.mantido, r.bonus)} = ${r.total}`;
  });

  /** "20 natural" ou "1 natural" quando o dado que ficou foi 20 ou 1; vazio nos outros casos. */
  readonly avisoNatural = computed(() => {
    const mantido = this.resultado().mantido;
    return this.natural() && (mantido === 20 || mantido === 1) ? `${mantido} natural` : null;
  });

  /** Posição do dado que ficou: só um é marcado, mesmo com valores repetidos. */
  readonly indiceMantido = computed(() => this.resultado().dados.indexOf(this.resultado().mantido));

  readonly leitura = computed(() => {
    const r = this.resultado();
    return `Dados: ${r.dados.join(', ')}. Ficou o ${r.mantido}.`;
  });
}
