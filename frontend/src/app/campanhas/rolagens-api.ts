import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { paraApiError } from '../core/api-error';
import { RolagemDaMesa, TAMANHO_DA_PAGINA } from './rolagem-da-mesa.model';

/** O histórico de rolagens de uma campanha (`GET /campanhas/{id}/rolagens`). Todo erro sai como ApiError. */
@Injectable({ providedIn: 'root' })
export class RolagensApi {
  private readonly http = inject(HttpClient);

  /** Da mais nova para a mais antiga; `antes` é o cursor (só ids menores que ele). */
  historico(campanhaId: number, antes?: number, limite = TAMANHO_DA_PAGINA): Observable<RolagemDaMesa[]> {
    let url = `${environment.apiUrl}/campanhas/${campanhaId}/rolagens?limite=${limite}`;
    if (antes !== undefined) {
      url += `&antes=${antes}`;
    }
    return this.http.get<RolagemDaMesa[]>(url).pipe(
      catchError((erro: unknown) => throwError(() => (erro instanceof HttpErrorResponse ? paraApiError(erro) : erro))),
    );
  }
}
