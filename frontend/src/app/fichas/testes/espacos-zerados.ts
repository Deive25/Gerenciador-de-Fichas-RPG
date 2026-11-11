import { Espacos } from '../ficha.model';

/** Os 9 níveis sem espaços ({0, 0}), como a API devolve numa ficha nova; só para os testes. */
export function espacosZerados(): Espacos {
  const zero = () => ({ total: 0, usados: 0 });
  return { '1': zero(), '2': zero(), '3': zero(), '4': zero(), '5': zero(), '6': zero(), '7': zero(), '8': zero(), '9': zero() };
}
