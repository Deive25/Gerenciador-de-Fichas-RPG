import { Sistema } from '../ficha.model';

export type CodigoAba = 'ficha' | 'ataques' | 'dados' | 'inventario' | 'magias' | 'rituais' | 'habilidades' | 'descricao';

interface Aba {
  codigo: CodigoAba;
  rotulo: string;
}

const FICHA: Aba = { codigo: 'ficha', rotulo: 'Ficha' };
const ATAQUES: Aba = { codigo: 'ataques', rotulo: 'Ataques' };
const DADOS: Aba = { codigo: 'dados', rotulo: 'Dados' };
const INVENTARIO: Aba = { codigo: 'inventario', rotulo: 'Inventário' };
const MAGIAS: Aba = { codigo: 'magias', rotulo: 'Magias' };
const RITUAIS: Aba = { codigo: 'rituais', rotulo: 'Rituais' };
const HABILIDADES: Aba = { codigo: 'habilidades', rotulo: 'Habilidades' };
const DESCRICAO: Aba = { codigo: 'descricao', rotulo: 'Descrição' };

/** As abas de cada sistema: a de conjuração é Magias em D&D e Rituais em Ordem (cada uma só existe no seu sistema). */
export const ABAS_POR_SISTEMA: Record<Sistema, readonly Aba[]> = {
  ORDEM_PARANORMAL: [FICHA, ATAQUES, DADOS, INVENTARIO, RITUAIS, HABILIDADES, DESCRICAO],
  DND: [FICHA, ATAQUES, DADOS, INVENTARIO, MAGIAS, HABILIDADES, DESCRICAO],
};

/** A aba pedida na URL; ausente ou desconhecida (ou de outro sistema) abre a Ficha. */
export function abaValida(sistema: Sistema, valor: string | undefined): CodigoAba {
  return ABAS_POR_SISTEMA[sistema].find((a) => a.codigo === valor)?.codigo ?? 'ficha';
}
