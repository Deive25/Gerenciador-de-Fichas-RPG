import { DOCUMENT } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthApi } from '../core/auth/auth-api';
import { Sessao } from '../core/auth/sessao';
import { FaixaDoServidor } from '../core/servidor/faixa-do-servidor';

/**
 * Esqueleto da aplicação: topo fixo (marca; logado, também os links "Fichas" e "Campanhas", "Nova ficha", o nome e "Sair") e uma
 * tela por vez, igual no celular e no computador (spec do redesign, §3.1).
 *
 * O <router-outlet> é único e não fica dentro de nenhum @if de largura: girar o aparelho
 * ou redimensionar a janela nunca recria a tela aberta (nem perde o que foi digitado).
 */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, FaixaDoServidor],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  protected readonly sessao = inject(Sessao);
  private readonly api = inject(AuthApi);
  private readonly documento = inject(DOCUMENT);
  protected readonly saindo = signal(false);

  /**
   * Avisa a API (para o refresh deixar de valer), apaga a sessão local e faz uma navegação COMPLETA
   * para /entrar: recarregar a página zera todo o estado em memória (lista e retratos em cache),
   * o que é mais seguro do que limpar cache por cache quando outra pessoa usa o mesmo aparelho.
   * Se a API falhar, sai do mesmo jeito: a sessão local sempre cai.
   */
  protected sair(): void {
    if (this.saindo()) {
      return;
    }
    this.saindo.set(true);
    const concluir = () => {
      this.sessao.limpar();
      this.documento.location.assign('/entrar');
    };
    const refresh = this.sessao.refreshGuardado();
    if (!refresh) {
      concluir();
      return;
    }
    this.api.sair(refresh).subscribe({ next: concluir, error: concluir });
  }
}
