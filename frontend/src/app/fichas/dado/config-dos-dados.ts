import { DadoLancado } from './lancador-de-dados';

/** Um dado para a `open-dice-dnd`: o tipo, o valor em que ele para e as cores da face e do número. */
export interface ConfigDoDado {
  dice: 'd4' | 'd6' | 'd8' | 'd10' | 'd12' | 'd20' | 'd100';
  rolled: number;
  backgroundColor: string;
  textColor: string;
}

/** Mais dados que isto na cena pesam demais no celular: vira o plano B (o resultado direto, sem camada). */
export const MAXIMO_DE_DADOS_NA_CENA = 20;

// Cores do tema (styles.scss). O que pinta a face é o backgroundColor; o diceColor só tinge a
// textura (laranja por padrão), por isso não é usado.
const CORES = {
  mantido: { backgroundColor: '#c9a45c', textColor: '#15110a' }, // --cor-acao / --cor-sobre-acao
  comum: { backgroundColor: '#2b2438', textColor: '#e6e1d6' }, // um tom acima do --cor-trilho / --cor-texto
  dano: { backgroundColor: '#5a2330', textColor: '#f0e2bf' }, // vinho, para o dano se separar do ataque
} as const;

/**
 * O valor que a biblioteca espera. O d10 da biblioteca vai de 0 a 9 (a face "0" é o 10 da mesa) e o d100
 * vai de 0 a 99 (o 100 da mesa é o "00" + "0"), então 10 e 100 viram 0. Os outros dados valem o que saiu.
 */
function valorParaABiblioteca(lados: number, valor: number): number {
  return (lados === 10 || lados === 100) && valor === lados ? 0 : valor;
}

/** O d100 é desenhado com dois d10, então conta como 2 dados na cena. */
export function pesoNaCena(dados: readonly Pick<DadoLancado, 'lados'>[]): number {
  return dados.reduce((soma, d) => soma + (d.lados === 100 ? 2 : 1), 0);
}

export function cabeNaCena(dados: readonly Pick<DadoLancado, 'lados'>[]): boolean {
  return pesoNaCena(dados) <= MAXIMO_DE_DADOS_NA_CENA;
}

/**
 * Lista de dados da biblioteca, na mesma ordem de `dados`, cada um parando exatamente no seu valor.
 * Função pura, sem importar a biblioteca, para testar sem WebGL.
 */
export function configDosDados(dados: readonly DadoLancado[]): ConfigDoDado[] {
  return dados.map((d) => ({
    dice: `d${d.lados}`,
    rolled: valorParaABiblioteca(d.lados, d.valor),
    ...CORES[d.cor],
  }));
}
