import { Injectable, InjectionToken, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { RxStomp, RxStompState } from '@stomp/rx-stomp';
import { Observable, Subscription, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { Renovacao } from '../core/auth/renovacao';
import { Sessao } from '../core/auth/sessao';
import { RolagemDaMesa } from './rolagem-da-mesa.model';

/** Fábrica do cliente STOMP: os testes trocam por um falso, sem WebSocket de verdade. */
export const FABRICA_RX_STOMP = new InjectionToken<() => RxStomp>('FABRICA_RX_STOMP', {
  providedIn: 'root',
  factory: () => () => new RxStomp(),
});

export type EstadoDaMesa = 'conectado' | 'reconectando' | 'desconectado';

/** A mesma URL da API, com `http` trocado por `ws` (e `https` por `wss`), mais /ws. */
const URL_DO_WEBSOCKET = `${environment.apiUrl.replace(/^http/, 'ws')}/ws`;
const HEARTBEAT_MS = 10_000;

/**
 * As rolagens da mesa ao vivo (STOMP sobre WebSocket, spec do 2.4 §5 e §6). A conexão é compartilhada
 * e só existe enquanto alguém está inscrito: abre na primeira inscrição e fecha na última, então
 * ninguém paga por WebSocket fora de uma tela de campanha.
 *
 * O token de acesso vai no CONNECT (o handshake HTTP é público). Como ele vence em 15 minutos, só a
 * primeira tentativa usa o que está na memória; as reconexões renovam antes pelo fluxo do 2.2.
 */
@Injectable({ providedIn: 'root' })
export class MesaAoVivo {
  private readonly fabrica = inject(FABRICA_RX_STOMP);
  private readonly sessao = inject(Sessao);
  private readonly renovacao = inject(Renovacao);
  private readonly router = inject(Router);

  private readonly _estado = signal<EstadoDaMesa>('desconectado');
  readonly estado = this._estado.asReadonly();

  private stomp: RxStomp | null = null;
  private acompanhaEstado: Subscription | null = null;
  private inscritos = 0;

  /** Cada rolagem nova da campanha. Inscrever-se abre a conexão (se preciso); cancelar a fecha quando não sobra ninguém. */
  rolagens$(campanhaId: number): Observable<RolagemDaMesa> {
    return new Observable<RolagemDaMesa>((assinante) => {
      const stomp = this.abrir();
      const assinatura = stomp
        .watch(`/topic/campanhas/${campanhaId}/rolagens`)
        .pipe(map((mensagem) => this.converter(mensagem.body)))
        .subscribe({
          next: (rolagem) => {
            if (rolagem) {
              assinante.next(rolagem);
            }
          },
          error: (erro: unknown) => assinante.error(erro),
        });
      return () => {
        assinatura.unsubscribe();
        this.fechar();
      };
    });
  }

  private converter(corpo: string): RolagemDaMesa | null {
    try {
      return JSON.parse(corpo) as RolagemDaMesa;
    } catch {
      return null; // uma mensagem estranha não derruba o painel
    }
  }

  private abrir(): RxStomp {
    this.inscritos++;
    if (this.stomp) {
      return this.stomp;
    }
    const stomp = this.fabrica();
    let tentativa = 0;
    stomp.configure({
      brokerURL: URL_DO_WEBSOCKET,
      heartbeatIncoming: HEARTBEAT_MS,
      heartbeatOutgoing: HEARTBEAT_MS,
      reconnectDelay: 5000,
      beforeConnect: async (cliente) => {
        const token = await this.tokenParaConectar(tentativa++ > 0);
        if (token) {
          cliente.stompClient.connectHeaders = { Authorization: `Bearer ${token}` };
        } else {
          // Sem token não há como entrar: para de reconectar (senão seria um laço de CONNECTs recusados).
          this.parar(stomp);
        }
      },
    });
    this.stomp = stomp;
    this.acompanhaEstado = stomp.connectionState$.subscribe((estado) =>
      this._estado.set(estado === RxStompState.OPEN ? 'conectado' : 'reconectando'),
    );
    stomp.activate();
    return stomp;
  }

  /** O token em memória na 1ª tentativa; renovado nas seguintes (ou se a memória está sem token). */
  private async tokenParaConectar(renovarAntes: boolean): Promise<string | null> {
    const atual = this.sessao.tokenDeAcesso();
    if (atual && !renovarAntes) {
      return atual;
    }
    const resultado = await this.renovacao.renovar();
    if (resultado === 'sem-sessao') {
      // Mesmo caminho do interceptor: a sessão vencida leva ao login, e depois volta para esta tela.
      void this.router.navigate(['/entrar'], { queryParams: { volta: this.router.url } });
      return null;
    }
    // Rede fora (indisponivel): ainda dá para tentar com o token que havia; sem ele, não há o que fazer.
    return this.sessao.tokenDeAcesso();
  }

  private fechar(): void {
    this.inscritos = Math.max(0, this.inscritos - 1);
    if (this.inscritos === 0 && this.stomp) {
      this.parar(this.stomp);
    }
  }

  private parar(stomp: RxStomp): void {
    void stomp.deactivate();
    if (this.stomp === stomp) {
      this.stomp = null;
      this.acompanhaEstado?.unsubscribe();
      this.acompanhaEstado = null;
      this._estado.set('desconectado');
    }
  }
}
