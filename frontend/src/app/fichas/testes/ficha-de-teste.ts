import { FichaOrdem } from '../ficha.model';

/** Uma ficha de Ordem completa e sem campanha, do usuário 1 (Arthur), para os testes das telas novas. */
export const FICHA_DE_TESTE: FichaOrdem = {
  id: 7, sistema: 'ORDEM_PARANORMAL', versao: 3, dono: { id: 1, nome: 'Arthur' }, campanha: null,
  jogador: 'Arthur', personagem: 'Detetive Lia', genero: 'F', idade: 34, retratoVersao: null,
  classe: 'OCULTISTA', origem: 'Acadêmica', nex: 10,
  atributos: { AGI: 3, FOR: 1, INT: 4, PRE: 2, VIG: 0 },
  recursos: { PV: { atual: 9, maximo: 12 }, SAN: { atual: 48, maximo: 60 }, PE: { atual: 5, maximo: 10 } },
  pericias: {},
  descricao: { aparencia: null, historia: null, anotacoes: null },
  habilidades: [],
  itens: [],
  rituais: [],
  ataques: [],
};
