import { AtaqueDnd, AtributoDnd, GrauDnd, ItemDnd, MagiaDnd } from '../../ficha.model';

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

/** Capacidade de carga: Força x 7,5 kg. */
export function capacidadeDeCarga(forca: number): number {
  return forca * 7.5;
}

/** Peso de tudo o que a ficha carrega (peso x quantidade), arredondado a 2 casas para não mostrar ruído de ponto flutuante. As moedas não pesam. */
export function pesoTotal(itens: readonly ItemDnd[]): number {
  const soma = itens.reduce((total, item) => total + item.peso * item.quantidade, 0);
  return Math.round(soma * 100) / 100;
}

/** "112,5", "7" ou "0,25": pt-BR, até 2 casas e sem zeros à direita. */
export function formatarPeso(kg: number): string {
  return kg.toLocaleString('pt-BR', { maximumFractionDigits: 2 });
}

/** CD das magias: 8 + proficiência + modificador do atributo de conjuração; sem atributo não há CD (null). */
export function cdDeMagia(nivel: number, valorAtributo: number | null): number | null {
  return valorAtributo === null ? null : 8 + proficiencia(nivel) + modificador(valorAtributo);
}

/** Ataque mágico: proficiência + modificador do atributo de conjuração; sem atributo, null. */
export function ataqueMagico(nivel: number, valorAtributo: number | null): number | null {
  return valorAtributo === null ? null : proficiencia(nivel) + modificador(valorAtributo);
}

/** "Truque" para o nível 0; senão "3º nível". */
export function rotuloNivelMagia(nivel: number): string {
  return nivel === 0 ? 'Truque' : `${nivel}º nível`;
}

/** Por nível e, no mesmo nível, por criação (id): a mesma ordem da API, para a lista não pular ao editar. */
export function ordenarMagias(magias: readonly MagiaDnd[]): MagiaDnd[] {
  return [...magias].sort((a, b) => a.nivel - b.nivel || a.id - b.id);
}

/** Bônus do ataque: modificador do atributo + proficiência (se proficiente) + o bônus do próprio ataque. */
export function bonusDoAtaqueDnd(
  ataque: Pick<AtaqueDnd, 'atributo' | 'proficiente' | 'bonus'>,
  atributos: Record<AtributoDnd, number>,
  nivel: number,
): number {
  return modificador(atributos[ataque.atributo]) + (ataque.proficiente ? proficiencia(nivel) : 0) + ataque.bonus;
}
