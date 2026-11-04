import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { routes } from './app.routes';
import { FichaApi } from './fichas/ficha-api';
import { Painel } from './fichas/painel/painel';

beforeEach(() => {
  TestBed.configureTestingModule({
    providers: [
      provideRouter(routes),
      { provide: FichaApi, useValue: { listar: vi.fn(() => of([])), versaoFichas: signal(0) } },
    ],
  });
});

describe('routes', () => {
  // Substitui os testes do FichaInicio (celular x computador): não há mais variação por largura.
  it('a rota /fichas abre o painel', async () => {
    const harness = await RouterTestingHarness.create();
    const tela = await harness.navigateByUrl('/fichas', Painel);
    expect(tela).toBeInstanceOf(Painel);
  });

  it('a rota vazia redireciona para /fichas', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/');
    expect(TestBed.inject(Router).url).toBe('/fichas');
  });
});
