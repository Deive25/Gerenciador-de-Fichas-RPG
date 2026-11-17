import { Component, DestroyRef, effect, inject, input, output, signal, untracked } from '@angular/core';
import { Sessao } from '../core/auth/sessao';
import { MesaAoVivo } from './mesa-ao-vivo';
import { RolagemDaMesa, quemRolou } from './rolagem-da-mesa.model';

const DURACAO_MS = 4000;

/**
 * Aviso curto no rodapé para a rolagem de OUTRA pessoa que chega com o painel fechado ("Lía · Kai · Furtividade: 25").
 * Tocar nele pede para abrir o painel. É também ele que mantém a conexão ao vivo ativa enquanto a tela da
 * campanha (ou da ficha com campanha) está aberta: a inscrição existe mesmo sem o painel.
 */
@Component({
  selector: 'app-aviso-de-rolagem',
  templateUrl: './aviso-de-rolagem.html',
  styleUrl: './aviso-de-rolagem.scss',
})
export class AvisoDeRolagem {
  private readonly mesa = inject(MesaAoVivo);
  private readonly sessao = inject(Sessao);

  readonly campanhaId = input.required<number>();
  readonly painelAberto = input(false);
  readonly abrir = output<void>();

  protected readonly texto = signal<string | null>(null);
  private temporizador: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    effect((limpar) => {
      const id = this.campanhaId();
      this.esconder();
      const assinatura = untracked(() => this.mesa.rolagens$(id).subscribe((r) => this.chegou(r)));
      limpar(() => assinatura.unsubscribe());
    });
    // Com o painel aberto, a rolagem já está nele: o aviso que estava na tela sai.
    effect(() => {
      if (this.painelAberto()) {
        untracked(() => this.esconder());
      }
    });
    inject(DestroyRef).onDestroy(() => clearTimeout(this.temporizador));
  }

  private chegou(rolagem: RolagemDaMesa): void {
    if (this.painelAberto() || rolagem.autor.id === this.sessao.usuario()?.id) {
      return;
    }
    this.texto.set(`${quemRolou(rolagem, null)} · ${rolagem.rotulo}: ${rolagem.total}`);
    clearTimeout(this.temporizador);
    this.temporizador = setTimeout(() => this.texto.set(null), DURACAO_MS);
  }

  protected tocar(): void {
    this.esconder();
    this.abrir.emit();
  }

  private esconder(): void {
    clearTimeout(this.temporizador);
    this.texto.set(null);
  }
}
