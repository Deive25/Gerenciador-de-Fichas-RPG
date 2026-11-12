import { LADOS_DOS_DADOS, LadosDoDado } from '../ficha.model';

/**
 * A notação de dados ("2d6+1d4+3"), lida no navegador com a MESMA gramática do servidor (NotacaoDeDados).
 * O front só usa para mostrar o erro no campo, atualizar os contadores e desabilitar o "Rolar"; quem manda
 * é o servidor, que confere de novo.
 *
 * Gramática: termos ligados por `+` ou `-`, sem sinal no começo. Cada termo é `NdM` (N de 1 a 20; M em 4, 6,
 * 8, 10, 12, 20 ou 100) ou uma constante de 0 a 99. Espaços são ignorados e o `d` pode ser maiúsculo.
 * No máximo 8 termos e 20 dados somados; dado depois de `-` não vale (só as constantes subtraem).
 */
const MAXIMO_DE_TERMOS = 8;
const MAXIMO_DE_DADOS_SOMADOS = 20;
const MAXIMO_DE_DADOS_POR_TERMO = 20;
const MAXIMO_DA_CONSTANTE = 99;

/** Quantos dados de cada tipo: a forma que os contadores da aba Dados usam. */
export type Contagem = Record<LadosDoDado, number>;

export type NotacaoLida = { ok: true; contagem: Contagem; modificador: number } | { ok: false; erro: string };

export function contagemZerada(): Contagem {
  return { 4: 0, 6: 0, 8: 0, 10: 0, 12: 0, 20: 0, 100: 0 };
}

const falhou = (erro: string): NotacaoLida => ({ ok: false, erro });

export function lerNotacao(texto: string): NotacaoLida {
  const limpo = texto.replace(/\s+/g, '').toLowerCase();
  if (limpo === '') {
    return falhou('Digite os dados, por exemplo 2d6+3.');
  }
  if (/^[+-]/.test(limpo)) {
    return falhou('Comece pelo dado ou número, sem sinal.');
  }
  // "1d6+2-1" → ['1d6', '+', '2', '-', '1']: o termo, depois o sinal e o termo seguinte, e assim por diante.
  const pedacos = limpo.split(/([+-])/);
  if (pedacos.some((p) => p === '')) {
    return falhou('Falta um termo ao lado do sinal.');
  }
  const termos = pedacos.filter((_, i) => i % 2 === 0);
  if (termos.length > MAXIMO_DE_TERMOS) {
    return falhou(`Use no máximo ${MAXIMO_DE_TERMOS} termos.`);
  }

  const contagem = contagemZerada();
  let modificador = 0;
  let dadosSomados = 0;
  for (let i = 0; i < pedacos.length; i += 2) {
    const termo = pedacos[i];
    const subtrai = pedacos[i - 1] === '-';
    const dado = /^(\d*)d(\d*)$/.exec(termo);
    if (dado) {
      const [, quantidade, lados] = dado;
      if (quantidade === '') {
        return falhou('Escreva quantos dados antes do d, por exemplo 1d6.');
      }
      const n = Number(quantidade);
      if (n < 1 || n > MAXIMO_DE_DADOS_POR_TERMO) {
        return falhou(`Use de 1 a ${MAXIMO_DE_DADOS_POR_TERMO} dados em cada termo.`);
      }
      const m = Number(lados);
      if (!(LADOS_DOS_DADOS as readonly number[]).includes(m)) {
        return falhou(`Não existe d${lados}. Use d4, d6, d8, d10, d12, d20 ou d100.`);
      }
      if (subtrai) {
        return falhou('Dados não podem ser subtraídos; só números.');
      }
      dadosSomados += n;
      contagem[m as LadosDoDado] += n;
    } else if (/^\d+$/.test(termo)) {
      if (Number(termo) > MAXIMO_DA_CONSTANTE) {
        return falhou(`Use números de 0 a ${MAXIMO_DA_CONSTANTE}.`);
      }
      modificador += subtrai ? -Number(termo) : Number(termo);
    } else {
      return falhou(`Não entendi “${termo}”. Use termos como 2d6 ou 3, ligados por + ou -.`);
    }
  }
  if (dadosSomados > MAXIMO_DE_DADOS_SOMADOS) {
    return falhou(`Use no máximo ${MAXIMO_DE_DADOS_SOMADOS} dados no total.`);
  }
  return { ok: true, contagem, modificador };
}

/**
 * A forma canônica: os dados de menos lados primeiro ("1d4+2d6") e depois o modificador ("+3" ou "-1").
 * Sem dado, só um modificador positivo vira texto ("3"); senão fica vazio, porque não há o que rolar.
 */
export function escreverNotacao(contagem: Contagem, modificador: number): string {
  const termos = LADOS_DOS_DADOS.filter((lados) => contagem[lados] > 0).map((lados) => `${contagem[lados]}d${lados}`);
  if (termos.length === 0) {
    return modificador > 0 ? String(modificador) : '';
  }
  const texto = termos.join('+');
  if (modificador === 0) {
    return texto;
  }
  return `${texto}${modificador > 0 ? '+' : '-'}${Math.abs(modificador)}`;
}
