import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Renovacao } from './renovacao';
import { Sessao } from './sessao';

const comToken = (req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> =>
  token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

/**
 * Põe o token de acesso nas chamadas à API (nunca nas de /auth/*, que não usam token e cujo 401
 * significa "senha errada" ou "sessão expirada", não "renove"). Num 401, renova UMA vez e repete.
 * A repetição vai direto ao `next`, sem passar de novo por esta lógica, então não há laço.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const daApi = req.url.startsWith(environment.apiUrl) && !req.url.startsWith(`${environment.apiUrl}/auth/`);
  if (!daApi) {
    return next(req);
  }
  const sessao = inject(Sessao);
  const renovacao = inject(Renovacao);
  const router = inject(Router);

  return next(comToken(req, sessao.tokenDeAcesso())).pipe(
    catchError((erro: unknown) => {
      if (!(erro instanceof HttpErrorResponse) || erro.status !== 401) {
        return throwError(() => erro);
      }
      return from(renovacao.renovar()).pipe(
        switchMap((resultado) => {
          if (resultado === 'renovada') {
            return next(comToken(req, sessao.tokenDeAcesso()));
          }
          if (resultado === 'sem-sessao') {
            void router.navigate(['/entrar'], { queryParams: { volta: router.url } });
          }
          return throwError(() => erro);
        }),
      );
    }),
  );
};
