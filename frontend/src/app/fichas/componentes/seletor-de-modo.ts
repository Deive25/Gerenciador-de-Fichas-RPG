import { Component, input, model } from '@angular/core';
import { ModoTeste } from '../ficha.model';

/** Os três modos do seletor, na ordem em que aparecem. */
const MODOS: readonly { valor: ModoTeste; rotulo: string }[] = [
  { valor: 'NORMAL', rotulo: 'Normal' },
  { valor: 'VANTAGEM', rotulo: 'Vantagem' },
  { valor: 'DESVANTAGEM', rotulo: 'Desvantagem' },
];

let proximoId = 0;

/**
 * Seletor Normal · Vantagem · Desvantagem de D&D, usado na aba Ficha e na aba Ataques. Quem usa guarda o
 * modo (`[(modo)]`) e o volta a Normal depois de rolar; o seletor só desenha e avisa a escolha.
 */
@Component({
  selector: 'app-seletor-de-modo',
  templateUrl: './seletor-de-modo.html',
  styleUrl: './seletor-de-modo.scss',
})
export class SeletorDeModo {
  readonly modo = model<ModoTeste>('NORMAL');
  readonly titulo = input('Modo da próxima rolagem');

  protected readonly modos = MODOS;
  /** Um id por instância: duas abas nunca ficam abertas juntas, mas o aria-labelledby não deve depender disso. */
  protected readonly idDoRotulo = `rotulo-modo-rolagem-${proximoId++}`;
}
