import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatTabsModule } from '@angular/material/tabs';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { ABAS_POR_SISTEMA, abaValida } from '../abas/abas-da-ficha';
import { FichaDndCorpo } from '../sistemas/dnd/ficha-dnd';
import { FichaOrdemCorpo } from '../sistemas/ordem/ficha-ordem';
import { DescricaoAba } from '../abas/descricao-aba';
import { HabilidadesAba } from '../abas/habilidades-aba';
import { InventarioOrdemAba } from '../sistemas/ordem/inventario-ordem-aba';
import { RecursosFicha } from './recursos-ficha';
import { Medalhao } from '../componentes/medalhao';
import { ResultadoRolagem } from '../componentes/resultado-rolagem';
import { FichaApi } from '../ficha-api';
import { DescricaoFicha, Ficha, Habilidade, ItemOrdem, PedidoTeste, TesteResultado } from '../ficha.model';
import { RetratoCache } from '../retrato-cache';
import { DadosRecorte, RecorteRetrato } from '../retrato/recorte-retrato';

type Estado = 'carregando' | 'pronto' | 'nao-encontrada' | 'erro';

@Component({
  selector: 'app-ficha-detalhe',
  imports: [
    RouterLink,
    MatMenuModule,
    MatTabsModule,
    Medalhao,
    RecursosFicha,
    HabilidadesAba,
    InventarioOrdemAba,
    DescricaoAba,
    ResultadoRolagem,
    FichaOrdemCorpo,
    FichaDndCorpo,
  ],
  templateUrl: './ficha-detalhe.html',
  styleUrl: './ficha-detalhe.scss',
})
export class FichaDetalhe {
  private readonly api = inject(FichaApi);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly retratos = inject(RetratoCache);

  /** Vem da rota /fichas/:id (withComponentInputBinding). */
  readonly id = input.required<string>();

  /** Vem do query param ?aba= (o mesmo binding de inputs): ficha, inventario, habilidades ou descricao. */
  readonly aba = input<string>();

  /** Abas do sistema da ficha carregada e o índice da aberta; aba ausente ou desconhecida é a Ficha (0). */
  readonly abas = computed(() => {
    const ficha = this.ficha();
    return ficha ? ABAS_POR_SISTEMA[ficha.sistema] : [];
  });
  readonly indiceDaAba = computed(() => {
    const ficha = this.ficha();
    if (!ficha) {
      return 0;
    }
    const codigo = abaValida(ficha.sistema, this.aba());
    return Math.max(0, this.abas().findIndex((a) => a.codigo === codigo));
  });

  readonly estado = signal<Estado>('carregando');
  readonly ficha = signal<Ficha | null>(null);
  readonly erro = signal<ApiError | null>(null);
  readonly ultimoResultado = signal<TesteResultado | null>(null);
  readonly rolando = signal<string | null>(null);
  readonly confirmandoExclusao = signal(false);
  readonly excluindo = signal(false);
  readonly confirmandoRemocaoRetrato = signal(false);
  readonly enviandoRetrato = signal(false);

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

  /** "Arthur · 34 anos · F": jogador, idade (se houver) e gênero. */
  legenda(f: Ficha): string {
    return [f.jogador, f.idade === null ? null : `${f.idade} anos`, f.genero].filter((p) => p !== null).join(' · ');
  }

  /** Troca de aba só na URL (replaceUrl: não enche o histórico), preservando os outros parâmetros. */
  trocarAba(indice: number): void {
    const aba = this.abas()[indice];
    if (!aba || indice === this.indiceDaAba()) {
      return;
    }
    void this.router.navigate([], { queryParams: { aba: aba.codigo }, queryParamsHandling: 'merge', replaceUrl: true });
  }

  /** A aba já gravou na API: só troca a lista na ficha, sem recarregar. */
  atualizarHabilidades(habilidades: Habilidade[]): void {
    this.ficha.update((f) => f && { ...f, habilidades });
  }

  /** Idem para os itens do inventário de Ordem. */
  atualizarItensOrdem(itens: ItemOrdem[]): void {
    this.ficha.update((f) => (f && f.sistema === 'ORDEM_PARANORMAL' ? { ...f, itens } : f));
  }

  /** Idem para a descrição: a aba já gravou, só troca na ficha. */
  atualizarDescricao(descricao: DescricaoFicha): void {
    this.ficha.update((f) => f && { ...f, descricao });
  }

  /** Rola o que o corpo da ficha pediu: o servidor faz os d20 e devolve cada dado. */
  rolar(pedido: PedidoTeste): void {
    const ficha = this.ficha();
    if (!ficha || this.rolando()) {
      return;
    }
    this.rolando.set(pedido.codigo);
    this.erro.set(null);
    this.api
      .testar(ficha.id, pedido)
      .pipe(finalize(() => this.rolando.set(null)))
      .subscribe({
        next: (resultado) => this.ultimoResultado.set(resultado),
        error: (erro: ApiError) => this.erro.set(erro),
      });
  }

  /** Arquivo escolhido → diálogo de recorte → envia o JPEG 400×500 → recarrega a ficha (nova versão). */
  aoEscolherArquivo(evento: Event): void {
    const seletor = evento.target as HTMLInputElement;
    const arquivo = seletor.files?.[0];
    seletor.value = ''; // permite escolher o mesmo arquivo de novo
    const ficha = this.ficha();
    if (!arquivo || !ficha) {
      return;
    }
    this.dialog
      .open<RecorteRetrato, DadosRecorte, Blob | undefined>(RecorteRetrato, { data: { arquivo } })
      .afterClosed()
      .subscribe((jpeg) => {
        if (jpeg) {
          this.enviarRetrato(ficha.id, jpeg);
        }
      });
  }

  removerRetrato(): void {
    const ficha = this.ficha();
    if (!ficha || this.enviandoRetrato()) {
      return;
    }
    this.enviandoRetrato.set(true);
    this.api
      .removerRetrato(ficha.id)
      .pipe(finalize(() => this.enviandoRetrato.set(false)))
      .subscribe({
        next: () => {
          this.retratos.esquecer(ficha.id);
          this.confirmandoRemocaoRetrato.set(false);
          this.carregar(ficha.id);
        },
        error: (erro: ApiError) => this.erro.set(erro),
      });
  }

  private enviarRetrato(fichaId: number, jpeg: Blob): void {
    this.enviandoRetrato.set(true);
    this.erro.set(null);
    this.api
      .enviarRetrato(fichaId, jpeg)
      .pipe(finalize(() => this.enviandoRetrato.set(false)))
      .subscribe({
        next: () => this.carregar(fichaId),
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
