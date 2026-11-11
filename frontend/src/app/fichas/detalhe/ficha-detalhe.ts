import { Component, DestroyRef, computed, effect, inject, input, signal, untracked } from '@angular/core';
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
import { InventarioDndAba } from '../sistemas/dnd/inventario-dnd-aba';
import { MagiasDndAba } from '../sistemas/dnd/magias-dnd-aba';
import { InventarioOrdemAba } from '../sistemas/ordem/inventario-ordem-aba';
import { RituaisOrdemAba } from '../sistemas/ordem/rituais-ordem-aba';
import { RecursosFicha } from './recursos-ficha';
import { Medalhao } from '../componentes/medalhao';
import { ResultadoRolagem } from '../componentes/resultado-rolagem';
import { CamadaDosDados } from '../dado/camada-dos-dados';
import { DadoAnimado } from '../dado/dado-animado';
import { LancadorDeDados } from '../dado/lancador-de-dados';
import { FichaApi } from '../ficha-api';
import {
  DescricaoFicha,
  Espacos,
  Ficha,
  Habilidade,
  ItemDnd,
  ItemOrdem,
  MagiaDnd,
  Moedas,
  PedidoTeste,
  RitualOrdem,
  TesteResultado,
} from '../ficha.model';
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
    InventarioDndAba,
    MagiasDndAba,
    RituaisOrdemAba,
    DescricaoAba,
    ResultadoRolagem,
    CamadaDosDados,
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
  private readonly dado = inject(DadoAnimado);

  /**
   * Conta as "vidas" da tela: sobe ao recarregar ou sair da ficha. Uma rolagem que volta depois disso
   * (resposta ou preparar() atrasados) é de outra ficha e é descartada.
   */
  private geracao = 0;

  /** Vem da rota /fichas/:id (withComponentInputBinding). */
  readonly id = input.required<string>();

  /** Vem do query param ?aba= (o mesmo binding de inputs): ficha, inventario, magias (D&D), rituais (Ordem), habilidades ou descricao. */
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
  /** Código do teste em andamento: da ida ao servidor até o cartão aparecer (a animação conta). */
  readonly rolando = signal<string | null>(null);
  /** Dados lançando na camada; o cartão desse resultado só aparece quando ela termina. */
  readonly animacao = signal<{ resultado: TesteResultado; lancador: LancadorDeDados; indiceMantido: number } | null>(null);
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
    inject(DestroyRef).onDestroy(() => this.geracao++);
  }

  carregar(id: number): void {
    this.estado.set('carregando');
    this.erro.set(null);
    this.geracao++;
    this.animacao.set(null); // a camada, ao sair, encerra a cena
    this.rolando.set(null);
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

  /** Idem para os itens do inventário de D&D. */
  atualizarItensDnd(itens: ItemDnd[]): void {
    this.ficha.update((f) => (f && f.sistema === 'DND' ? { ...f, itens } : f));
  }

  /** Idem para as moedas de D&D. */
  atualizarMoedas(moedas: Moedas): void {
    this.ficha.update((f) => (f && f.sistema === 'DND' ? { ...f, moedas } : f));
  }

  /** Idem para as magias de D&D. */
  atualizarMagias(magias: MagiaDnd[]): void {
    this.ficha.update((f) => (f && f.sistema === 'DND' ? { ...f, magias } : f));
  }

  /** Idem para os espaços de magia de D&D (os 9 níveis). */
  atualizarEspacos(espacos: Espacos): void {
    this.ficha.update((f) => (f && f.sistema === 'DND' ? { ...f, espacos } : f));
  }

  /** Idem para os rituais de Ordem. */
  atualizarRituais(rituais: RitualOrdem[]): void {
    this.ficha.update((f) => (f && f.sistema === 'ORDEM_PARANORMAL' ? { ...f, rituais } : f));
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
    const geracao = this.geracao;
    this.rolando.set(pedido.codigo);
    this.erro.set(null);
    this.api.testar(ficha.id, pedido).subscribe({
      next: (resultado) => void this.animar(resultado, geracao),
      error: (erro: ApiError) => {
        if (geracao === this.geracao) {
          this.rolando.set(null);
          this.erro.set(erro);
        }
      },
    });
  }

  /** Com o 3D disponível, abre a camada e deixa o cartão para quando ela terminar; senão, plano B. */
  private async animar(resultado: TesteResultado, geracao: number): Promise<void> {
    const lancador = await this.dado.preparar();
    if (geracao !== this.geracao) {
      lancador?.encerrar(); // saiu da ficha enquanto o pedaço 3D baixava
      return;
    }
    if (!lancador) {
      this.mostrar(resultado);
      return;
    }
    this.animacao.set({ resultado, lancador, indiceMantido: resultado.dados.indexOf(resultado.mantido) });
  }

  /** A camada terminou (dados parados, toque, Esc, limite ou falha): agora sim o cartão. */
  aoTerminarAnimacao(): void {
    const animacao = this.animacao();
    if (animacao) {
      this.animacao.set(null);
      this.mostrar(animacao.resultado);
    }
  }

  private mostrar(resultado: TesteResultado): void {
    this.ultimoResultado.set(resultado);
    this.rolando.set(null);
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
