import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/api-error';
import { FichaApi } from '../ficha-api';
import { Ficha } from '../ficha.model';
import { CartaoFicha } from './cartao-ficha';
import { FiltroPainel, contar, filtrarFichas } from './regras-painel';

type Estado = 'carregando' | 'pronto' | 'erro';

/**
 * Rota /fichas: o painel com todas as fichas. Busca, filtros e contadores rodam no
 * navegador sobre o GET /fichas (spec do redesign, §3.2): a lista é pequena, então não há
 * motivo para uma rota nova na API.
 */
@Component({
  selector: 'app-painel',
  imports: [RouterLink, CartaoFicha],
  templateUrl: './painel.html',
  styleUrl: './painel.scss',
})
export class Painel {
  private readonly api = inject(FichaApi);

  readonly filtros: readonly { valor: FiltroPainel; rotulo: string }[] = [
    { valor: 'todos', rotulo: 'Todos' },
    { valor: 'feridos', rotulo: 'Feridos' },
    { valor: 'sanidade-baixa', rotulo: 'Sanidade baixa' },
  ];

  readonly estado = signal<Estado>('carregando');
  readonly fichas = signal<Ficha[]>([]);
  readonly erro = signal<ApiError | null>(null);
  readonly busca = signal('');
  readonly filtro = signal<FiltroPainel>('todos');

  readonly visiveis = computed(() => filtrarFichas(this.fichas(), this.busca(), this.filtro()));
  readonly contagem = computed(() => contar(this.fichas()));

  constructor() {
    // Carrega na criação e de novo sempre que alguma ficha é criada, editada ou excluída.
    effect(() => {
      this.api.versaoFichas();
      untracked(() => this.carregar());
    });
  }

  carregar(): void {
    this.estado.set('carregando');
    this.api.listar().subscribe({
      next: (fichas) => {
        this.fichas.set(fichas);
        this.estado.set('pronto');
      },
      error: (erro: ApiError) => {
        this.erro.set(erro);
        this.estado.set('erro');
      },
    });
  }

  aoBuscar(evento: Event): void {
    this.busca.set((evento.target as HTMLInputElement).value);
  }

  limparFiltros(): void {
    this.busca.set('');
    this.filtro.set('todos');
  }
}
