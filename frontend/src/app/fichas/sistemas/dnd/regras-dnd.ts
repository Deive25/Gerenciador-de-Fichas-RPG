import { GrauDnd } from '../../ficha.model';

// As mesmas regras do backend (RegrasDnd). O front calcula para mostrar; quem rola é o servidor.

/** Math.floor, não Math.trunc: o valor 9 dá −1 (trunc daria 0, arredondando para zero). */
export function modificador(valor: number): number {
  return Math.floor((valor - 10) / 2);
}

export function proficiencia(nivel: number): number {
  return 2 + Math.floor((nivel - 1) / 4);
}

export function bonusPericia(valorAtributo: number, nivel: number, grau?: GrauDnd): number {
  const mod = modificador(valorAtributo);
  if (grau === 'ESPECIALISTA') {
    return mod + 2 * proficiencia(nivel);
  }
  return grau === 'PROFICIENTE' ? mod + proficiencia(nivel) : mod;
}

export function bonusSalvaguarda(valorAtributo: number, nivel: number, proficiente: boolean): number {
  return modificador(valorAtributo) + (proficiente ? proficiencia(nivel) : 0);
}

/** "+2", "+0" ou "−1" (sinal de menos U+2212, não o hífen). */
export function comSinal(n: number): string {
  return n < 0 ? `−${-n}` : `+${n}`;
}
