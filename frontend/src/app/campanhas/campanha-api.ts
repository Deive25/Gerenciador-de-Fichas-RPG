import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { paraApiError } from '../core/api-error';
import { Ficha, FichaEntrada, FichaResumo } from '../fichas/ficha.model';
import { Campanha, CampanhaEntrada, CampanhaResumo, Convite, Membro, PreviaConvite } from './campanha.model';

/** Único ponto de contato com a API de campanhas e convites. Todo erro sai como ApiError. */
@Injectable({ providedIn: 'root' })
export class CampanhaApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/campanhas`;
  private readonly convites = `${environment.apiUrl}/convites`;

  listar(): Observable<CampanhaResumo[]> {
    return this.tratar(this.http.get<CampanhaResumo[]>(this.base));
  }

  /**
   * Devolve o id da campanha criada: do corpo da resposta ou, se vier sem corpo, do fim do
   * cabeçalho Location (/campanhas/{id}); a spec só fixa o 201 com Location.
   */
  criar(entrada: CampanhaEntrada): Observable<number> {
    return this.tratar(
      this.http.post<{ id?: number } | null>(this.base, entrada, { observe: 'response' }).pipe(
        map((resposta) => {
          const id = resposta.body?.id ?? Number(resposta.headers.get('Location')?.split('/').pop());
          if (!Number.isInteger(id)) {
            throw new HttpErrorResponse({ status: 500, error: null });
          }
          return id;
        }),
      ),
    );
  }

  buscar(id: number): Observable<Campanha> {
    return this.tratar(this.http.get<Campanha>(`${this.base}/${id}`));
  }

  atualizar(id: number, entrada: CampanhaEntrada): Observable<Campanha> {
    return this.tratar(this.http.put<Campanha>(`${this.base}/${id}`, entrada));
  }

  excluir(id: number): Observable<void> {
    return this.tratar(this.http.delete<void>(`${this.base}/${id}`));
  }

  membros(id: number): Observable<Membro[]> {
    return this.tratar(this.http.get<Membro[]>(`${this.base}/${id}/membros`));
  }

  /** O mestre remove um jogador; o próprio jogador usa a mesma rota para sair. */
  removerMembro(id: number, usuarioId: number): Observable<void> {
    return this.tratar(this.http.delete<void>(`${this.base}/${id}/membros/${usuarioId}`));
  }

  fichas(id: number): Observable<FichaResumo[]> {
    return this.tratar(this.http.get<FichaResumo[]>(`${this.base}/${id}/fichas`));
  }

  criarFicha(id: number, entrada: FichaEntrada): Observable<Ficha> {
    return this.tratar(this.http.post<Ficha>(`${this.base}/${id}/fichas`, entrada));
  }

  /** Gera um código novo (o antigo deixa de valer) e ativa o convite. */
  renovarConvite(id: number): Observable<Convite> {
    return this.tratar(this.http.post<Convite>(`${this.base}/${id}/convite`, {}));
  }

  desativarConvite(id: number): Observable<void> {
    return this.tratar(this.http.delete<void>(`${this.base}/${id}/convite`));
  }

  /** Pública: funciona sem sessão. */
  previa(codigo: string): Observable<PreviaConvite> {
    return this.tratar(this.http.get<PreviaConvite>(`${this.convites}/${encodeURIComponent(codigo)}`));
  }

  /** Entra como jogador (201); quem já é membro recebe 200. Os dois com o id da campanha. */
  aceitar(codigo: string): Observable<{ campanhaId: number }> {
    return this.tratar(
      this.http.post<{ campanhaId: number }>(`${this.convites}/${encodeURIComponent(codigo)}/aceitar`, {}),
    );
  }

  private tratar<T>(requisicao: Observable<T>): Observable<T> {
    return requisicao.pipe(
      catchError((erro: unknown) => throwError(() => (erro instanceof HttpErrorResponse ? paraApiError(erro) : erro))),
    );
  }
}
