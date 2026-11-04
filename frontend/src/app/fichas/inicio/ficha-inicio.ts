import { Component, inject } from '@angular/core';
import { Layout } from '../../core/layout';
import { FichaLista } from '../lista/ficha-lista';

/**
 * Rota /fichas. No celular é a própria lista; no computador a lista já está na coluna
 * lateral, então aqui fica só um convite para escolher ou criar uma ficha.
 */
@Component({
  selector: 'app-ficha-inicio',
  imports: [FichaLista],
  templateUrl: './ficha-inicio.html',
})
export class FichaInicio {
  readonly desktop = inject(Layout).desktop;
}
