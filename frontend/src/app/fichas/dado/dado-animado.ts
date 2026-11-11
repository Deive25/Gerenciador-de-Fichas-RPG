import { Injectable, inject } from '@angular/core';
import { CARREGADOR_DO_LANCADOR, LancadorDeDados, ModuloDoLancador } from './lancador-de-dados';

/**
 * Decide entre o dado 3D e o plano B (o resultado direto, sem camada). `preparar()` devolve um lançador
 * novo, ou `null` quando o 3D não é possível: movimento reduzido, sem WebGL ou falha ao baixar o pedaço.
 */
@Injectable({ providedIn: 'root' })
export class DadoAnimado {
  private readonly carregador = inject(CARREGADOR_DO_LANCADOR);
  /** O import() guardado: o pedaço 3D é baixado uma vez só, mesmo em várias rolagens. */
  private modulo: Promise<ModuloDoLancador> | null = null;

  async preparar(): Promise<LancadorDeDados | null> {
    if (this.movimentoReduzido() || !this.temWebGl()) {
      return null;
    }
    this.modulo ??= this.carregador();
    try {
      return (await this.modulo).criarLancador3d();
    } catch (erro) {
      this.modulo = null; // uma falha de rede não pode travar as próximas rolagens
      console.error('Não foi possível carregar o dado 3D; mostrando o resultado direto.', erro);
      return null;
    }
  }

  private movimentoReduzido(): boolean {
    return typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;
  }

  private temWebGl(): boolean {
    const canvas = document.createElement('canvas');
    return canvas.getContext('webgl2') !== null || canvas.getContext('webgl') !== null;
  }
}
