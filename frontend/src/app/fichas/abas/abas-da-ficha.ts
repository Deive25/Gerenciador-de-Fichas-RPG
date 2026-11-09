import { Sistema } from '../ficha.model';

export type CodigoAba = 'ficha' | 'habilidades' | 'descricao';

interface Aba {
  codigo: CodigoAba;
  rotulo: string;
}

const ABAS: readonly Aba[] = [
  { codigo: 'ficha', rotulo: 'Ficha' },
  { codigo: 'habilidades', rotulo: 'Habilidades' },
  { codigo: 'descricao', rotulo: 'Descrição' },
];

/** Hoje os dois sistemas têm as mesmas abas; o mapa existe para o 2.1h+ acrescentar abas por sistema. */
export const ABAS_POR_SISTEMA: Record<Sistema, readonly Aba[]> = {
  ORDEM_PARANORMAL: ABAS,
  DND: ABAS,
};

/** A aba pedida na URL; ausente ou desconhecida (ou de outro sistema) abre a Ficha. */
export function abaValida(sistema: Sistema, valor: string | undefined): CodigoAba {
  return ABAS_POR_SISTEMA[sistema].find((a) => a.codigo === valor)?.codigo ?? 'ficha';
}
