import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiError } from '../core/api-error';
import { FichaApi } from './ficha-api';
import { FichaEntrada } from './ficha.model';

const API = 'http://localhost:8080';

const ENTRADA: FichaEntrada = {
  jogador: 'Arthur',
  personagem: 'Lia',
  genero: 'F',
  idade: 34,
  vidaAtual: 9,
  vidaMaxima: 12,
  sanidadeAtual: 48,
  sanidadeMaxima: 60,
  forca: 45,
  destreza: null,
  constituicao: null,
  inteligencia: null,
  carisma: null,
  sabedoria: null,
  tamanho: null,
  sorte: null,
  poder: null,
  pericias: { INVESTIGACAO: 70 },
};

describe('FichaApi', () => {
  let api: FichaApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(FichaApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('listar faz GET /fichas', () => {
    api.listar().subscribe();
    const req = http.expectOne(`${API}/fichas`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('buscar faz GET /fichas/7', () => {
    api.buscar(7).subscribe();
    const req = http.expectOne(`${API}/fichas/7`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('criar faz POST /fichas com o corpo sem id', () => {
    api.criar(ENTRADA).subscribe();
    const req = http.expectOne(`${API}/fichas`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(ENTRADA);
    expect('id' in req.request.body).toBe(false);
    req.flush({ id: 1, ...ENTRADA });
  });

  it('atualizar faz PUT /fichas/7', () => {
    api.atualizar(7, ENTRADA).subscribe();
    const req = http.expectOne(`${API}/fichas/7`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(ENTRADA);
    req.flush({ id: 7, ...ENTRADA });
  });

  it('excluir faz DELETE /fichas/7', () => {
    api.excluir(7).subscribe();
    const req = http.expectOne(`${API}/fichas/7`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('testarPericia faz POST /fichas/7/testes-de-pericia com a pericia', () => {
    api.testarPericia(7, 'CURA').subscribe();
    const req = http.expectOne(`${API}/fichas/7/testes-de-pericia`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ pericia: 'CURA' });
    req.flush({ pericia: 'CURA', nome: 'Cura', valor: 0, rolagem: 50, resultado: 'FALHA' });
  });

  it('avisa que as fichas mudaram so depois de criar, atualizar ou excluir com sucesso', () => {
    expect(api.versaoFichas()).toBe(0);

    api.criar(ENTRADA).subscribe();
    http.expectOne(`${API}/fichas`).flush({ id: 1, ...ENTRADA });
    api.atualizar(1, ENTRADA).subscribe();
    http.expectOne(`${API}/fichas/1`).flush({ id: 1, ...ENTRADA });
    api.excluir(1).subscribe();
    http.expectOne(`${API}/fichas/1`).flush(null, { status: 204, statusText: 'No Content' });
    expect(api.versaoFichas()).toBe(3);

    api.criar(ENTRADA).subscribe({ error: () => undefined });
    http.expectOne(`${API}/fichas`).flush({ title: 'Dados inválidos' }, { status: 400, statusText: 'Bad Request' });
    api.listar().subscribe();
    http.expectOne(`${API}/fichas`).flush([]);
    expect(api.versaoFichas()).toBe(3);
  });

  it('erros chegam como ApiError', () => {
    let recebido: ApiError | undefined;
    api.buscar(9).subscribe({ error: (e: ApiError) => (recebido = e) });
    http
      .expectOne(`${API}/fichas/9`)
      .flush(
        { title: 'Ficha não encontrada', detail: 'Não existe ficha com o id 9.', status: 404 },
        { status: 404, statusText: 'Not Found' },
      );
    expect(recebido?.status).toBe(404);
    expect(recebido?.titulo).toBe('Ficha não encontrada');
  });
});
