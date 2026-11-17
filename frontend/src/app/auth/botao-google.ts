import { Component, ElementRef, afterNextRender, inject, input, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { ApiError } from '../core/api-error';
import { AuthApi } from '../core/auth/auth-api';
import { GoogleIdentity } from '../core/auth/google-identity';
import { voltaSegura } from '../core/auth/guards';
import { Sessao } from '../core/auth/sessao';

/**
 * Botão oficial "Continuar com o Google" (não é nosso: a marca do Google exige o visual dele).
 * O `credential` que ele entrega é um ID token; a API o valida e devolve a sessão do J1.
 */
@Component({
  selector: 'app-botao-google',
  templateUrl: './botao-google.html',
  styleUrl: './botao-google.scss',
})
export class BotaoGoogle {
  private readonly google = inject(GoogleIdentity);
  private readonly api = inject(AuthApi);
  private readonly sessao = inject(Sessao);
  private readonly router = inject(Router);

  readonly volta = input<string | undefined>();

  private readonly alvo = viewChild.required<ElementRef<HTMLElement>>('alvo');

  protected readonly erro = signal<string | null>(null);
  protected readonly indisponivel = signal(false);
  private readonly enviando = signal(false);

  constructor() {
    afterNextRender(() => {
      this.google
        .carregar()
        .then((id) => {
          const elemento = this.alvo().nativeElement;
          id.initialize({ client_id: environment.googleClientId, callback: (r) => this.receber(r.credential) });
          id.renderButton(elemento, {
            theme: 'filled_black',
            text: 'continue_with',
            shape: 'pill',
            locale: 'pt-BR',
            // O Google só aceita de 200 a 400 px de largura.
            width: Math.min(400, Math.max(200, elemento.clientWidth)),
          });
        })
        .catch(() => this.indisponivel.set(true));
    });
  }

  private receber(idToken: string): void {
    // O callback pode disparar de novo (duplo clique, dois botões) com o primeiro pedido em voo.
    if (this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.erro.set(null);
    this.api.entrarComGoogle(idToken).subscribe({
      next: (resposta) => {
        this.sessao.iniciar(resposta);
        void this.router.navigateByUrl(voltaSegura(this.volta()));
      },
      error: (erro: ApiError) => {
        this.enviando.set(false);
        // Status 0 = sem resposta (rede); a mensagem longa do ApiError fala em "acordando", que aqui não cabe.
        this.erro.set(erro.status === 0 ? 'Não foi possível falar com o servidor.' : erro.detalhe || erro.titulo);
      },
    });
  }
}
