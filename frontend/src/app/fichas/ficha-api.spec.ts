import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiError } from '../core/api-error';
import { FichaApi } from './ficha-api';
import { FichaEntrada, FichaResumo } from './ficha.model';

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

  it('listar devolve o resumo das fichas (sistema e recursos), sem os dados completos', () => {
    let resumo: FichaResumo[] = [];
    api.listar().subscribe((r) => (resumo = r));
    const corpo: FichaResumo[] = [
      {
        id: 1, sistema: 'ORDEM_PARANORMAL', jogador: 'Arthur', personagem: 'Lia', idade: null, retratoVersao: null,
        recursos: [{ codigo: 'PV', atual: 9, maximo: 12 }],
      },
    ];
    http.expectOne(`${API}/fichas`).flush(corpo);
    expect(resumo[0].sistema).toBe('ORDEM_PARANORMAL');
    expect(resumo[0].recursos[0].codigo).toBe('PV');
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
  it('buscarFicha faz GET /fichas/7', () => {
    api.buscarFicha(7).subscribe();
    const req = http.expectOne(`${API}/fichas/7`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('atualizarRecursos faz PATCH /fichas/7/recursos so com o que foi passado e avisa que mudou', () => {
    const antes = api.versaoFichas();
    api.atualizarRecursos(7, { PV: 10 }).subscribe();
    const req = http.expectOne(`${API}/fichas/7/recursos`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ PV: 10 });
    req.flush({});
    expect(api.versaoFichas()).toBe(antes + 1);
  });

  it('testar faz POST /fichas/7/testes com alvo e codigo', () => {
    let resultado: unknown;
    api.testar(7, { alvo: 'PERICIA', codigo: 'INVESTIGACAO' }).subscribe((r) => (resultado = r));
    const req = http.expectOne(`${API}/fichas/7/testes`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ alvo: 'PERICIA', codigo: 'INVESTIGACAO' });
    const corpo = { alvo: 'PERICIA', codigo: 'INVESTIGACAO', nome: 'Investigação', dados: [4, 17, 9], mantido: 17, bonus: 5, total: 22 };
    req.flush(corpo);
    expect(resultado).toEqual(corpo);
  });

  it('testar nao conta como alteracao da ficha', () => {
    const antes = api.versaoFichas();
    api.testar(7, { alvo: 'ATRIBUTO', codigo: 'AGI' }).subscribe();
    http.expectOne(`${API}/fichas/7/testes`).flush({});
    expect(api.versaoFichas()).toBe(antes);
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

  it('enviarRetrato faz PUT /fichas/7/retrato com o JPEG cru e avisa que as fichas mudaram', () => {
    const jpeg = new Blob([new Uint8Array([0xff, 0xd8, 0xff])], { type: 'image/jpeg' });
    api.enviarRetrato(7, jpeg).subscribe();
    const req = http.expectOne(`${API}/fichas/7/retrato`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.headers.get('Content-Type')).toBe('image/jpeg');
    expect(req.request.body).toBe(jpeg);
    req.flush(null, { status: 204, statusText: 'No Content' });
    expect(api.versaoFichas()).toBe(1);
  });

  it('removerRetrato faz DELETE /fichas/7/retrato e avisa que as fichas mudaram', () => {
    api.removerRetrato(7).subscribe();
    const req = http.expectOne(`${API}/fichas/7/retrato`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
    expect(api.versaoFichas()).toBe(1);
  });

  it('baixarRetrato faz GET /fichas/7/retrato?v=123 pedindo um blob', () => {
    api.baixarRetrato(7, 123).subscribe();
    const req = http.expectOne(`${API}/fichas/7/retrato?v=123`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['x']));
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
