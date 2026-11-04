import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { RetratoCache } from '../retrato-cache';

/** Fundos das iniciais: tons escuros que combinam com o dourado do aro. */
export const DEGRADES = [
  'linear-gradient(160deg, #8a6436, #1f150b)',
  'linear-gradient(160deg, #3f6672, #0c1418)',
  'linear-gradient(160deg, #743434, #190b0c)',
  'linear-gradient(160deg, #4b3a6b, #120d1c)',
  'linear-gradient(160deg, #3d5a3a, #0d140c)',
  'linear-gradient(160deg, #5a5048, #14110e)',
] as const;

/** Primeira letra da primeira e da última palavra ("Prof. Hale" → "PH"); vazio vira "?". */
export function iniciais(nome: string): string {
  const palavras = nome
    .split(/\s+/)
    .map((p) => p.replace(/[^\p{L}\p{N}]/gu, ''))
    .filter((p) => p.length > 0);
  if (palavras.length === 0) {
    return '?';
  }
  const primeira = palavras[0][0];
  const ultima = palavras.length > 1 ? palavras[palavras.length - 1][0] : '';
  return (primeira + ultima).toUpperCase();
}

/** Escolhe o degradê pelo id: a mesma ficha tem sempre a mesma cor. */
export function degradeDe(semente: number): string {
  return DEGRADES[Math.abs(semente) % DEGRADES.length];
}

/**
 * Retrato oval 4:5 com aro dourado. Com retrato (2.1c), mostra a imagem, baixada pelo RetratoCache.
 * Sem retrato, ou se a imagem falhar, mostra as iniciais sobre um degradê; nesse caso fica fora
 * da leitura de tela, porque o nome já está escrito ao lado.
 */
@Component({
  selector: 'app-medalhao',
  template: `
    @if (mostrarFoto()) {
      <img [src]="urlFoto()" [alt]="'Retrato de ' + nome()" (error)="falhou.set(true)" />
    } @else {
      {{ sigla() }}
    }
  `,
  styleUrl: './medalhao.scss',
  host: {
    '[attr.aria-hidden]': "mostrarFoto() ? null : 'true'",
    '[class.grande]': "tamanho() === 'grande'",
    '[style.--degrade]': 'degrade()',
  },
})
export class Medalhao {
  private readonly cache = inject(RetratoCache);

  readonly nome = input.required<string>();
  /** Id da ficha: escolhe o degradê e identifica o retrato no cache. */
  readonly semente = input.required<number>();
  readonly tamanho = input<'pequeno' | 'grande'>('pequeno');
  /** retratoVersao da ficha; null = sem retrato. */
  readonly retratoVersao = input<number | null>(null);

  readonly sigla = computed(() => iniciais(this.nome()));
  readonly degrade = computed(() => degradeDe(this.semente()));

  readonly urlFoto = signal<string | null>(null);
  readonly falhou = signal(false);
  readonly mostrarFoto = computed(() => this.urlFoto() !== null && !this.falhou());

  constructor() {
    // Busca a URL local sempre que a ficha ou a versão mudam; cancela o pedido anterior.
    effect((aoLimpar) => {
      const fichaId = this.semente();
      const versao = this.retratoVersao();
      this.urlFoto.set(null);
      this.falhou.set(false);
      if (versao === null) {
        return;
      }
      const inscricao = this.cache.url(fichaId, versao).subscribe({
        next: (url) => this.urlFoto.set(url),
        error: () => this.falhou.set(true),
      });
      aoLimpar(() => inscricao.unsubscribe());
    });
  }
}
