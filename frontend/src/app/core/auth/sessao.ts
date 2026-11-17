import { Injectable, computed, signal } from '@angular/core';
import { RespostaAuth, Usuario } from './auth.model';

const CHAVE_REFRESH = 'fichas-rpg.refresh';

/**
 * Estado da sessão. O token de acesso (15 min) fica só na memória; o refresh (30 dias) fica no
 * navegador para sobreviver ao recarregamento:
 * - conta normal: `localStorage` (vale para todas as abas);
 * - conta demo: `sessionStorage` (vale só para a aba), o que deixa abrir o mestre e o jogador de
 *   demonstração no mesmo navegador sem que um troque o refresh do outro. Fechar a aba encerra a demo.
 * Só um dos dois guarda refresh por vez (iniciar apaga o outro). Todo acesso a storage passa por
 * try/catch: ele pode estar bloqueado (janela privada, política do navegador) e isso não pode quebrar a tela.
 */
@Injectable({ providedIn: 'root' })
export class Sessao {
  private readonly _usuario = signal<Usuario | null>(null);
  private acesso: string | null = null;

  readonly usuario = this._usuario.asReadonly();
  readonly estaLogado = computed(() => this._usuario() !== null);

  tokenDeAcesso(): string | null {
    return this.acesso;
  }

  /** O refresh da aba (demo) tem prioridade sobre o compartilhado (conta normal). */
  refreshGuardado(): string | null {
    return this.ler(() => sessionStorage) ?? this.ler(() => localStorage);
  }

  /** A sessão é demo? Em memória, vale a conta; logo após recarregar, vale o refresh guardado na aba. */
  ehDemo(): boolean {
    const usuario = this._usuario();
    return usuario ? usuario.demo === true : this.ler(() => sessionStorage) !== null;
  }

  /** `demo` diz onde guardar o refresh; quem renova passa `ehDemo()` para não trocar de tipo. */
  iniciar(resposta: RespostaAuth, demo = false): void {
    this.acesso = resposta.accessToken;
    this._usuario.set({ ...resposta.usuario, demo });
    this.gravar(() => (demo ? sessionStorage : localStorage), resposta.refreshToken);
    this.apagar(() => (demo ? localStorage : sessionStorage));
  }

  limpar(): void {
    this.acesso = null;
    this._usuario.set(null);
    this.apagar(() => localStorage);
    this.apagar(() => sessionStorage);
  }

  private ler(storage: () => Storage): string | null {
    try {
      return storage().getItem(CHAVE_REFRESH);
    } catch {
      return null;
    }
  }

  private gravar(storage: () => Storage, refresh: string): void {
    try {
      storage().setItem(CHAVE_REFRESH, refresh);
    } catch {
      // Sem storage a sessão dura só até recarregar a página.
    }
  }

  private apagar(storage: () => Storage): void {
    try {
      storage().removeItem(CHAVE_REFRESH);
    } catch {
      // Nada a apagar.
    }
  }
}
