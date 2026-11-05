import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Primeiro passo de "nova ficha": escolher o sistema; cada cartão leva ao formulário do seu sistema. */
@Component({
  selector: 'app-escolha-sistema',
  imports: [RouterLink],
  templateUrl: './escolha-sistema.html',
  styleUrl: './escolha-sistema.scss',
})
export class EscolhaSistema {}
