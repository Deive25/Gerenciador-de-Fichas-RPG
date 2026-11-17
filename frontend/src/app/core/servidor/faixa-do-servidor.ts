import { Component, inject } from '@angular/core';
import { EstadoDoServidor } from './estado-do-servidor';

/**
 * Faixa no topo do app enquanto a API gratuita acorda (ou depois de desistir). Fica fora do ar quando
 * a API responde rápido: quem não espera nunca a vê.
 */
@Component({
  selector: 'app-faixa-do-servidor',
  templateUrl: './faixa-do-servidor.html',
  styleUrl: './faixa-do-servidor.scss',
})
export class FaixaDoServidor {
  protected readonly servidor = inject(EstadoDoServidor);
}
