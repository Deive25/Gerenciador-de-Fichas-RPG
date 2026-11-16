import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiError } from '../api-error';
import { AuthApi } from './auth-api';
import { ResultadoRenovacao } from './auth.model';
import { Sessao } from './sessao';

const NOME_DA_TRAVA = 'fichas-rpg-refresh';

/**
 * Troca o refresh por um par novo. O servidor rotaciona o refresh a cada uso e derruba a sessão se
 * um refresh já usado for reapresentado; por isso duas renovações nunca podem usar o mesmo refresh:
 * - na mesma aba, as chamadas que chegam juntas dividem uma promessa só;
 * - entre abas, uma trava (Web Locks) as põe em fila, e cada uma relê o refresh do localStorage
 *   já dentro da trava, pegando o que a outra aba acabou de gravar.
 */
@Injectable({ providedIn: 'root' })
export class Renovacao {
  private readonly api = inject(AuthApi);
  private readonly sessao = inject(Sessao);
  private emAndamento: Promise<ResultadoRenovacao> | null = null;

  renovar(): Promise<ResultadoRenovacao> {
    if (!this.emAndamento) {
      this.emAndamento = this.comTrava(() => this.trocar()).finally(() => {
        this.emAndamento = null;
      });
    }
    return this.emAndamento;
  }

  private comTrava<T>(tarefa: () => Promise<T>): Promise<T> {
    const locks = typeof navigator === 'undefined' ? undefined : navigator.locks;
    return locks ? (locks.request(NOME_DA_TRAVA, tarefa) as Promise<T>) : tarefa();
  }

  private async trocar(): Promise<ResultadoRenovacao> {
    // Lido aqui dentro (e não antes da trava): outra aba pode ter renovado enquanto esperávamos.
    const refresh = this.sessao.refreshGuardado();
    if (!refresh) {
      return 'sem-sessao';
    }
    try {
      this.sessao.iniciar(await firstValueFrom(this.api.renovar(refresh)));
      return 'renovada';
    } catch (erro) {
      const { status } = erro as ApiError;
      if (status === 401) {
        this.sessao.limpar();
        return 'sem-sessao';
      }
      // Rede fora (0) ou 5xx: o refresh continua valendo, a pessoa não perde a sessão por falha do servidor.
      return 'indisponivel';
    }
  }
}
