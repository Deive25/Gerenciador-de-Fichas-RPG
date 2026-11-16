import { DOCUMENT } from '@angular/common';
import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Observable, forkJoin } from 'rxjs';
import { ApiError } from '../core/api-error';
import { Sessao } from '../core/auth/sessao';
import { CartaoFicha } from '../fichas/painel/cartao-ficha';
import { FichaResumo } from '../fichas/ficha.model';
import { CampanhaApi } from './campanha-api';
import { Campanha, CampanhaEntrada, Membro, NOMES_PAPEL } from './campanha.model';
import { FormularioCampanha } from './formulario-campanha';

type Estado = 'carregando' | 'pronto' | 'nao-encontrada' | 'erro';

/** O que está esperando um "Sim" na confirmação em linha (uma por vez). */
type Confirmacao = { tipo: 'excluir' } | { tipo: 'sair' } | { tipo: 'renovar' } | { tipo: 'remover'; usuarioId: number };

/**
 * Rota /campanhas/:id: a mesa da campanha. O que aparece vem do `meuPapel` que a API devolve:
 * o mestre edita, convida e remove; o jogador só vê a mesa e pode sair. A API é quem decide de
 * verdade (403/404); esconder o botão aqui é só para a tela não oferecer o que daria erro.
 */
@Component({
  selector: 'app-campanha-tela',
  imports: [RouterLink, CartaoFicha, FormularioCampanha],
  templateUrl: './campanha-tela.html',
  styleUrl: './campanha-tela.scss',
})
export class CampanhaTela {
  private readonly api = inject(CampanhaApi);
  private readonly router = inject(Router);
  private readonly sessao = inject(Sessao);
  private readonly documento = inject(DOCUMENT);

  /** Vem da rota /campanhas/:id (withComponentInputBinding). */
  readonly id = input.required<string>();

  protected readonly papeis = NOMES_PAPEL;
  readonly estado = signal<Estado>('carregando');
  readonly campanha = signal<Campanha | null>(null);
  readonly membros = signal<Membro[]>([]);
  readonly fichas = signal<FichaResumo[]>([]);
  readonly erro = signal<ApiError | null>(null);

  readonly editando = signal(false);
  readonly confirmacao = signal<Confirmacao | null>(null);
  readonly ocupado = signal(false);
  /** Erro de uma ação (remover, sair, excluir, convite): vai para um alerta, sem derrubar a tela. */
  readonly erroAcao = signal<ApiError | null>(null);
  readonly erroAoEditar = signal<ApiError | null>(null);
  /** Resultado de "Copiar", lido pelos leitores de tela (aria-live). */
  readonly mensagemCopia = signal<string | null>(null);

  readonly ehMestre = computed(() => this.campanha()?.meuPapel === 'MESTRE');
  private readonly meuId = computed(() => this.sessao.usuario()?.id);
  /** O link completo do convite (só com o convite ativo). */
  readonly link = computed(() => {
    const convite = this.campanha()?.convite;
    return convite?.ativo ? `${this.documento.location.origin}/convite/${convite.codigo}` : null;
  });

  constructor() {
    effect(() => {
      const id = Number(this.id());
      untracked(() => this.carregar(id));
    });
  }

  carregar(id: number): void {
    this.estado.set('carregando');
    this.erro.set(null);
    this.editando.set(false);
    this.confirmacao.set(null);
    this.erroAcao.set(null);
    forkJoin({
      campanha: this.api.buscar(id),
      membros: this.api.membros(id),
      fichas: this.api.fichas(id),
    }).subscribe({
      next: ({ campanha, membros, fichas }) => {
        this.campanha.set(campanha);
        this.membros.set(membros);
        this.fichas.set(fichas);
        this.estado.set('pronto');
      },
      error: (erro: ApiError) => {
        this.erro.set(erro);
        this.estado.set(erro.status === 404 ? 'nao-encontrada' : 'erro');
      },
    });
  }

  /** O dono abre a própria ficha e o mestre abre todas; a de outro jogador é só o resumo. */
  podeAbrir(ficha: FichaResumo): boolean {
    return this.ehMestre() || ficha.dono.id === this.meuId();
  }

  podeRemover(membro: Membro): boolean {
    return this.ehMestre() && membro.papel === 'JOGADOR';
  }

