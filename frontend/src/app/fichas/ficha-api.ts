import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, tap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { paraApiError } from '../core/api-error';
import {
  DescricaoFicha,
  Ficha,
  FichaEntrada,
  FichaResumo,
  Habilidade,
  HabilidadeEntrada,
  PedidoTeste,
  TesteResultado,
} from './ficha.model';

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

  listar(): Observable<FichaResumo[]> {
    return this.tratar(this.http.get<FichaResumo[]>(this.base));
  }

  buscarFicha(id: number): Observable<Ficha> {
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

  /** O corpo é o próprio JPEG (já recortado e reduzido no navegador), sem JSON. */
  enviarRetrato(id: number, jpeg: Blob): Observable<void> {
    return this.alterou(
      this.tratar(
        this.http.put<void>(`${this.base}/${id}/retrato`, jpeg, { headers: { 'Content-Type': 'image/jpeg' } }),
      ),
    );
  }

  removerRetrato(id: number): Observable<void> {
    return this.alterou(this.tratar(this.http.delete<void>(`${this.base}/${id}/retrato`)));
  }

  /**
   * Baixa o retrato como blob (e não por <img src>): a partir do login (2.2) a rota exige token, e o
   * <img> não envia o cabeçalho Authorization. O ?v= muda a cada envio, então o cache HTTP nunca
   * devolve uma imagem velha.
   */
  baixarRetrato(id: number, versao: number): Observable<Blob> {
    return this.tratar(this.http.get(`${this.base}/${id}/retrato`, { params: { v: versao }, responseType: 'blob' }));
  }

  /** Edição rápida: só os valores atuais dos códigos enviados mudam (PV, SAN, PE em Ordem; PV e PV_TEMP em D&D). */
  atualizarRecursos(id: number, atuais: Partial<Record<string, number>>): Observable<Ficha> {
    return this.alterou(this.tratar(this.http.patch<Ficha>(`${this.base}/${id}/recursos`, atuais)));
  }

  /** O servidor rola os d20 (ele é a fonte do aleatório); o navegador só mostra o resultado. */
  testar(id: number, teste: PedidoTeste): Observable<TesteResultado> {
    return this.tratar(this.http.post<TesteResultado>(`${this.base}/${id}/testes`, teste));
  }

  atualizarDescricao(id: number, descricao: DescricaoFicha): Observable<DescricaoFicha> {
    return this.alterou(this.tratar(this.http.put<DescricaoFicha>(`${this.base}/${id}/descricao`, descricao)));
  }

  criarHabilidade(id: number, habilidade: HabilidadeEntrada): Observable<Habilidade> {
    return this.alterou(this.tratar(this.http.post<Habilidade>(`${this.base}/${id}/habilidades`, habilidade)));
  }

  atualizarHabilidade(id: number, habilidadeId: number, habilidade: HabilidadeEntrada): Observable<Habilidade> {
    return this.alterou(
      this.tratar(this.http.put<Habilidade>(`${this.base}/${id}/habilidades/${habilidadeId}`, habilidade)),
    );
  }

  excluirHabilidade(id: number, habilidadeId: number): Observable<void> {
    return this.alterou(this.tratar(this.http.delete<void>(`${this.base}/${id}/habilidades/${habilidadeId}`)));
  }

  private alterou<T>(requisicao: Observable<T>): Observable<T> {
    return requisicao.pipe(tap(() => this.versao.update((v) => v + 1)));
  }

  private tratar<T>(requisicao: Observable<T>): Observable<T> {
    return requisicao.pipe(catchError((erro: HttpErrorResponse) => throwError(() => paraApiError(erro))));
  }
}
