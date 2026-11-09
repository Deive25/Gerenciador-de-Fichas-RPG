import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/api-error';
import { FichaApi } from '../ficha-api';
import { Ficha } from '../ficha.model';
import { FormularioDnd } from '../sistemas/dnd/formulario-dnd';
import { FormularioOrdem } from '../sistemas/ordem/formulario-ordem';

type Estado = 'carregando' | 'pronto' | 'nao-encontrada' | 'erro';

/**
 * Rota /fichas/:id/editar: a rota é a mesma para qualquer sistema, então este componente
 * carrega a ficha e abre o formulário do sistema dela. PUT é substituição completa, por isso
 * nenhum formulário aparece antes de a ficha chegar (senão salvaria campos vazios por cima).
 */
@Component({
  selector: 'app-edicao-ficha',
  imports: [RouterLink, FormularioOrdem, FormularioDnd],
  templateUrl: './edicao-ficha.html',
  styleUrl: './edicao-ficha.scss',
})
export class EdicaoFicha {
  private readonly api = inject(FichaApi);

  /** Vem da rota /fichas/:id (withComponentInputBinding). */
  readonly id = input.required<string>();

  readonly estado = signal<Estado>('carregando');
  readonly ficha = signal<Ficha | null>(null);
  readonly erro = signal<ApiError | null>(null);

  constructor() {
    effect(() => {
      const id = Number(this.id());
      untracked(() => this.carregar(id));
    });
  }

  carregar(id: number): void {
    this.estado.set('carregando');
    this.erro.set(null);
    this.api.buscarFicha(id).subscribe({
      next: (ficha) => {
        this.ficha.set(ficha);
        this.estado.set('pronto');
      },
      error: (erro: ApiError) => {
        this.erro.set(erro);
        this.estado.set(erro.status === 404 ? 'nao-encontrada' : 'erro');
      },
    });
  }
}
