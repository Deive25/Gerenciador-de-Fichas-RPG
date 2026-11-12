import { Component, computed, input } from '@angular/core';
import { AtaqueResultado } from '../ficha.model';
import { contaComBonus } from './formato-rolagem';

/**
 * Resultado de um ataque (Ordem ou D&D): o teste ("Espada longa: 19 + 5 = 24") com os d20 e o mantido em
 * destaque, o dano ("Dano: 6 + 3 = 9") com cada dado, o selo "Crítico" e, em D&D, o 20 ou 1 natural.
 */
@Component({
  selector: 'app-resultado-ataque',
  templateUrl: './resultado-ataque.html',
  styleUrl: './resultado-ataque.scss',
})
export class ResultadoAtaque {
  readonly resultado = input.required<AtaqueResultado>();
  /** Só em D&D: avisa "20 natural" ou "1 natural" (informativo; não muda o total). */
  readonly natural = input(false);

  readonly tituloAtaque = computed(() => {
    const { nome, ataque } = this.resultado();
    return `${nome}: ${contaComBonus(ataque.mantido, ataque.bonus)} = ${ataque.total}`;
  });

  /** "Dano: 6 + 2 + 3 = 11": o valor de cada dado, o bônus e o total; só constante, "Dano: 5". */
  readonly tituloDano = computed(() => {
    const { dano } = this.resultado();
    if (dano.dados.length === 0) {
      return `Dano: ${dano.total}`;
    }
    return `Dano: ${contaComBonus(dano.dados.map((d) => d.valor).join(' + '), dano.bonus)} = ${dano.total}`;
  });

  /** Posição do d20 que ficou: só um é marcado, mesmo com valores repetidos. */
  readonly indiceMantido = computed(() => this.resultado().ataque.dados.indexOf(this.resultado().ataque.mantido));

  readonly avisoNatural = computed(() => {
    const mantido = this.resultado().ataque.mantido;
    return this.natural() && (mantido === 20 || mantido === 1) ? `${mantido} natural` : null;
  });

  readonly leitura = computed(() => {
    const { ataque, dano, critico } = this.resultado();
    const dadosDoDano = dano.dados.map((d) => `d${d.lados} ${d.valor}`).join(', ');
    return (
      `Dados do ataque: ${ataque.dados.join(', ')}. Ficou o ${ataque.mantido}.` +
      (critico ? ' Crítico.' : '') +
      (dadosDoDano ? ` Dados do dano: ${dadosDoDano}.` : '')
    );
  });
}
