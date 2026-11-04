import { Routes } from '@angular/router';
import { FichaInicio } from './fichas/inicio/ficha-inicio';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'fichas' },
  { path: 'fichas', component: FichaInicio, title: 'Fichas · Fichas RPG' },
  { path: '**', redirectTo: 'fichas' },
];
