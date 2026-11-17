import { Injectable } from '@angular/core';

/** Só o que usamos de `google.accounts.id` (tipos à mão: sem instalar `@types/google.accounts`). */
export interface GoogleAccountsId {
  initialize(config: { client_id: string; callback: (resposta: { credential: string }) => void }): void;
  renderButton(elemento: HTMLElement, opcoes: Record<string, unknown>): void;
}

const URL_SCRIPT = 'https://accounts.google.com/gsi/client';

/**
 * Carrega o Google Identity Services sob demanda: o `<script>` só entra na página quando uma tela de
 * login pede, e uma vez só. As outras telas nunca baixam nada do Google.
 */
@Injectable({ providedIn: 'root' })
export class GoogleIdentity {
  private promessa: Promise<GoogleAccountsId> | null = null;

  carregar(): Promise<GoogleAccountsId> {
    this.promessa ??= new Promise<GoogleAccountsId>((resolve, reject) => {
      const script = document.createElement('script');
      script.src = URL_SCRIPT;
      script.async = true;
      script.onload = () => {
        const id = (window as unknown as { google?: { accounts?: { id?: GoogleAccountsId } } }).google?.accounts?.id;
        if (id) {
          resolve(id);
        } else {
          this.falhar(script, reject);
        }
      };
      script.onerror = () => this.falhar(script, reject);
      document.head.appendChild(script);
    });
    return this.promessa;
  }

  /** Remove o script e esquece a promessa, para uma próxima tela poder tentar de novo. */
  private falhar(script: HTMLScriptElement, rejeitar: (motivo: Error) => void): void {
    script.remove();
    this.promessa = null;
    rejeitar(new Error('Google Identity Services indisponível'));
  }
}
