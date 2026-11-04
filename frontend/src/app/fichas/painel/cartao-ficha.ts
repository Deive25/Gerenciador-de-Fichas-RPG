import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BarraStatus } from '../componentes/barra-status';
import { Medalhao } from '../componentes/medalhao';
import { Ficha } from '../ficha.model';
import { estaFerido, temSanidadeBaixa } from './regras-painel';

/** Cartão de uma ficha no painel. O cartão inteiro é o link para a ficha. */
@Component({
  selector: 'app-cartao-ficha',
  imports: [RouterLink, BarraStatus, Medalhao],
  templateUrl: './cartao-ficha.html',
  styleUrl: './cartao-ficha.scss',
})
export class CartaoFicha {
  readonly ficha = input.required<Ficha>();

  readonly legenda = computed(() => {
    const f = this.ficha();
    return f.idade === null ? f.jogador : `${f.jogador} · ${f.idade} anos`;
  });

  readonly ferido = computed(() => estaFerido(this.ficha()));
  readonly sanidadeBaixa = computed(() => temSanidadeBaixa(this.ficha()));
}
