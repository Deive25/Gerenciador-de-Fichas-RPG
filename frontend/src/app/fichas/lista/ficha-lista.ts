import { Component, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ApiError } from '../../core/api-error';
import { BarraStatus } from '../componentes/barra-status';
import { FichaApi } from '../ficha-api';
import { Ficha } from '../ficha.model';

type Estado = 'carregando' | 'pronto' | 'erro';

@Component({
  selector: 'app-ficha-lista',
  imports: [RouterLink, RouterLinkActive, BarraStatus],
  templateUrl: './ficha-lista.html',
  styleUrl: './ficha-lista.scss',
})
export class FichaLista {
  private readonly api = inject(FichaApi);

  readonly estado = signal<Estado>('carregando');
  readonly fichas = signal<Ficha[]>([]);
  readonly erro = signal<ApiError | null>(null);

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
}
