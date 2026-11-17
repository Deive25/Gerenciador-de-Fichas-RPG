import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Renovacao } from './renovacao';
import { Sessao } from './sessao';
import { respostaDe } from './teste-auth';

const REFRESH = 'http://localhost:8080/auth/refresh';

describe('Renovacao', () => {
  let renovacao: Renovacao;
  let sessao: Sessao;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    renovacao = TestBed.inject(Renovacao);
    sessao = TestBed.inject(Sessao);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    http.verify();
    vi.unstubAllGlobals();
  });

  it('sem refresh guardado devolve sem-sessao sem chamar a API', async () => {
    expect(await renovacao.renovar()).toBe('sem-sessao');
    http.expectNone(REFRESH);
  });

  it('renova: manda o refresh guardado e guarda o par novo', async () => {
    localStorage.setItem('fichas-rpg.refresh', 'velho');
    const p = renovacao.renovar();
    const req = http.expectOne(REFRESH);
    expect(req.request.body).toEqual({ refreshToken: 'velho' });
    req.flush(respostaDe('2'));
    expect(await p).toBe('renovada');
    expect(sessao.tokenDeAcesso()).toBe('acesso-2');
    expect(sessao.refreshGuardado()).toBe('refresh-2');
  });

  it('duas chamadas juntas geram UM POST /auth/refresh', async () => {
    localStorage.setItem('fichas-rpg.refresh', 'velho');
    const a = renovacao.renovar();
    const b = renovacao.renovar();
    http.expectOne(REFRESH).flush(respostaDe('2'));
    expect(await Promise.all([a, b])).toEqual(['renovada', 'renovada']);
  });

  it('depois de terminar, uma nova chamada renova de novo', async () => {
    localStorage.setItem('fichas-rpg.refresh', 'velho');
    const a = renovacao.renovar();
    http.expectOne(REFRESH).flush(respostaDe('2'));
    await a;
    const b = renovacao.renovar();
    const req = http.expectOne(REFRESH);
    expect(req.request.body).toEqual({ refreshToken: 'refresh-2' });
    req.flush(respostaDe('3'));
    expect(await b).toBe('renovada');
  });

  it('com a trava, relê o refresh dentro dela (outra aba pode ter renovado enquanto esperava)', async () => {
    localStorage.setItem('fichas-rpg.refresh', 'antes-da-espera');
    const request = vi.fn(async (nome: string, tarefa: () => Promise<unknown>) => {
      // Enquanto esta aba esperava na fila, outra aba gravou um refresh novo.
      localStorage.setItem('fichas-rpg.refresh', 'gravado-pela-outra-aba');
      return tarefa();
    });
    vi.stubGlobal('navigator', { locks: { request } });
    const p = renovacao.renovar();
    const req = http.expectOne(REFRESH);
    expect(request.mock.calls[0][0]).toBe('fichas-rpg-refresh');
    expect(req.request.body).toEqual({ refreshToken: 'gravado-pela-outra-aba' });
    req.flush(respostaDe('3'));
    expect(await p).toBe('renovada');
  });

  it('401 apaga o refresh e devolve sem-sessao', async () => {
    localStorage.setItem('fichas-rpg.refresh', 'velho');
    const p = renovacao.renovar();
    http.expectOne(REFRESH).flush({ title: 'Sessão expirada', detail: 'Entre de novo.' }, { status: 401, statusText: 'Unauthorized' });
    expect(await p).toBe('sem-sessao');
    expect(localStorage.getItem('fichas-rpg.refresh')).toBeNull();
  });

  it.each([0, 500, 503])('status %i mantém o refresh e devolve indisponivel', async (status) => {
    localStorage.setItem('fichas-rpg.refresh', 'velho');
    const p = renovacao.renovar();
    const req = http.expectOne(REFRESH);
    if (status === 0) {
      req.error(new ProgressEvent('error'));
    } else {
      req.flush(null, { status, statusText: 'Erro' });
    }
    expect(await p).toBe('indisponivel');
    expect(localStorage.getItem('fichas-rpg.refresh')).toBe('velho');
  });

  describe('sessão demo (sessionStorage)', () => {
    beforeEach(() => sessionStorage.clear());

    it('renova a conta demo lendo e regravando o refresh no sessionStorage', async () => {
      sessionStorage.setItem('fichas-rpg.refresh', 'demo-velho');
      const p = renovacao.renovar();
      const req = http.expectOne(REFRESH);
      expect(req.request.body).toEqual({ refreshToken: 'demo-velho' });
      req.flush(respostaDe('2'));
      expect(await p).toBe('renovada');
      expect(sessionStorage.getItem('fichas-rpg.refresh')).toBe('refresh-2');
      expect(localStorage.getItem('fichas-rpg.refresh')).toBeNull();
      expect(sessao.usuario()?.demo).toBe(true);
    });

    it('com a trava, relê o refresh demo dentro dela', async () => {
      sessionStorage.setItem('fichas-rpg.refresh', 'antes-da-espera');
      const request = vi.fn(async (nome: string, tarefa: () => Promise<unknown>) => {
        sessionStorage.setItem('fichas-rpg.refresh', 'gravado-depois');
        return tarefa();
      });
      vi.stubGlobal('navigator', { locks: { request } });
      const p = renovacao.renovar();
      const req = http.expectOne(REFRESH);
      expect(req.request.body).toEqual({ refreshToken: 'gravado-depois' });
      req.flush(respostaDe('3'));
      expect(await p).toBe('renovada');
    });

    it('401 na conta demo apaga o refresh do sessionStorage', async () => {
      sessionStorage.setItem('fichas-rpg.refresh', 'velho');
      const p = renovacao.renovar();
      http.expectOne(REFRESH).flush({ title: 'x', detail: 'y' }, { status: 401, statusText: 'Unauthorized' });
      expect(await p).toBe('sem-sessao');
      expect(sessionStorage.getItem('fichas-rpg.refresh')).toBeNull();
    });
  });
});
