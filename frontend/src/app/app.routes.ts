import { Routes } from '@angular/router';
import { FichaDetalhe } from './fichas/detalhe/ficha-detalhe';
import { FichaInicio } from './fichas/inicio/ficha-inicio';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'fichas' },
  { path: 'fichas', component: FichaInicio, title: 'Fichas · Fichas RPG' },
  { path: 'fichas/:id', component: FichaDetalhe, title: 'Ficha · Fichas RPG' },
  { path: '**', redirectTo: 'fichas' },
];
