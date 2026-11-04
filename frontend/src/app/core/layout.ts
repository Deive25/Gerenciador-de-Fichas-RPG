import { BreakpointObserver } from '@angular/cdk/layout';
import { Injectable, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

/** Largura a partir da qual a interface usa duas colunas (spec §7). */
export const LARGURA_COMPUTADOR = '(min-width: 900px)';

/** Única fonte do "estamos no computador?", usada pelo shell, pelo App e pelas telas. */
@Injectable({ providedIn: 'root' })
export class Layout {
  readonly desktop = toSignal(
    inject(BreakpointObserver)
      .observe(LARGURA_COMPUTADOR)
      .pipe(map((estado) => estado.matches)),
    { initialValue: false },
  );
}
