import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { ATRIBUTOS, PERICIAS } from '../catalogo';
import { BarraStatus } from '../componentes/barra-status';
import { ResultadoRolagem } from '../componentes/resultado-rolagem';
import { FichaApi } from '../ficha-api';
import { CodigoPericia, Ficha, TestePericiaResultado } from '../ficha.model';

type Estado = 'carregando' | 'pronto' | 'nao-encontrada' | 'erro';

@Component({
  selector: 'app-ficha-detalhe',
  imports: [RouterLink, MatTabsModule, MatTooltipModule, BarraStatus, ResultadoRolagem],
  templateUrl: './ficha-detalhe.html',
  styleUrl: './ficha-detalhe.scss',
})
export class FichaDetalhe {
  private readonly api = inject(FichaApi);
  private readonly router = inject(Router);

  /** Vem da rota /fichas/:id (withComponentInputBinding). */
  readonly id = input.required<string>();

  readonly atributos = ATRIBUTOS;
  readonly pericias = PERICIAS;

  readonly estado = signal<Estado>('carregando');
  readonly ficha = signal<Ficha | null>(null);
  readonly erro = signal<ApiError | null>(null);
  readonly ultimoResultado = signal<TestePericiaResultado | null>(null);
  readonly rolando = signal<CodigoPericia | null>(null);
  readonly confirmandoExclusao = signal(false);
  readonly excluindo = signal(false);

  constructor() {
    // Recarrega quando o id muda (no computador, trocar de ficha reaproveita o componente).
    effect(() => {
      const id = Number(this.id());
      untracked(() => this.carregar(id));
    });
  }

  carregar(id: number): void {
    this.estado.set('carregando');
    this.ultimoResultado.set(null);
    this.confirmandoExclusao.set(false);
    this.api.buscar(id).subscribe({
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

  valorPericia(codigo: CodigoPericia): number | null {
    return this.ficha()?.pericias[codigo] ?? null;
  }

  rolar(codigo: CodigoPericia): void {
    const ficha = this.ficha();
    if (!ficha || this.rolando()) {
      return;
    }
    this.rolando.set(codigo);
    this.api
      .testarPericia(ficha.id, codigo)
      .pipe(finalize(() => this.rolando.set(null)))
      .subscribe({
        next: (resultado) => this.ultimoResultado.set(resultado),
        error: (erro: ApiError) => this.erro.set(erro),
      });
  }

  excluir(): void {
    const ficha = this.ficha();
    if (!ficha || this.excluindo()) {
      return;
    }
    this.excluindo.set(true);
    this.api
      .excluir(ficha.id)
      .pipe(finalize(() => this.excluindo.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/fichas']),
        error: (erro: ApiError) => this.erro.set(erro),
      });
  }
}
