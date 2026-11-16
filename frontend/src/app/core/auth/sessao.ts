import { Injectable, computed, signal } from '@angular/core';
import { RespostaAuth, Usuario } from './auth.model';

const CHAVE_REFRESH = 'fichas-rpg.refresh';

/**
 * Estado da sessão. O token de acesso (15 min) fica só na memória; o refresh (30 dias) fica no
 * localStorage para sobreviver ao recarregamento. Todo acesso ao localStorage passa por try/catch:
 * ele pode estar bloqueado (janela privada, política do navegador) e isso não pode quebrar a tela.
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

  refreshGuardado(): string | null {
    try {
      return localStorage.getItem(CHAVE_REFRESH);
    } catch {
      return null;
    }
  }

  iniciar(resposta: RespostaAuth): void {
    this.acesso = resposta.accessToken;
    this._usuario.set(resposta.usuario);
    try {
      localStorage.setItem(CHAVE_REFRESH, resposta.refreshToken);
    } catch {
      // Sem localStorage a sessão dura só até recarregar a página.
    }
  }

  limpar(): void {
    this.acesso = null;
    this._usuario.set(null);
    try {
      localStorage.removeItem(CHAVE_REFRESH);
    } catch {
      // Nada a apagar.
    }
  }
}
