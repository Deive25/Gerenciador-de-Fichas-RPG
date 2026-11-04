import { Component, inject } from '@angular/core';
import { Layout } from './core/layout';
import { FichaLista } from './fichas/lista/ficha-lista';
import { Shell } from './layout/shell';

@Component({
  imports: [Shell, FichaLista],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  /** A lista lateral só existe no computador; no celular ela nem é criada (nada de GET à toa). */
  readonly desktop = inject(Layout).desktop;
}
