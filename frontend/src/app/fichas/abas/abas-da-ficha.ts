import { Sistema } from '../ficha.model';

export type CodigoAba = 'ficha' | 'inventario' | 'habilidades' | 'descricao';

interface Aba {
  codigo: CodigoAba;
  rotulo: string;
}

const FICHA: Aba = { codigo: 'ficha', rotulo: 'Ficha' };
const INVENTARIO: Aba = { codigo: 'inventario', rotulo: 'Inventário' };
const HABILIDADES: Aba = { codigo: 'habilidades', rotulo: 'Habilidades' };
const DESCRICAO: Aba = { codigo: 'descricao', rotulo: 'Descrição' };

/** As abas de cada sistema. Por enquanto só Ordem tem o Inventário; o de D&D entra na tarefa seguinte. */
export const ABAS_POR_SISTEMA: Record<Sistema, readonly Aba[]> = {
  ORDEM_PARANORMAL: [FICHA, INVENTARIO, HABILIDADES, DESCRICAO],
  DND: [FICHA, HABILIDADES, DESCRICAO],
};

/** A aba pedida na URL; ausente ou desconhecida (ou de outro sistema) abre a Ficha. */
export function abaValida(sistema: Sistema, valor: string | undefined): CodigoAba {
  return ABAS_POR_SISTEMA[sistema].find((a) => a.codigo === valor)?.codigo ?? 'ficha';
}
