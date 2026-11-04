import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { LARGURA_COMPUTADOR } from '../../layout/shell';
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
  readonly desktop = toSignal(
    inject(BreakpointObserver)
      .observe(LARGURA_COMPUTADOR)
      .pipe(map((estado) => estado.matches)),
    { initialValue: false },
  );
}
