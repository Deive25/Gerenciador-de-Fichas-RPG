import { Component, DestroyRef, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { ApiError } from '../../core/api-error';
import { AnelStatus } from '../componentes/anel-status';
import { FichaApi } from '../ficha-api';
import { Ficha, RecursoValor, Sistema } from '../ficha.model';

/** Espera depois do último toque antes de salvar: vários toques seguidos viram um PATCH só. */
export const ATRASO_SALVAMENTO_MS = 800;

/** Um anel de recurso: o código é o que o PATCH leva (PV, SAN, PE, PV_TEMP). */
export interface AnelDef {
  codigo: string;
  rotulo: string;
  tipo: 'vida' | 'sanidade' | 'esforco' | 'temporario';
}

/** Os anéis de cada sistema, na ordem em que aparecem. */
export const ANEIS_POR_SISTEMA: Record<Sistema, readonly AnelDef[]> = {
  ORDEM_PARANORMAL: [
    { codigo: 'PV', rotulo: 'Vida', tipo: 'vida' },
    { codigo: 'SAN', rotulo: 'Sanidade', tipo: 'sanidade' },
    { codigo: 'PE', rotulo: 'Esforço', tipo: 'esforco' },
  ],
  DND: [
    { codigo: 'PV', rotulo: 'Vida', tipo: 'vida' },
    { codigo: 'PV_TEMP', rotulo: 'Temporário', tipo: 'temporario' },
  ],
};

type Valores = Record<string, number | null>;

/** Os recursos da ficha por código (a união Ficha tem recursos de formas diferentes). */
function recursosDe(ficha: Ficha): Record<string, RecursoValor | undefined> {
  return ficha.recursos;
}

function atuais(ficha: Ficha): Valores {
  const recursos = recursosDe(ficha);
  return Object.fromEntries(ANEIS_POR_SISTEMA[ficha.sistema].map((a) => [a.codigo, recursos[a.codigo]?.atual ?? null]));
}

/**
 * Resultado de um toque em − (delta −1) ou + (delta +1). O + não passa do máximo e, se o valor já
 * está acima dele (ficha antiga), fica parado; o − nunca passa de 0 e desce de 1 em 1 mesmo acima
 * do máximo (15/12 → 14, e não um pulo para 12). Valor ausente conta como 0. Sem máximo (PE das
 * fichas convertidas), o + não tem teto e nunca trava.
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
 * Edição rápida dos recursos atuais (Ordem: PV, SAN e PE; D&D: PV e PV temporário), direto na ficha (spec do redesign §4.6).
 * A tela muda na hora; o PATCH sai 800 ms depois do último toque e leva só os códigos que mudaram
 * (ex.: `{ PV: 9 }`), então não há como sobrescrever o que não foi tocado. Se falhar, volta ao
 * último valor salvo. Um toque durante o salvamento agenda outro salvamento, e sair da tela com um
 * ajuste esperando salva na hora.
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

  readonly aneis = computed(() => ANEIS_POR_SISTEMA[this.ficha().sistema]);
  readonly valores = signal<Valores>({});
  readonly salvando = signal(false);
  readonly erro = signal<ApiError | null>(null);

  private ultimoSalvo: Valores = {};
  /** Códigos tocados desde o último envio: só eles vão no PATCH. */
  private pendentes = new Set<string>();
  private temporizador: ReturnType<typeof setTimeout> | null = null;
  private salvarDepois = false;
  private destruido = false;

  constructor() {
    // Copia os valores da ficha, menos quando há ajuste esperando ou salvando: aí o valor da tela é
    // mais novo que o da ficha e não pode ser apagado.
    effect(() => {
      const ficha = this.ficha();
      untracked(() => {
        if (this.temporizador === null && !this.salvando() && this.pendentes.size === 0) {
          this.valores.set(atuais(ficha));
          this.ultimoSalvo = atuais(ficha);
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

  ajustar(codigo: string, delta: number): void {
    this.mudar(codigo, aplicarAjuste(this.valores()[codigo], delta, this.maximo(codigo)));
  }

  definir(codigo: string, valor: number): void {
    this.mudar(codigo, limitarDigitado(valor, this.maximo(codigo)));
  }

  maximo(codigo: string): number | null {
    return recursosDe(this.ficha())[codigo]?.maximo ?? null;
  }

  private mudar(codigo: string, valor: number): void {
    this.valores.update((v) => ({ ...v, [codigo]: valor }));
    this.pendentes.add(codigo);
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
    if (this.pendentes.size === 0) {
      return;
    }
    const corpo: Record<string, number> = {};
    for (const codigo of this.pendentes) {
      corpo[codigo] = this.valores()[codigo] ?? 0;
    }
    this.pendentes = new Set();
    this.salvando.set(true);
    this.api.atualizarRecursos(this.ficha().id, corpo).subscribe({
      next: (resposta) => {
        this.ultimoSalvo = { ...this.ultimoSalvo, ...corpo };
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
        this.pendentes = new Set();
        if (this.temporizador !== null) {
          clearTimeout(this.temporizador);
          this.temporizador = null;
        }
        this.valores.set({ ...this.ultimoSalvo });
        this.erro.set(erro);
      },
    });
  }
}
