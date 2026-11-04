import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Layout } from '../core/layout';

/**
 * Esqueleto da aplicação, pensado primeiro para o celular: barra no topo e navegação
 * embaixo, uma tela por vez. A partir de 900 px, a navegação sobe para o topo e aparece
 * a coluna lateral (slot [lateral]), que o App preenche com a lista de fichas.
 *
 * Há um único <router-outlet>, fora de qualquer @if: cruzar 900 px (girar o aparelho,
 * redimensionar a janela) troca só o layout e não recria a tela aberta.
 */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  readonly desktop = inject(Layout).desktop;
}
