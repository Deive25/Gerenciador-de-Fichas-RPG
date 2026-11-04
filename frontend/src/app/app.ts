import { Component } from '@angular/core';
import { FichaLista } from './fichas/lista/ficha-lista';
import { Shell } from './layout/shell';

@Component({
  imports: [Shell, FichaLista],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {}
