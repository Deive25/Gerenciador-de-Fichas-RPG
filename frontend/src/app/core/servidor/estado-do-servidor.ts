import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * - verificando: acabou de abrir, ainda dentro dos 3 s de tolerância (não bloqueia nada);
 * - acordando: sem resposta há 3 s ou mais (a API gratuita dorme); mostra a faixa;
 * - pronto: a API respondeu ao liveness;
 * - falhou: 3 min sem resposta; a faixa pede para recarregar.
 */
export type SituacaoDoServidor = 'verificando' | 'acordando' | 'pronto' | 'falhou';

const MOSTRAR_APOS_S = 3;
const TENTAR_A_CADA_S = 5;
const DESISTIR_APOS_S = 180;

/**
 * A API gratuita dorme depois de 15 min e leva cerca de 1 min para acordar. Ao abrir o app, chamamos
 * o liveness (que não consulta o banco). Um único relógio de 1 s conta os segundos; é ele que decide
 * quando mostrar a faixa, quando tentar de novo e quando desistir.
 */
@Injectable({ providedIn: 'root' })
export class EstadoDoServidor {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/actuator/health/liveness`;

  private readonly _estado = signal<SituacaoDoServidor>('verificando');
  private readonly _segundos = signal(0);
  private relogio: ReturnType<typeof setInterval> | null = null;
  private pedido: Subscription | null = null;
  private esperando: Array<() => void> = [];

  readonly estado = this._estado.asReadonly();
  readonly segundos = this._segundos.asReadonly();
  /** Os botões de entrar ficam desabilitados enquanto a API acorda (ou depois de desistir). */
  readonly aguardando = computed(() => this._estado() === 'acordando' || this._estado() === 'falhou');

  /** Começa a conferir (uma vez só). */
  iniciar(): void {
    if (this.relogio !== null) {
      return;
    }
    this.pingar();
    this.relogio = setInterval(() => this.passar(), 1000);
  }

  /** Resolve quando os botões podem ser usados (na hora, se já podem). Nunca resolve se o servidor falhou. */
  quandoLiberado(): Promise<void> {
    if (!this.aguardando()) {
      return Promise.resolve();
    }
    return new Promise((resolver) => this.esperando.push(resolver));
  }

  private passar(): void {
    const segundos = this._segundos() + 1;
    this._segundos.set(segundos);
    if (segundos >= DESISTIR_APOS_S) {
      this.parar();
      this._estado.set('falhou');
      return;
    }
    if (segundos >= MOSTRAR_APOS_S && this._estado() === 'verificando') {
      this._estado.set('acordando');
    }
    if (segundos % TENTAR_A_CADA_S === 0) {
      this.pingar();
    }
  }

  /** Cada tentativa cancela a anterior: um pedido preso (a API acordando) não se acumula. */
  private pingar(): void {
    this.pedido?.unsubscribe();
    this.pedido = this.http.get(this.url).subscribe({
      next: () => {
        this.parar();
        this._estado.set('pronto');
        this.esperando.splice(0).forEach((resolver) => resolver());
      },
      error: () => undefined, // 503 do proxy ou rede: a próxima tentativa vem em 5 s.
    });
  }

  private parar(): void {
    if (this.relogio !== null) {
      clearInterval(this.relogio);
    }
    this.pedido?.unsubscribe();
    this.pedido = null;
  }
}
