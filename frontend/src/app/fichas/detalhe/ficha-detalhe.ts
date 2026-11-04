import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { ATRIBUTOS, PERICIAS, faixas } from '../catalogo';
import { AnelStatus } from '../componentes/anel-status';
import { Medalhao } from '../componentes/medalhao';
import { ResultadoRolagem } from '../componentes/resultado-rolagem';
import { FichaApi } from '../ficha-api';
import { CodigoPericia, Ficha, TestePericiaResultado } from '../ficha.model';

type Estado = 'carregando' | 'pronto' | 'nao-encontrada' | 'erro';

/** Onde fica a marca do sucesso extremo na barra da perícia (⌊valor/5⌋, em %). */
export function marcaExtremo(valor: number): number {
  return Math.min(faixas(Math.max(valor, 0)).extremo, 100);
}

/** Largura da barra da perícia: o próprio valor, limitado a 0–100 %. */
export function larguraBarra(valor: number): number {
  return Math.min(Math.max(valor, 0), 100);
}

@Component({
  selector: 'app-ficha-detalhe',
  imports: [RouterLink, MatMenuModule, MatTooltipModule, AnelStatus, Medalhao, ResultadoRolagem],
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
  readonly marcaExtremo = marcaExtremo;
  readonly larguraBarra = larguraBarra;

  readonly estado = signal<Estado>('carregando');
  readonly ficha = signal<Ficha | null>(null);
  readonly erro = signal<ApiError | null>(null);
  readonly ultimoResultado = signal<TestePericiaResultado | null>(null);
  readonly rolando = signal<CodigoPericia | null>(null);
  readonly confirmandoExclusao = signal(false);
  readonly excluindo = signal(false);

  constructor() {
    // Recarrega quando o id muda (navegar de uma ficha para outra reaproveita o componente).
    effect(() => {
      const id = Number(this.id());
      untracked(() => this.carregar(id));
    });
  }

  carregar(id: number): void {
    this.estado.set('carregando');
    this.erro.set(null);
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

  /** "Arthur · 34 anos · F": jogador, idade (se houver) e gênero. */
  legenda(f: Ficha): string {
    return [f.jogador, f.idade === null ? null : `${f.idade} anos`, f.genero].filter((p) => p !== null).join(' · ');
  }

  /** Perícia sem valor conta como 0, como na regra do servidor. */
  valorPericia(codigo: CodigoPericia): number {
    return this.ficha()?.pericias[codigo] ?? 0;
  }

  rolar(codigo: CodigoPericia): void {
    const ficha = this.ficha();
    if (!ficha || this.rolando()) {
      return;
    }
    this.rolando.set(codigo);
    this.erro.set(null);
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
    // Só libera o botão se der erro: no sucesso a tela navega, e liberar antes disso
    // permitiria um segundo DELETE durante a navegação.
    this.excluindo.set(true);
    this.api.excluir(ficha.id).subscribe({
      next: () => void this.router.navigate(['/fichas']),
      error: (erro: ApiError) => {
        this.excluindo.set(false);
        this.erro.set(erro);
      },
    });
  }
}
