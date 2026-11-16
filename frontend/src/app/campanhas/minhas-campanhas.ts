import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiError } from '../core/api-error';
import { CampanhaApi } from './campanha-api';
import { CampanhaEntrada, CampanhaResumo, NOMES_PAPEL } from './campanha.model';
import { FormularioCampanha } from './formulario-campanha';

type Estado = 'carregando' | 'pronto' | 'erro';

/** Rota /campanhas: as campanhas de que a pessoa participa, e a criação de uma nova. */
@Component({
  selector: 'app-minhas-campanhas',
  imports: [RouterLink, FormularioCampanha],
  templateUrl: './minhas-campanhas.html',
  styleUrl: './minhas-campanhas.scss',
})
export class MinhasCampanhas {
  private readonly api = inject(CampanhaApi);
  private readonly router = inject(Router);

  protected readonly papeis = NOMES_PAPEL;
  readonly estado = signal<Estado>('carregando');
  readonly campanhas = signal<CampanhaResumo[]>([]);
  readonly erro = signal<ApiError | null>(null);
  readonly criando = signal(false);
  readonly salvando = signal(false);
  readonly erroAoCriar = signal<ApiError | null>(null);

  constructor() {
    this.carregar();
  }

  carregar(): void {
    this.estado.set('carregando');
    this.api.listar().subscribe({
      next: (campanhas) => {
        this.campanhas.set(campanhas);
        this.estado.set('pronto');
      },
      error: (erro: ApiError) => {
        this.erro.set(erro);
        this.estado.set('erro');
      },
    });
  }

  abrirFormulario(): void {
    this.erroAoCriar.set(null);
    this.criando.set(true);
  }

  criar(entrada: CampanhaEntrada): void {
    if (this.salvando()) {
      return;
    }
    this.erroAoCriar.set(null);
    this.salvando.set(true);
    // Só libera o botão se der erro: no sucesso a tela navega (evita campanha duplicada por duplo toque).
    this.api.criar(entrada).subscribe({
      next: (id) => void this.router.navigate(['/campanhas', id]),
      error: (erro: ApiError) => {
        this.salvando.set(false);
        this.erroAoCriar.set(erro);
      },
    });
  }

  textoFichas(quantidade: number): string {
    return quantidade === 1 ? '1 ficha' : `${quantidade} fichas`;
  }
}
