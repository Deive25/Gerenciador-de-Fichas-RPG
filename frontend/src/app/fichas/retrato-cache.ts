import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, shareReplay, throwError } from 'rxjs';
import { FichaApi } from './ficha-api';

interface Entrada {
  versao: number;
  url$: Observable<string>;
  /** URL local criada (blob:…), para revogar quando a versão mudar. */
  criada: string | null;
}

/**
 * Transforma o retrato baixado (blob) numa URL local que o <img> mostra. Guarda uma URL por ficha:
 * a mesma versão reaproveita a URL (sem baixar de novo); uma versão nova baixa e revoga a antiga,
 * para a memória do navegador não acumular imagens velhas.
 */
@Injectable({ providedIn: 'root' })
export class RetratoCache {
  private readonly api = inject(FichaApi);
  private readonly entradas = new Map<number, Entrada>();

  url(fichaId: number, versao: number): Observable<string> {
    const atual = this.entradas.get(fichaId);
    if (atual && atual.versao === versao) {
      return atual.url$;
    }
    if (atual) {
      this.revogar(atual);
    }
    const entrada: Entrada = { versao, criada: null, url$: undefined as unknown as Observable<string> };
    entrada.url$ = this.api.baixarRetrato(fichaId, versao).pipe(
      map((blob) => {
        entrada.criada = URL.createObjectURL(blob);
        return entrada.criada;
      }),
      // Falhou: esquece a entrada para a próxima tentativa baixar de novo.
      catchError((erro: unknown) => {
        if (this.entradas.get(fichaId) === entrada) {
          this.entradas.delete(fichaId);
        }
        return throwError(() => erro);
      }),
      shareReplay(1),
    );
    this.entradas.set(fichaId, entrada);
    return entrada.url$;
  }

  /** Depois de remover o retrato: revoga a URL e esquece a ficha. */
  esquecer(fichaId: number): void {
    const atual = this.entradas.get(fichaId);
    if (atual) {
      this.revogar(atual);
      this.entradas.delete(fichaId);
    }
  }

  private revogar(entrada: Entrada): void {
    if (entrada.criada) {
      URL.revokeObjectURL(entrada.criada);
    }
  }
}
