import { Component, DestroyRef, effect, inject, input, output, signal, untracked } from '@angular/core';
import { ApiError } from '../../core/api-error';
import { AnelStatus } from '../componentes/anel-status';
import { FichaApi } from '../ficha-api';
import { Ficha, paraEntradaDaFicha } from '../ficha.model';

/** Espera depois do último toque antes de salvar: vários toques seguidos viram um PUT só. */
export const ATRASO_SALVAMENTO_MS = 800;

type Recurso = 'vidaAtual' | 'sanidadeAtual';

/**
 * Resultado de um toque em − (delta −1) ou + (delta +1). O + não passa do máximo e, se o valor já
 * está acima dele (ficha antiga), fica parado; o − nunca passa de 0 e desce de 1 em 1 mesmo acima
 * do máximo (15/12 → 14, e não um pulo para 12). Valor ausente conta como 0.
 */
export function aplicarAjuste(atual: number | null, delta: number, maximo: number | null): number {
  const base = atual ?? 0;
  if (delta > 0) {
    if (maximo !== null && base >= maximo) {
      return base;
    }
    return maximo === null ? base + delta : Math.min(base + delta, maximo);
  }
  return Math.max(base + delta, 0);
}

/** Valor digitado: fica entre 0 e o máximo (sem máximo, só não pode ser negativo). */
export function limitarDigitado(valor: number, maximo: number | null): number {
  const limitado = maximo === null ? valor : Math.min(valor, maximo);
  return Math.max(limitado, 0);
}

/**
 * Edição rápida da vida e da sanidade atuais, direto na ficha (spec do redesign §4.6).
 * A tela muda na hora; o PUT sai 800 ms depois do último toque, com a ficha inteira (sem id nem
 * retratoVersao). Se falhar, volta ao último valor salvo. Um toque durante o salvamento agenda
 * outro salvamento, e sair da tela com um ajuste esperando salva na hora.
 */
@Component({
  selector: 'app-recursos-ficha',
  imports: [AnelStatus],
  templateUrl: './recursos-ficha.html',
  styleUrl: './recursos-ficha.scss',
})
export class RecursosFicha {
  private readonly api = inject(FichaApi);

  readonly ficha = input.required<Ficha>();
  /** A ficha como o servidor devolveu depois de salvar. */
  readonly atualizada = output<Ficha>();

  readonly vidaAtual = signal<number | null>(null);
  readonly sanidadeAtual = signal<number | null>(null);
  readonly salvando = signal(false);
  readonly erro = signal<ApiError | null>(null);

  private ultimoSalvo: Record<Recurso, number | null> = { vidaAtual: null, sanidadeAtual: null };
  private temporizador: ReturnType<typeof setTimeout> | null = null;
  private salvarDepois = false;
  private destruido = false;

  constructor() {
    // Copia os valores da ficha, menos quando há ajuste esperando ou salvando: aí o valor da tela é
    // mais novo que o da ficha e não pode ser apagado.
    effect(() => {
      const ficha = this.ficha();
      untracked(() => {
        if (this.temporizador === null && !this.salvando()) {
          this.vidaAtual.set(ficha.vidaAtual);
          this.sanidadeAtual.set(ficha.sanidadeAtual);
          this.ultimoSalvo = { vidaAtual: ficha.vidaAtual, sanidadeAtual: ficha.sanidadeAtual };
        }
      });
    });

    inject(DestroyRef).onDestroy(() => {
      this.destruido = true;
      if (this.temporizador !== null) {
        clearTimeout(this.temporizador);
        this.temporizador = null;
        this.salvar();
      }
    });
  }

  ajustar(recurso: Recurso, delta: number): void {
    this.mudar(recurso, aplicarAjuste(this.valor(recurso), delta, this.maximo(recurso)));
  }

  definir(recurso: Recurso, valor: number): void {
    this.mudar(recurso, limitarDigitado(valor, this.maximo(recurso)));
  }

  private mudar(recurso: Recurso, valor: number): void {
    (recurso === 'vidaAtual' ? this.vidaAtual : this.sanidadeAtual).set(valor);
    this.erro.set(null);
    if (this.temporizador !== null) {
      clearTimeout(this.temporizador);
    }
    this.temporizador = setTimeout(() => {
      this.temporizador = null;
      this.salvar();
    }, ATRASO_SALVAMENTO_MS);
  }

  private salvar(): void {
    if (this.salvando()) {
      this.salvarDepois = true;
      return;
    }
    const valores = { vidaAtual: this.vidaAtual(), sanidadeAtual: this.sanidadeAtual() };
    const ficha = this.ficha();
    this.salvando.set(true);
    this.api.atualizar(ficha.id, paraEntradaDaFicha({ ...ficha, ...valores })).subscribe({
      next: (resposta) => {
        this.ultimoSalvo = valores;
        this.salvando.set(false);
        if (!this.destruido) {
          this.atualizada.emit(resposta);
        }
        if (this.salvarDepois) {
          this.salvarDepois = false;
          this.salvar();
        }
      },
      error: (erro: ApiError) => {
        this.salvando.set(false);
        this.salvarDepois = false;
        if (this.temporizador !== null) {
          clearTimeout(this.temporizador);
          this.temporizador = null;
        }
        this.vidaAtual.set(this.ultimoSalvo.vidaAtual);
        this.sanidadeAtual.set(this.ultimoSalvo.sanidadeAtual);
        this.erro.set(erro);
      },
    });
  }

  private valor(recurso: Recurso): number | null {
    return recurso === 'vidaAtual' ? this.vidaAtual() : this.sanidadeAtual();
  }

  private maximo(recurso: Recurso): number | null {
    return recurso === 'vidaAtual' ? this.ficha().vidaMaxima : this.ficha().sanidadeMaxima;
  }
}
