import { DiceRoller } from 'open-dice-dnd';
import { configDosDados } from './config-dos-dados';
import { LancadorDeDados } from './lancador-de-dados';

/**
 * O único arquivo que importa a open-dice-dnd (e, por ela, o three e o cannon-es): só é carregado, por
 * `import()`, na primeira rolagem. Não roda no jsdom (sem WebGL), então os testes nunca o importam.
 * A cena é criada a cada rolagem e destruída no fim, para não deixar contexto WebGL aberto.
 */
class Lancador3d implements LancadorDeDados {
  private roller: DiceRoller | null = null;
  private encerrado = false;

  async lancar(palco: HTMLElement, dados: number[], indiceMantido: number): Promise<void> {
    if (this.encerrado) {
      return;
    }
    this.roller = new DiceRoller({ container: palco });
    await this.roller.roll(configDosDados(dados, indiceMantido));
  }

  encerrar(): void {
    this.encerrado = true;
    this.roller?.destroy();
    this.roller = null;
  }
}

export function criarLancador3d(): LancadorDeDados {
  return new Lancador3d();
}
