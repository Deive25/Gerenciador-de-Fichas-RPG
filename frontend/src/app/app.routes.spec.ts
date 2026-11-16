import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { NEVER, of } from 'rxjs';
import { Cadastrar } from './auth/cadastrar';
import { Entrar } from './auth/entrar';
import { routes } from './app.routes';
import { Sessao } from './core/auth/sessao';
import { EscolhaSistema } from './fichas/criacao/escolha-sistema';
import { EdicaoFicha } from './fichas/edicao/edicao-ficha';
import { FichaApi } from './fichas/ficha-api';
import { Painel } from './fichas/painel/painel';
import { FormularioDnd } from './fichas/sistemas/dnd/formulario-dnd';
import { FormularioOrdem } from './fichas/sistemas/ordem/formulario-ordem';

beforeEach(() => {
  // As rotas de fichas agora ficam atrás do guard exigeSessao: os testes abrem como pessoa logada.
  localStorage.clear();
  TestBed.configureTestingModule({
    providers: [
      provideRouter(routes, withComponentInputBinding()),
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: FichaApi, useValue: { listar: vi.fn(() => of([])), buscarFicha: vi.fn(() => NEVER), versaoFichas: signal(0) } },
    ],
  });
  TestBed.inject(Sessao).iniciar({ accessToken: 'a', refreshToken: 'r', usuario: { id: 1, nome: 'Ana', email: 'ana@x.com' } });
});

describe('routes', () => {
  // Substitui os testes do FichaInicio (celular x computador): não há mais variação por largura.
  it('a rota /fichas abre o painel', async () => {
    const harness = await RouterTestingHarness.create();
    const tela = await harness.navigateByUrl('/fichas', Painel);
    expect(tela).toBeInstanceOf(Painel);
  });

  it('a rota /fichas/nova abre a escolha do sistema', async () => {
    const harness = await RouterTestingHarness.create();
    const tela = await harness.navigateByUrl('/fichas/nova', EscolhaSistema);
    expect(tela).toBeInstanceOf(EscolhaSistema);
  });

  it('a rota /fichas/nova/ordem-paranormal abre o formulario de Ordem', async () => {
    const harness = await RouterTestingHarness.create();
    const tela = await harness.navigateByUrl('/fichas/nova/ordem-paranormal', FormularioOrdem);
    expect(tela).toBeInstanceOf(FormularioOrdem);
  });

  it('a rota /fichas/nova/dnd-2024 abre o formulario de D&D com a edicao 2024', async () => {
    const harness = await RouterTestingHarness.create();
    const tela = await harness.navigateByUrl('/fichas/nova/dnd-2024', FormularioDnd);
    expect(tela).toBeInstanceOf(FormularioDnd);
    expect(tela.edicao()).toBe('D2024');
  });

  it('a rota /fichas/nova/dnd-2014 abre o formulario de D&D com a edicao 2014', async () => {
    const harness = await RouterTestingHarness.create();
    const tela = await harness.navigateByUrl('/fichas/nova/dnd-2014', FormularioDnd);
    expect(tela.edicao()).toBe('D2014');
  });

  it('a rota /fichas/9/editar abre a edicao, que escolhe o formulario pelo sistema', async () => {
    const harness = await RouterTestingHarness.create();
    const tela = await harness.navigateByUrl('/fichas/9/editar', EdicaoFicha);
    expect(tela).toBeInstanceOf(EdicaoFicha);
    expect(tela.id()).toBe('9');
  });

  it('a rota vazia redireciona para /fichas', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/');
    expect(TestBed.inject(Router).url).toBe('/fichas');
  });

  it('qualquer rota desconhecida redireciona para /fichas', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/nao-existe');
    expect(TestBed.inject(Router).url).toBe('/fichas');
  });

  describe('sem sessão', () => {
    beforeEach(() => TestBed.inject(Sessao).limpar());

    it('as rotas de fichas mandam para /entrar com a volta', async () => {
      const harness = await RouterTestingHarness.create();
      await harness.navigateByUrl('/fichas/2/editar');
      expect(TestBed.inject(Router).url).toBe('/entrar?volta=%2Ffichas%2F2%2Feditar');
    });

    it('/entrar abre a tela de entrar', async () => {
      const harness = await RouterTestingHarness.create();
      const tela = await harness.navigateByUrl('/entrar', Entrar);
      expect(tela).toBeInstanceOf(Entrar);
    });

    it('/cadastrar abre a tela de criar conta', async () => {
      const harness = await RouterTestingHarness.create();
      const tela = await harness.navigateByUrl('/cadastrar', Cadastrar);
      expect(tela).toBeInstanceOf(Cadastrar);
    });

    it('o volta da URL chega ao input da tela de entrar', async () => {
      const harness = await RouterTestingHarness.create();
      const tela = await harness.navigateByUrl('/entrar?volta=%2Ffichas%2F2', Entrar);
      expect(tela.volta()).toBe('/fichas/2');
    });
  });

  it('logado, /entrar e /cadastrar mandam para /fichas', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/entrar');
    expect(TestBed.inject(Router).url).toBe('/fichas');
    await harness.navigateByUrl('/cadastrar');
    expect(TestBed.inject(Router).url).toBe('/fichas');
  });
});
