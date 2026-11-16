import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { Sessao } from './sessao';
import { respostaDe } from './teste-auth';

const API = 'http://localhost:8080';
const NAO_AUTORIZADO = { status: 401, statusText: 'Unauthorized' };

describe('authInterceptor', () => {
  let http: HttpClient;
  let ctl: HttpTestingController;
  let sessao: Sessao;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    ctl = TestBed.inject(HttpTestingController);
    sessao = TestBed.inject(Sessao);
    router = TestBed.inject(Router);
    sessao.iniciar(respostaDe('1'));
  });
  afterEach(() => ctl.verify());

  it('põe o Bearer nas chamadas à API', () => {
    http.get(`${API}/fichas`).subscribe();
    expect(ctl.expectOne(`${API}/fichas`).request.headers.get('Authorization')).toBe('Bearer acesso-1');
  });

  it('não põe o cabeçalho fora da API nem em /auth/', () => {
    http.get('https://outro.exemplo/x').subscribe();
    expect(ctl.expectOne('https://outro.exemplo/x').request.headers.has('Authorization')).toBe(false);
    http.post(`${API}/auth/login`, {}).subscribe();
    expect(ctl.expectOne(`${API}/auth/login`).request.headers.has('Authorization')).toBe(false);
  });

  it('não põe o cabeçalho num host que só começa com o endereço da API', () => {
    http.get(`${API}.outro.exemplo/fichas`).subscribe();
    expect(ctl.expectOne(`${API}.outro.exemplo/fichas`).request.headers.has('Authorization')).toBe(false);
  });

  it('num 401 renova uma vez e repete a chamada com o token novo', async () => {
    let resposta: unknown;
    http.get(`${API}/fichas`).subscribe((r) => (resposta = r));
    ctl.expectOne(`${API}/fichas`).flush(null, NAO_AUTORIZADO);
    await vi.waitFor(() => ctl.expectOne(`${API}/auth/refresh`).flush(respostaDe('2')));
    const repetida = await vi.waitFor(() => ctl.expectOne(`${API}/fichas`));
    expect(repetida.request.headers.get('Authorization')).toBe('Bearer acesso-2');
    repetida.flush([{ id: 1 }]);
    expect(resposta).toEqual([{ id: 1 }]);
  });

  it('duas chamadas com 401 juntas geram um só POST /auth/refresh', async () => {
    http.get(`${API}/fichas`).subscribe();
    http.get(`${API}/fichas/2`).subscribe();
    ctl.expectOne(`${API}/fichas`).flush(null, NAO_AUTORIZADO);
    ctl.expectOne(`${API}/fichas/2`).flush(null, NAO_AUTORIZADO);
    await vi.waitFor(() => ctl.expectOne(`${API}/auth/refresh`).flush(respostaDe('2')));
    await vi.waitFor(() => {
      ctl.expectOne(`${API}/fichas`).flush([]);
      ctl.expectOne(`${API}/fichas/2`).flush({});
    });
  });

  it('se a repetida também der 401, propaga sem renovar de novo (sem laço)', async () => {
    let erro: unknown;
    http.get(`${API}/fichas`).subscribe({ error: (e) => (erro = e) });
    ctl.expectOne(`${API}/fichas`).flush(null, NAO_AUTORIZADO);
    await vi.waitFor(() => ctl.expectOne(`${API}/auth/refresh`).flush(respostaDe('2')));
    const repetida = await vi.waitFor(() => ctl.expectOne(`${API}/fichas`));
    repetida.flush(null, NAO_AUTORIZADO);
    expect((erro as { status: number }).status).toBe(401);
    ctl.expectNone(`${API}/auth/refresh`);
  });

  it('renovação recusada (401): limpa a sessão, vai para /entrar?volta= e propaga o erro', async () => {
    const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    vi.spyOn(router, 'url', 'get').mockReturnValue('/fichas/2');
    let erro: unknown;
    http.get(`${API}/fichas/2`).subscribe({ error: (e) => (erro = e) });
    ctl.expectOne(`${API}/fichas/2`).flush(null, NAO_AUTORIZADO);
    await vi.waitFor(() => ctl.expectOne(`${API}/auth/refresh`).flush(null, NAO_AUTORIZADO));
    await vi.waitFor(() => expect(erro).toBeDefined());
    expect(navegar).toHaveBeenCalledWith(['/entrar'], { queryParams: { volta: '/fichas/2' } });
    expect(localStorage.getItem('fichas-rpg.refresh')).toBeNull();
  });

  it('API indisponível na renovação: propaga o erro original, mantém o refresh e não navega', async () => {
    const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    let erro: { status: number } | undefined;
    http.get(`${API}/fichas`).subscribe({ error: (e) => (erro = e) });
    ctl.expectOne(`${API}/fichas`).flush(null, NAO_AUTORIZADO);
    await vi.waitFor(() => ctl.expectOne(`${API}/auth/refresh`).flush(null, { status: 503, statusText: 'x' }));
    await vi.waitFor(() => expect(erro?.status).toBe(401));
    expect(navegar).not.toHaveBeenCalled();
    expect(localStorage.getItem('fichas-rpg.refresh')).toBe('refresh-1');
  });

  it('o 401 de /auth/login (senha errada) NÃO tenta renovar nem navega', () => {
    const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    let erro: { status: number } | undefined;
    http.post(`${API}/auth/login`, {}).subscribe({ error: (e) => (erro = e) });
    ctl.expectOne(`${API}/auth/login`).flush(null, NAO_AUTORIZADO);
    expect(erro?.status).toBe(401);
    ctl.expectNone(`${API}/auth/refresh`);
    expect(navegar).not.toHaveBeenCalled();
  });

  it('erros que não são 401 passam direto', () => {
    let erro: { status: number } | undefined;
    http.get(`${API}/fichas`).subscribe({ error: (e) => (erro = e) });
    ctl.expectOne(`${API}/fichas`).flush(null, { status: 404, statusText: 'x' });
    expect(erro?.status).toBe(404);
    ctl.expectNone(`${API}/auth/refresh`);
  });
});
