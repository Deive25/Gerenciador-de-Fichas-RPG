import { Routes } from '@angular/router';
import { FichaInicio } from './fichas/inicio/ficha-inicio';

// Detalhe e formulário carregam sob demanda (spec §7): o Material de abas, dicas e campos
// fica fora do pacote inicial, que precisa abrir rápido no celular.
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'fichas' },
  { path: 'fichas', component: FichaInicio, title: 'Fichas · Fichas RPG' },
  // 'fichas/nova' antes de 'fichas/:id', senão "nova" seria lido como id.
  {
    path: 'fichas/nova',
    loadComponent: () => import('./fichas/formulario/ficha-formulario').then((m) => m.FichaFormulario),
    title: 'Nova ficha · Fichas RPG',
  },
  {
    path: 'fichas/:id',
    loadComponent: () => import('./fichas/detalhe/ficha-detalhe').then((m) => m.FichaDetalhe),
    title: 'Ficha · Fichas RPG',
  },
  {
    path: 'fichas/:id/editar',
    loadComponent: () => import('./fichas/formulario/ficha-formulario').then((m) => m.FichaFormulario),
    title: 'Editar ficha · Fichas RPG',
  },
  { path: '**', redirectTo: 'fichas' },
];
