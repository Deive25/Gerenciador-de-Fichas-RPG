/**
 * Espelho, no navegador, das regras 1, 3, 4 e 5 da política de senha do servidor (PoliticaDeSenha.java), só para
 * dar retorno enquanto a pessoa digita. A regra 6 (senhas comuns) fica só no servidor, e o servidor é quem decide.
 */
export interface RequisitoDaSenha {
  id: 'tamanho' | 'nome-email' | 'padrao';
  texto: string;
  ok: boolean;
}

export const MIN_CARACTERES_SENHA = 15;

const SEQUENCIAS = ['0123456789', 'abcdefghijklmnopqrstuvwxyz', 'qwertyuiopasdfghjklzxcvbnm'].flatMap((s) => {
  const inversa = Array.from(s).reverse().join('');
  return [s.repeat(3), inversa.repeat(3)];
});

/** Quantidade de caracteres como o servidor conta: code points (um emoji vale 1). */
export function tamanhoDaSenha(senha: string): number {
  return Array.from(senha).length;
}

function semAcentos(texto: string): string {
  return texto.normalize('NFD').replace(/\p{M}+/gu, '');
}

/** O texto é um mesmo trecho repetido 2 vezes ou mais? Menor período p = n - pi[n-1] (função de prefixo do KMP). */
function ehTrechoRepetido(texto: string): boolean {
  const s = Array.from(texto);
  const n = s.length;
  if (n === 0) {
    return false;
  }
  const pi = new Array<number>(n).fill(0);
  for (let i = 1; i < n; i++) {
    let k = pi[i - 1];
    while (k > 0 && s[i] !== s[k]) {
      k = pi[k - 1];
    }
    if (s[i] === s[k]) {
      k++;
    }
    pi[i] = k;
  }
  return n - pi[n - 1] <= n / 2;
}

function ehSequencia(minuscula: string): boolean {
  const semEspacos = minuscula.replace(/\s+/gu, '');
  return SEQUENCIAS.some((s) => s.includes(semEspacos));
}

function contemNomeOuEmail(senhaSimples: string, nome: string, email: string): boolean {
  const palavras = semAcentos(nome.toLowerCase()).split(/[^\p{L}]+/u);
  if (palavras.some((p) => p.length >= 4 && senhaSimples.includes(p))) {
    return true;
  }
  const minusculo = semAcentos(email.trim().toLowerCase());
  const arroba = minusculo.indexOf('@');
  const local = arroba >= 0 ? minusculo.slice(0, arroba) : minusculo;
  return local.length >= 4 && senhaSimples.includes(local);
}

export function requisitosDaSenha(senha: string, nome: string, email: string): RequisitoDaSenha[] {
  const minuscula = senha.toLowerCase();
  const simples = semAcentos(minuscula);
  return [
    { id: 'tamanho', texto: '15 caracteres ou mais', ok: tamanhoDaSenha(senha) >= MIN_CARACTERES_SENHA },
    { id: 'nome-email', texto: 'Sem o seu nome nem o seu e-mail', ok: !contemNomeOuEmail(simples, nome, email) },
    {
      id: 'padrao',
      texto: 'Sem sequências nem repetições (como 123456 ou abcabc)',
      ok: !ehTrechoRepetido(minuscula) && !ehSequencia(minuscula),
    },
  ];
}
