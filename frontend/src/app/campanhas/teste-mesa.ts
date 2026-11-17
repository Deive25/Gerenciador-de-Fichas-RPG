import { signal } from '@angular/core';
import { Observable, Subject, of } from 'rxjs';
import { EstadoDaMesa } from './mesa-ao-vivo';
import { RolagemDaMesa } from './rolagem-da-mesa.model';

/**
 * A mesa ao vivo de mentira, para os testes das telas: nenhum WebSocket de verdade. `emitir` entrega
 * uma rolagem a quem está inscrito na campanha; `inscritos` diz de quais campanhas há inscrição.
 */
export function mesaFalsa() {
  const vivas = new Map<number, Set<Subject<RolagemDaMesa>>>();
  return {
    estado: signal<EstadoDaMesa>('conectado'),
    rolagens$: (campanhaId: number) =>
      new Observable<RolagemDaMesa>((assinante) => {
        const s = new Subject<RolagemDaMesa>();
        const conjunto = vivas.get(campanhaId) ?? new Set();
        conjunto.add(s);
        vivas.set(campanhaId, conjunto);
        const sub = s.subscribe(assinante);
        return () => {
          sub.unsubscribe();
          conjunto.delete(s);
        };
      }),
    emitir: (campanhaId: number, rolagem: RolagemDaMesa) => vivas.get(campanhaId)?.forEach((s) => s.next(rolagem)),
    inscritos: (campanhaId: number) => vivas.get(campanhaId)?.size ?? 0,
  };
}

/** O histórico de mentira: vazio. */
export const historicoVazio = { historico: () => of([] as RolagemDaMesa[]) };
