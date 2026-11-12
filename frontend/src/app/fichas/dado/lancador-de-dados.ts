import { InjectionToken } from '@angular/core';
import { LadosDoDado } from '../ficha.model';

/**
 * Como o dado aparece: `mantido` (dourado) é o d20 que valeu no teste; `comum` (escuro) são os outros dados
 * rolados; `dano` (vinho) são os dados de dano de um ataque.
 */
export type CorDoDado = 'mantido' | 'comum' | 'dano';

/** Um dado a lançar: de quantos lados, o valor que o servidor sorteou (de 1 a `lados`) e a cor. */
export interface DadoLancado {
  lados: LadosDoDado;
  valor: number;
  cor: CorDoDado;
}

/** Lança os dados numa cena e avisa quando eles param. Quem usa não sabe se é 3D ou outra coisa. */
export interface LancadorDeDados {
  /**
   * Cria a cena dentro do `palco` e lança os `dados`, cada um parando no seu valor e com a sua cor.
   * Resolve quando todos param.
   */
  lancar(palco: HTMLElement, dados: DadoLancado[]): Promise<void>;
  /** Corta a animação e libera a cena (WebGL, física). Pode ser chamado a qualquer momento. */
  encerrar(): void;
}

/** O que o pedaço 3D (lancador-3d.ts) exporta. */
export interface ModuloDoLancador {
  criarLancador3d(): LancadorDeDados;
}

/**
 * Como baixar o pedaço 3D. O `import()` fica só aqui: a biblioteca e o three vão para um pedaço
 * separado, baixado na primeira rolagem. Os testes trocam este token por um carregador falso, e assim
 * nenhum teste chega a importar o lancador-3d.ts (o jsdom não tem WebGL).
 */
export const CARREGADOR_DO_LANCADOR = new InjectionToken<() => Promise<ModuloDoLancador>>('CARREGADOR_DO_LANCADOR', {
  providedIn: 'root',
  factory: () => () => import('./lancador-3d'),
});
