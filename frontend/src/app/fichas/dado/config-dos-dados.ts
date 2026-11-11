/** Um d20 para a `open-dice-dnd`: o valor que o servidor sorteou e as cores da face e do número. */
export interface ConfigDoDado {
  dice: 'd20';
  rolled: number;
  backgroundColor: string;
  textColor: string;
}

// Cores do tema (styles.scss). O que pinta a face do d20 é o backgroundColor; o diceColor só tinge a
// textura (laranja por padrão), por isso não é usado.
const MANTIDO = { backgroundColor: '#c9a45c', textColor: '#15110a' }; // --cor-acao / --cor-sobre-acao
const OUTRO = { backgroundColor: '#2b2438', textColor: '#e6e1d6' }; // um tom acima do --cor-trilho / --cor-texto

/**
 * Lista de dados da biblioteca: um d20 por valor de `dados`, parando exatamente nesse valor.
 * Só o dado do índice do mantido fica dourado (o mesmo índice do cartão: `dados.indexOf(mantido)`).
 * Função pura, sem importar a biblioteca, para testar sem WebGL.
 */
export function configDosDados(dados: number[], indiceMantido: number): ConfigDoDado[] {
  return dados.map((valor, i) => ({
    dice: 'd20',
    rolled: valor,
    ...(i === indiceMantido ? MANTIDO : OUTRO),
  }));
}
