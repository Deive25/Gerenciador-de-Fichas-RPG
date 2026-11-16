import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { exigeSessao, soConvidado, voltaSegura } from './guards';
import { Sessao } from './sessao';
import { respostaDe } from './teste-auth';

@Component({ template: 'ficha' })
class Ficha {}
@Component({ template: 'entrar' })
class Entrar {}

const REFRESH = 'http://localhost:8080/auth/refresh';

describe('voltaSegura', () => {
  it.each([
    ['/fichas/2', '/fichas/2'],
    ['/fichas/2?aba=3', '/fichas/2?aba=3'],
    ['//mal.com', '/fichas'],
    ['https://mal.com', '/fichas'],
    ['fichas', '/fichas'],
    ['', '/fichas'],
    [null, '/fichas'],
    [undefined, '/fichas'],
  ])('%s -> %s', (entrada, saida) => {
    expect(voltaSegura(entrada)).toBe(saida);
  });
});

describe('guards', () => {
  let ctl: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: '', canActivateChild: [exigeSessao], children: [{ path: 'fichas/:id', component: Ficha }] },
          { path: 'entrar', component: Entrar, canActivate: [soConvidado] },
          { path: 'fichas', component: Ficha },
        ]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    ctl = TestBed.inject(HttpTestingController);
  });
  afterEach(() => ctl.verify());

  it('exigeSessao: logado abre a rota', async () => {
    TestBed.inject(Sessao).iniciar(respostaDe());
    const h = await RouterTestingHarness.create();
    await h.navigateByUrl('/fichas/2', Ficha);
    expect(TestBed.inject(Router).url).toBe('/fichas/2');
  });

  it('exigeSessao: sem sessão nem refresh vai para /entrar com a volta', async () => {
    const h = await RouterTestingHarness.create();
    await h.navigateByUrl('/fichas/2');
    expect(TestBed.inject(Router).url).toBe('/entrar?volta=%2Ffichas%2F2');
  });

  it('exigeSessao: recarregar /fichas/2 com refresh guardado renova e continua em /fichas/2', async () => {
    localStorage.setItem('fichas-rpg.refresh', 'guardado');
    const h = await RouterTestingHarness.create();
    const nav = h.navigateByUrl('/fichas/2', Ficha);
    await vi.waitFor(() => ctl.expectOne(REFRESH).flush(respostaDe('2')));
    await nav;
    expect(TestBed.inject(Router).url).toBe('/fichas/2');
    expect(TestBed.inject(Sessao).estaLogado()).toBe(true);
  });

  it('exigeSessao: refresh recusado vai para /entrar', async () => {
    localStorage.setItem('fichas-rpg.refresh', 'velho');
    const h = await RouterTestingHarness.create();
    const nav = h.navigateByUrl('/fichas/2');
    await vi.waitFor(() => ctl.expectOne(REFRESH).flush(null, { status: 401, statusText: 'x' }));
    await nav;
    expect(TestBed.inject(Router).url).toBe('/entrar?volta=%2Ffichas%2F2');
  });

  it('exigeSessao: API indisponível deixa abrir a rota', async () => {
    localStorage.setItem('fichas-rpg.refresh', 'guardado');
    const h = await RouterTestingHarness.create();
    const nav = h.navigateByUrl('/fichas/2', Ficha);
    await vi.waitFor(() => ctl.expectOne(REFRESH).flush(null, { status: 503, statusText: 'x' }));
    await nav;
    expect(TestBed.inject(Router).url).toBe('/fichas/2');
  });

  it('soConvidado: logado vai para /fichas', async () => {
    TestBed.inject(Sessao).iniciar(respostaDe());
    const h = await RouterTestingHarness.create();
    await h.navigateByUrl('/entrar');
    expect(TestBed.inject(Router).url).toBe('/fichas');
  });

  it('soConvidado: sem sessão abre /entrar', async () => {
    const h = await RouterTestingHarness.create();
    await h.navigateByUrl('/entrar', Entrar);
    expect(TestBed.inject(Router).url).toBe('/entrar');
  });

  it('soConvidado: refresh guardado e válido (renovada) vai para /fichas', async () => {
    localStorage.setItem('fichas-rpg.refresh', 'guardado');
    const h = await RouterTestingHarness.create();
    const nav = h.navigateByUrl('/entrar');
    await vi.waitFor(() => ctl.expectOne(REFRESH).flush(respostaDe('2')));
    await nav;
    expect(TestBed.inject(Router).url).toBe('/fichas');
  });
});
