// A open-dice-dnd 1.13.0 não traz tipos. Aqui só o que o app usa (lancador-3d.ts).
declare module 'open-dice-dnd' {
  /** Um dado a lançar: `rolled` é o valor em que ele para; as cores pintam a face e o número. */
  export interface ConfigDeRolagem {
    dice: string;
    rolled: number;
    backgroundColor: string;
    textColor: string;
  }

  export class DiceRoller {
    /** O canvas é criado dentro do `container`, que precisa ter tamanho. */
    constructor(opcoes: { container: HTMLElement });
    /** Resolve quando os dados param. */
    roll(config: ConfigDeRolagem | ConfigDeRolagem[]): Promise<number>;
    /** Desmonta o WebGL, os listeners e a física. */
    destroy(): void;
  }
}
