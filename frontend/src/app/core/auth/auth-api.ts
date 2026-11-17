import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { paraApiError } from '../api-error';
import { RespostaAuth } from './auth.model';

/** Único ponto de contato com /auth/*. Todo erro sai como ApiError. */
@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/auth`;

  registrar(dados: { nome: string; email: string; senha: string }): Observable<RespostaAuth> {
    return this.tratar(this.http.post<RespostaAuth>(`${this.base}/registro`, dados));
  }

  entrar(dados: { email: string; senha: string }): Observable<RespostaAuth> {
    return this.tratar(this.http.post<RespostaAuth>(`${this.base}/login`, dados));
  }

  /** `idToken` é o `credential` do botão do Google; a API o valida e devolve a nossa sessão. */
  entrarComGoogle(idToken: string): Observable<RespostaAuth> {
    return this.tratar(this.http.post<RespostaAuth>(`${this.base}/google`, { idToken }));
  }

  renovar(refreshToken: string): Observable<RespostaAuth> {
    return this.tratar(this.http.post<RespostaAuth>(`${this.base}/refresh`, { refreshToken }));
  }

  sair(refreshToken: string): Observable<void> {
    return this.tratar(this.http.post<void>(`${this.base}/logout`, { refreshToken }));
  }

  private tratar<T>(fonte: Observable<T>): Observable<T> {
    return fonte.pipe(catchError((e: HttpErrorResponse) => throwError(() => paraApiError(e))));
  }
}
