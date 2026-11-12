/**
 * "19 + 5", "7 − 1" (sinal de menos U+2212, e não "7 + -1") ou só "12" quando o bônus é 0.
 * Usado pelos cartões de resultado para escrever a conta da rolagem.
 */
export function contaComBonus(base: string | number, bonus: number): string {
  if (bonus === 0) {
    return String(base);
  }
  return bonus < 0 ? `${base} − ${-bonus}` : `${base} + ${bonus}`;
}