  removendo(membro: Membro): boolean {
    const c = this.confirmacao();
    return c?.tipo === 'remover' && c.usuarioId === membro.usuarioId;
  }

  nomeDe(usuarioId: number): string {
    return this.membros().find((m) => m.usuarioId === usuarioId)?.nome ?? '';
  }

  // ---- Editar e excluir a campanha (mestre) ----

  salvar(entrada: CampanhaEntrada): void {
    const campanha = this.campanha();
    if (!campanha || this.ocupado()) {
      return;
    }
    this.erroAoEditar.set(null);
    this.ocupado.set(true);
    this.api.atualizar(campanha.id, entrada).subscribe({
      next: (atualizada) => {
        this.ocupado.set(false);
        this.campanha.set(atualizada);
        this.editando.set(false);
      },
      error: (erro: ApiError) => {
        this.ocupado.set(false);
        this.erroAoEditar.set(erro);
      },
    });
  }

  excluir(): void {
    const campanha = this.campanha();
    if (!campanha) {
      return;
    }
    // Só libera o botão se der erro: no sucesso a tela navega.
    this.executar(this.api.excluir(campanha.id), () => void this.router.navigate(['/campanhas']), false);
  }

  // ---- Membros ----

  remover(usuarioId: number): void {
    const campanha = this.campanha();
    if (!campanha) {
      return;
    }
    this.executar(this.api.removerMembro(campanha.id, usuarioId), () => {
      this.confirmacao.set(null);
      // As fichas dele saíram da mesa: recarrega a lista de membros e a das fichas.
      forkJoin({ membros: this.api.membros(campanha.id), fichas: this.api.fichas(campanha.id) }).subscribe({
        next: ({ membros, fichas }) => {
          this.membros.set(membros);
          this.fichas.set(fichas);
        },
        error: (erro: ApiError) => this.erroAcao.set(erro),
      });
    });
  }

  sair(): void {
    const campanha = this.campanha();
    const meuId = this.meuId();
    if (!campanha || meuId === undefined) {
      return;
    }
    this.executar(this.api.removerMembro(campanha.id, meuId), () => void this.router.navigate(['/campanhas']), false);
  }

  // ---- Convite (mestre) ----

  /** Renova o link (o antigo para de funcionar) ou gera um, se estava desativado. */
  renovarConvite(): void {
    const campanha = this.campanha();
    if (!campanha) {
      return;
    }
    this.mensagemCopia.set(null);
    this.executar(this.api.renovarConvite(campanha.id), (convite) => {
      this.confirmacao.set(null);
      this.campanha.update((c) => c && { ...c, convite });
    });
  }

  desativarConvite(): void {
    const campanha = this.campanha();
    if (!campanha) {
      return;
    }
    this.mensagemCopia.set(null);
    this.executar(this.api.desativarConvite(campanha.id), () => {
      this.campanha.update((c) => c && c.convite && { ...c, convite: { ...c.convite, ativo: false } });
    });
  }

  async copiar(): Promise<void> {
    const link = this.link();
    if (!link) {
      return;
    }
    try {
      await navigator.clipboard.writeText(link);
      this.mensagemCopia.set('Link copiado');
    } catch {
      // Sem permissão ou sem HTTPS: o link já está num campo selecionável, a pessoa copia à mão.
      this.mensagemCopia.set('Não foi possível copiar. Selecione o link e copie.');
    }
  }

  protected selecionar(evento: Event): void {
    (evento.target as HTMLInputElement).select();
  }

  /** Roda uma ação da API com o botão travado; no erro mostra o alerta, no sucesso chama `depois`. */
  private executar<T>(
    requisicao: Observable<T>,
    depois: (resposta: T) => void,
    liberarNoSucesso = true,
  ): void {
    if (this.ocupado()) {
      return;
    }
    this.erroAcao.set(null);
    this.ocupado.set(true);
    requisicao.subscribe({
      next: (resposta) => {
        if (liberarNoSucesso) {
          this.ocupado.set(false);
        }
        depois(resposta);
      },
      error: (erro: ApiError) => {
        this.ocupado.set(false);
        this.erroAcao.set(erro);
      },
    });
  }
}
