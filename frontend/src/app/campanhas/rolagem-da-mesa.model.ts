import { AtaqueResultado, RolagemLivre, TesteResultado } from '../fichas/ficha.model';
import { Referencia } from './campanha.model';

export type TipoDeRolagem = 'TESTE' | 'ATAQUE' | 'LIVRE';

/** Itens por página do histórico (o padrão da API). Página cheia = pode haver mais. */
export const TAMANHO_DA_PAGINA = 50;

interface RolagemBase {
  id: number;
  campanhaId: number;
  /** Instante ISO 8601 (UTC). */
  criadaEm: string;
  /** Quem apertou o botão (pode ser o mestre rolando com a ficha de um jogador). */
  autor: Referencia;
  /** Nulo quando a ficha é do mestre: o nome do NPC nunca chega ao navegador. */
  personagem: string | null;
  doMestre: boolean;
  rotulo: string;
  total: number;
}

/** Uma rolagem da mesa (spec do 2.4, §5). O `detalhe` é a resposta da rota, como foi devolvida a quem rolou. */
export type RolagemDaMesa = RolagemBase &
  (
    | { tipo: 'TESTE'; detalhe: TesteResultado }
    | { tipo: 'ATAQUE'; detalhe: AtaqueResultado }
    | { tipo: 'LIVRE'; detalhe: RolagemLivre }
  );

/**
 * Quem rolou, como a mesa lê: "Mestre" para a ficha do mestre; "{personagem} (rolado pelo mestre)" quando o mestre
 * rolou com a ficha de um jogador; senão "{autor} · {personagem}".
 */
export function quemRolou(rolagem: RolagemDaMesa, mestreId: number | null): string {
  if (rolagem.doMestre || rolagem.personagem === null) {
    return 'Mestre';
  }
  if (mestreId !== null && rolagem.autor.id === mestreId) {
    return `${rolagem.personagem} (rolado pelo mestre)`;
  }
  return `${rolagem.autor.nome} · ${rolagem.personagem}`;
}

/** "agora", "há N min", "há N h" e, depois de 24 h, a data. */
export function horaRelativa(criadaEm: string, agora: number): string {
  const minutos = Math.floor((agora - Date.parse(criadaEm)) / 60_000);
  if (minutos < 1) {
    return 'agora';
  }
  if (minutos < 60) {
    return `há ${minutos} min`;
  }
  const horas = Math.floor(minutos / 60);
  return horas < 24 ? `há ${horas} h` : new Date(criadaEm).toLocaleDateString('pt-BR');
}

/** Junta duas listas pelo `id` (a mesma rolagem pode vir do GET e do tópico) e ordena da mais nova para a mais antiga. */
export function juntarRolagens(atuais: RolagemDaMesa[], novas: RolagemDaMesa[]): RolagemDaMesa[] {
  const porId = new Map<number, RolagemDaMesa>();
  for (const r of [...atuais, ...novas]) {
    porId.set(r.id, r);
  }
  return [...porId.values()].sort((a, b) => b.id - a.id);
}
