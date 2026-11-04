import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, tap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { paraApiError } from '../core/api-error';
import { CodigoPericia, Ficha, FichaEntrada, TestePericiaResultado } from './ficha.model';

/** Único ponto de contato com a API de fichas. Todo erro sai como ApiError. */
@Injectable({ providedIn: 'root' })
export class FichaApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/fichas`;
  private readonly versao = signal(0);

  /**
   * Muda a cada criação, atualização ou exclusão bem-sucedida. O painel observa este sinal
   * para recarregar a lista sempre que alguma ficha muda.
   */
  readonly versaoFichas = this.versao.asReadonly();

  listar(): Observable<Ficha[]> {
    return this.tratar(this.http.get<Ficha[]>(this.base));
  }

  buscar(id: number): Observable<Ficha> {
    return this.tratar(this.http.get<Ficha>(`${this.base}/${id}`));
  }

  criar(entrada: FichaEntrada): Observable<Ficha> {
    return this.alterou(this.tratar(this.http.post<Ficha>(this.base, entrada)));
  }

  atualizar(id: number, entrada: FichaEntrada): Observable<Ficha> {
    return this.alterou(this.tratar(this.http.put<Ficha>(`${this.base}/${id}`, entrada)));
  }

  excluir(id: number): Observable<void> {
    return this.alterou(this.tratar(this.http.delete<void>(`${this.base}/${id}`)));
  }

  testarPericia(id: number, pericia: CodigoPericia): Observable<TestePericiaResultado> {
    return this.tratar(this.http.post<TestePericiaResultado>(`${this.base}/${id}/testes-de-pericia`, { pericia }));
  }

  private alterou<T>(requisicao: Observable<T>): Observable<T> {
    return requisicao.pipe(tap(() => this.versao.update((v) => v + 1)));
  }

  private tratar<T>(requisicao: Observable<T>): Observable<T> {
    return requisicao.pipe(catchError((erro: HttpErrorResponse) => throwError(() => paraApiError(erro))));
  }
}
