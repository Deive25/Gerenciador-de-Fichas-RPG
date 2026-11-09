import { Routes } from '@angular/router';
import { Painel } from './fichas/painel/painel';

// Detalhe e formulários carregam sob demanda (spec §7): o Material de abas, dicas e campos
// fica fora do pacote inicial, que precisa abrir rápido no celular.
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'fichas' },
  // O painel é a tela de entrada: vai no pacote inicial para abrir sem uma segunda requisição.
  { path: 'fichas', component: Painel, title: 'Fichas · Fichas RPG' },
  // 'fichas/nova' antes de 'fichas/:id', senão "nova" seria lido como id.
  // Nova ficha começa pela escolha do sistema; cada sistema tem o seu formulário.
  {
    path: 'fichas/nova',
    loadComponent: () => import('./fichas/criacao/escolha-sistema').then((m) => m.EscolhaSistema),
    title: 'Nova ficha · Fichas RPG',
  },
  {
    path: 'fichas/nova/ordem-paranormal',
    loadComponent: () => import('./fichas/sistemas/ordem/formulario-ordem').then((m) => m.FormularioOrdem),
    title: 'Nova ficha de Ordem · Fichas RPG',
  },
  // data.edicao chega ao input `edicao` do formulário (withComponentInputBinding): uma tela para as duas edições.
  {
    path: 'fichas/nova/dnd-2014',
    loadComponent: () => import('./fichas/sistemas/dnd/formulario-dnd').then((m) => m.FormularioDnd),
    data: { edicao: 'D2014' },
    title: 'Nova ficha de D&D 5e · Fichas RPG',
  },
  {
    path: 'fichas/nova/dnd-2024',
    loadComponent: () => import('./fichas/sistemas/dnd/formulario-dnd').then((m) => m.FormularioDnd),
    data: { edicao: 'D2024' },
    title: 'Nova ficha de D&D 2024 · Fichas RPG',
  },
  {
    path: 'fichas/:id',
    loadComponent: () => import('./fichas/detalhe/ficha-detalhe').then((m) => m.FichaDetalhe),
    title: 'Ficha · Fichas RPG',
  },
  {
    // A rota é a mesma para qualquer sistema: a EdicaoFicha carrega a ficha e abre o formulário dela.
    path: 'fichas/:id/editar',
    loadComponent: () => import('./fichas/edicao/edicao-ficha').then((m) => m.EdicaoFicha),
    title: 'Editar ficha · Fichas RPG',
  },
  { path: '**', redirectTo: 'fichas' },
];
