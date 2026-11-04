import { Component, computed, input } from '@angular/core';

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
 * Retrato oval 4:5 com aro dourado. Sem imagem (até o 2.1c), mostra as iniciais sobre um
 * degradê. Fica fora da leitura de tela porque o nome já está escrito ao lado.
 */
@Component({
  selector: 'app-medalhao',
  template: '{{ sigla() }}',
  styleUrl: './medalhao.scss',
  host: {
    'aria-hidden': 'true',
    '[class.grande]': "tamanho() === 'grande'",
    '[style.--degrade]': 'degrade()',
  },
})
export class Medalhao {
  readonly nome = input.required<string>();
  readonly semente = input.required<number>();
  readonly tamanho = input<'pequeno' | 'grande'>('pequeno');

  readonly sigla = computed(() => iniciais(this.nome()));
  readonly degrade = computed(() => degradeDe(this.semente()));
}
