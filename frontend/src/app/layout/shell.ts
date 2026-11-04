import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

/**
 * Esqueleto da aplicação: topo fixo (marca + "Nova ficha") e uma tela por vez, igual no
 * celular e no computador (spec do redesign, §3.1). Sai o layout em dois painéis do 2.1.
 *
 * O <router-outlet> é único e não fica dentro de nenhum @if de largura: girar o aparelho
 * ou redimensionar a janela nunca recria a tela aberta (nem perde o que foi digitado).
 */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {}
