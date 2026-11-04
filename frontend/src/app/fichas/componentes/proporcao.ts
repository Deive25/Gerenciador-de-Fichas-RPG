/**
 * Percentual inteiro (0–100) de `atual` sobre `maximo`, usado pelas barras e pelos anéis
 * de vida e sanidade. Sem máximo (ou com máximo ≤ 0) não há proporção: devolve null e quem
 * desenha mostra "—". Atual ausente conta como 0; fora da faixa, fica limitado a 0–100
 * (vida acima do máximo existe na mesa, mas a barra não passa de cheia).
 */
export function proporcao(atual: number | null, maximo: number | null): number | null {
  if (maximo === null || maximo <= 0) {
    return null;
  }
  const percentual = Math.round(((atual ?? 0) / maximo) * 100);
  return Math.min(100, Math.max(0, percentual));
}
