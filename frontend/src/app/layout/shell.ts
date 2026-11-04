import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';

/** Largura a partir da qual a interface usa duas colunas (spec §7). */
export const LARGURA_COMPUTADOR = '(min-width: 900px)';

/**
 * Esqueleto da aplicação, pensado primeiro para o celular: barra no topo e navegação
 * embaixo, uma tela por vez. A partir de 900 px, a navegação sobe para o topo e aparece
 * a coluna lateral (slot [lateral]), que o App preenche com a lista de fichas.
 */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly breakpoints = inject(BreakpointObserver);

  readonly desktop = toSignal(
    this.breakpoints.observe(LARGURA_COMPUTADOR).pipe(map((estado) => estado.matches)),
    { initialValue: false },
  );
}
