import { AtaqueResultado, RolagemLivre, TesteResultado } from '../ficha.model';
import { DadoLancado } from './lancador-de-dados';

// Monta a lista de dados da cena a partir do que o servidor devolveu. Funções puras, sem a biblioteca 3D.

/**
 * Um d20 por valor, e só o do `indiceMantido` dourado. O índice é o do cartão (`dados.indexOf(mantido)`),
 * e assim, com valores repetidos, só um dado fica em destaque.
 */
export function d20sDoTeste(dados: number[], indiceMantido: number): DadoLancado[] {
  return dados.map((valor, i) => ({ lados: 20, valor, cor: i === indiceMantido ? 'mantido' : 'comum' }));
}

export function dadosDoTeste(r: TesteResultado): DadoLancado[] {
  return d20sDoTeste(r.dados, r.dados.indexOf(r.mantido));
}

/** Os d20 do ataque (o mantido dourado) seguidos dos dados do dano, em vinho. */
export function dadosDoAtaque(r: AtaqueResultado): DadoLancado[] {
  const d20 = d20sDoTeste(r.ataque.dados, r.ataque.dados.indexOf(r.ataque.mantido));
  const dano: DadoLancado[] = r.dano.dados.map((d) => ({ lados: d.lados, valor: d.valor, cor: 'dano' }));
  return [...d20, ...dano];
}

/** Na rolagem livre todos os dados são comuns (não há dado mantido). */
export function dadosDaRolagemLivre(r: RolagemLivre): DadoLancado[] {
  return r.dados.map((d) => ({ lados: d.lados, valor: d.valor, cor: 'comum' }));
}
