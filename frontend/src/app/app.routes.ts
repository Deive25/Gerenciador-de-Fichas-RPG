import { Routes } from '@angular/router';
import { Painel } from './fichas/painel/painel';

// Detalhe e formulário carregam sob demanda (spec §7): o Material de abas, dicas e campos
// fica fora do pacote inicial, que precisa abrir rápido no celular.
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'fichas' },
  // O painel é a tela de entrada: vai no pacote inicial para abrir sem uma segunda requisição.
  { path: 'fichas', component: Painel, title: 'Fichas · Fichas RPG' },
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
