import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { ApiError } from '../core/api-error';
import { FichaOrdemEntrada } from '../fichas/ficha.model';
import { CampanhaApi } from './campanha-api';

const BASE = `${environment.apiUrl}/campanhas`;

describe('CampanhaApi', () => {
  let api: CampanhaApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(CampanhaApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('listar: GET /campanhas', () => {
    let resposta: unknown;
    api.listar().subscribe((r) => (resposta = r));
    const req = http.expectOne(BASE);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1 }]);
    expect(resposta).toEqual([{ id: 1 }]);
  });

  it('criar: POST /campanhas e devolve o id do corpo', () => {
    let id: number | undefined;
    api.criar({ nome: 'Mesa', descricao: null }).subscribe((r) => (id = r));
    const req = http.expectOne(BASE);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ nome: 'Mesa', descricao: null });
    req.flush({ id: 4, nome: 'Mesa' }, { status: 201, statusText: 'Created' });
    expect(id).toBe(4);
  });

  it('criar: sem corpo, o id vem do Location', () => {
    let id: number | undefined;
    api.criar({ nome: 'Mesa', descricao: null }).subscribe((r) => (id = r));
    http.expectOne(BASE).flush(null, { status: 201, statusText: 'Created', headers: { Location: '/campanhas/9' } });
    expect(id).toBe(9);
  });

  it('buscar: GET /campanhas/{id}', () => {
    api.buscar(3).subscribe();
    const req = http.expectOne(`${BASE}/3`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('atualizar: PUT /campanhas/{id} com nome e descrição', () => {
    api.atualizar(3, { nome: 'Nova', descricao: 'x' }).subscribe();
    const req = http.expectOne(`${BASE}/3`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ nome: 'Nova', descricao: 'x' });
    req.flush({});
  });

  it('excluir: DELETE /campanhas/{id}', () => {
    api.excluir(3).subscribe();
    const req = http.expectOne(`${BASE}/3`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('membros e removerMembro', () => {
    api.membros(3).subscribe();
    const lista = http.expectOne(`${BASE}/3/membros`);
    expect(lista.request.method).toBe('GET');
    lista.flush([]);

    api.removerMembro(3, 8).subscribe();
    const remocao = http.expectOne(`${BASE}/3/membros/8`);
    expect(remocao.request.method).toBe('DELETE');
    remocao.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('fichas: GET /campanhas/{id}/fichas', () => {
    api.fichas(3).subscribe();
    const req = http.expectOne(`${BASE}/3/fichas`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('criarFicha: POST /campanhas/{id}/fichas com o corpo da ficha', () => {
    const entrada = { sistema: 'ORDEM_PARANORMAL', personagem: 'Lia' } as FichaOrdemEntrada;
    api.criarFicha(3, entrada).subscribe();
    const req = http.expectOne(`${BASE}/3/fichas`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBe(entrada);
    req.flush({});
  });

  it('convite: renovar (POST) e desativar (DELETE)', () => {
    let convite: unknown;
    api.renovarConvite(3).subscribe((c) => (convite = c));
    const renovacao = http.expectOne(`${BASE}/3/convite`);
    expect(renovacao.request.method).toBe('POST');
    renovacao.flush({ codigo: 'abc', ativo: true });
    expect(convite).toEqual({ codigo: 'abc', ativo: true });

    api.desativarConvite(3).subscribe();
    const desativacao = http.expectOne(`${BASE}/3/convite`);
    expect(desativacao.request.method).toBe('DELETE');
    desativacao.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('previa: GET /convites/{codigo}; aceitar: POST /convites/{codigo}/aceitar', () => {
    api.previa('Abc123').subscribe();
    const previa = http.expectOne(`${environment.apiUrl}/convites/Abc123`);
    expect(previa.request.method).toBe('GET');
    previa.flush({ campanhaId: 3, campanha: 'Mesa', mestre: 'Ana' });

    let aceite: unknown;
    api.aceitar('Abc123').subscribe((r) => (aceite = r));
    const aceitar = http.expectOne(`${environment.apiUrl}/convites/Abc123/aceitar`);
    expect(aceitar.request.method).toBe('POST');
    aceitar.flush({ campanhaId: 3 }, { status: 201, statusText: 'Created' });
    expect(aceite).toEqual({ campanhaId: 3 });
  });

  it('erros saem como ApiError', () => {
    let erro: ApiError | undefined;
    api.previa('xx').subscribe({ error: (e: ApiError) => (erro = e) });
    http
      .expectOne(`${environment.apiUrl}/convites/xx`)
      .flush({ title: 'Convite inválido ou desativado', detail: 'Peça outro.' }, { status: 404, statusText: 'Not Found' });
    expect(erro).toEqual({ status: 404, titulo: 'Convite inválido ou desativado', detalhe: 'Peça outro.', campos: {} });
  });
});
