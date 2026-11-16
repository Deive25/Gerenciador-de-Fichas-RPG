import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BarraStatus } from '../componentes/barra-status';
import { Medalhao } from '../componentes/medalhao';
import { FichaResumo, NOMES_SISTEMA } from '../ficha.model';
import { estaFerido, temSanidadeBaixa } from './regras-painel';

/** Rótulo e cor da barra de cada código de recurso (Ordem: PV, SAN, PE). */
const APRESENTACAO_RECURSO: Record<string, { rotulo: string; tipo: 'vida' | 'sanidade' | 'esforco' }> = {
  PV: { rotulo: 'Vida', tipo: 'vida' },
  SAN: { rotulo: 'Sanidade', tipo: 'sanidade' },
  PE: { rotulo: 'Esforço', tipo: 'esforco' },
};

/**
 * Cartão de uma ficha. O cartão inteiro é o link para a ficha; com `podeAbrir` falso (a ficha de outro
 * jogador, na mesa) vira um bloco só com o resumo, sem link.
 */
@Component({
  selector: 'app-cartao-ficha',
  imports: [RouterLink, NgTemplateOutlet, BarraStatus, Medalhao],
  templateUrl: './cartao-ficha.html',
  styleUrl: './cartao-ficha.scss',
})
export class CartaoFicha {
  readonly ficha = input.required<FichaResumo>();
  readonly podeAbrir = input(true);
  /** Na mesa de uma campanha, mostra de quem é a ficha. */
  readonly mostrarDono = input(false);
  /** Mostra a campanha da ficha, quando houver (dentro da própria campanha seria repetição). */
  readonly mostrarCampanha = input(true);

  readonly legenda = computed(() => {
    const f = this.ficha();
    return f.idade === null ? f.jogador : `${f.jogador} · ${f.idade} anos`;
  });

  readonly ferido = computed(() => estaFerido(this.ficha()));
  readonly sanidadeBaixa = computed(() => temSanidadeBaixa(this.ficha()));

  readonly sistema = computed(() => NOMES_SISTEMA[this.ficha().sistema]);

  /** Uma barra por recurso conhecido do resumo, na ordem em que a API manda. */
  readonly barras = computed(() =>
    this.ficha().recursos.flatMap((r) => {
      const apresentacao = APRESENTACAO_RECURSO[r.codigo];
      return apresentacao ? [{ ...r, ...apresentacao }] : [];
    }),
  );
}
