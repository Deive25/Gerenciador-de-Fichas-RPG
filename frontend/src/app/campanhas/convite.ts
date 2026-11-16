import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiError } from '../core/api-error';
import { Renovacao } from '../core/auth/renovacao';
import { Sessao } from '../core/auth/sessao';
import { CampanhaApi } from './campanha-api';
import { PreviaConvite } from './campanha.model';

type Estado = 'carregando' | 'pronto' | 'invalido' | 'erro';

/**
 * Rota /convite/:codigo, FORA dos guards de sessão: quem chega pelo link ainda pode não ter conta.
 * A prévia (campanha e mestre) é pública. Sem sessão, "Entrar" e "Criar conta" voltam para cá pelo
 * `?volta=` do 2.2; com sessão, "Entrar na campanha" aceita o convite.
 */
@Component({
  selector: 'app-convite',
  imports: [RouterLink],
  templateUrl: './convite.html',
  styleUrl: './convite.scss',
})
export class ConviteTela {
  private readonly api = inject(CampanhaApi);
  private readonly sessao = inject(Sessao);
  private readonly renovacao = inject(Renovacao);
  private readonly router = inject(Router);

  /** Vem da rota /convite/:codigo (withComponentInputBinding). */
  readonly codigo = input.required<string>();

  readonly estado = signal<Estado>('carregando');
  readonly previa = signal<PreviaConvite | null>(null);
  readonly erro = signal<ApiError | null>(null);
  readonly jaMembro = signal(false);
  readonly aceitando = signal(false);
  readonly erroAoAceitar = signal<ApiError | null>(null);

  readonly logado = this.sessao.estaLogado;
  /** Para onde /entrar e /cadastrar mandam a pessoa depois de entrar: de volta a este convite. */
  readonly volta = computed(() => ({ volta: `/convite/${this.codigo()}` }));

  /** Conta as cargas: uma resposta de um código antigo (a URL mudou no meio) é descartada. */
  private geracao = 0;

  constructor() {
    effect(() => {
      const codigo = this.codigo();
      untracked(() => void this.carregar(codigo));
    });
  }

  async carregar(codigo: string): Promise<void> {
    const geracao = ++this.geracao;
    this.estado.set('carregando');
    this.erro.set(null);
    this.erroAoAceitar.set(null);
    this.jaMembro.set(false);
    // Recarregou a página: o token de acesso (só na memória) sumiu, mas o refresh guardado ainda pode
    // restaurar a sessão. Sem isso, quem já tem conta veria "Entrar" em vez de "Entrar na campanha".
    if (!this.sessao.estaLogado()) {
      await this.renovacao.renovar();
    }
    if (geracao !== this.geracao) {
      return;
    }
    this.api.previa(codigo).subscribe({
      next: (previa) => {
        if (geracao !== this.geracao) {
          return;
        }
        this.previa.set(previa);
        this.estado.set('pronto');
        this.verificarSeJaEMembro(previa.campanhaId, geracao);
      },
      error: (erro: ApiError) => {
        if (geracao !== this.geracao) {
          return;
        }
        this.erro.set(erro);
        this.estado.set(erro.status === 404 ? 'invalido' : 'erro');
      },
    });
  }

  private verificarSeJaEMembro(campanhaId: number, geracao: number): void {
    if (!this.sessao.estaLogado()) {
      return;
    }
    this.api.listar().subscribe({
      next: (campanhas) => {
        if (geracao === this.geracao) {
          this.jaMembro.set(campanhas.some((c) => c.id === campanhaId));
        }
      },
      // Sem a lista, a tela só não avisa; aceitar de quem já é membro responde 200 e abre a campanha.
      error: () => undefined,
    });
  }

  aceitar(): void {
    if (this.aceitando()) {
      return;
    }
    this.erroAoAceitar.set(null);
    this.aceitando.set(true);
    // Só libera o botão se der erro: no sucesso a tela navega.
    this.api.aceitar(this.codigo()).subscribe({
      next: ({ campanhaId }) => void this.router.navigate(['/campanhas', campanhaId]),
      error: (erro: ApiError) => {
        this.aceitando.set(false);
        this.erroAoAceitar.set(erro);
      },
    });
  }
}
