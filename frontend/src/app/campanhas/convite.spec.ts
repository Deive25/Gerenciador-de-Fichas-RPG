import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { NEVER, of, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiError } from '../core/api-error';
import { authInterceptor } from '../core/auth/auth.interceptor';
import { Sessao } from '../core/auth/sessao';
import { respostaDe } from '../core/auth/teste-auth';
import { CampanhaApi } from './campanha-api';
import { CampanhaResumo, PreviaConvite } from './campanha.model';
import { ConviteTela } from './convite';

const PREVIA: PreviaConvite = { campanhaId: 3, campanha: 'Mesa de sexta', mestre: 'Bia' };
const INVALIDO: ApiError = {
  status: 404, titulo: 'Convite inválido ou desativado', detalhe: 'Convite inválido ou desativado', campos: {},
};
const INDISPONIVEL: ApiError = {
  status: 0, titulo: 'Servidor indisponível', detalhe: 'Ele pode estar acordando.', campos: {},
};
const MINHA: CampanhaResumo = { id: 3, nome: 'Mesa de sexta', papel: 'JOGADOR', mestre: { id: 2, nome: 'Bia' }, fichas: 0 };

@Component({ template: 'tela de entrar' })
class TelaEntrar {}

/** As promessas da tela (renovar a sessão, por exemplo) não entram no whenStable: deixa as pendências rodarem. */
async function aguardar(harness: RouterTestingHarness): Promise<void> {
  await new Promise<void>((resolve) => setTimeout(resolve, 0));
  await harness.fixture.whenStable();
  harness.detectChanges();
}

async function montar(api: Partial<Record<keyof CampanhaApi, unknown>>, logado: boolean) {
  localStorage.clear();
  TestBed.configureTestingModule({
    providers: [
      provideRouter([{ path: 'convite/:codigo', component: ConviteTela }], withComponentInputBinding()),
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: CampanhaApi, useValue: api },
    ],
  });
  if (logado) {
    TestBed.inject(Sessao).iniciar(respostaDe());
  }
  const harness = await RouterTestingHarness.create();
  const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  const tela = await harness.navigateByUrl('/convite/Abc123', ConviteTela);
  await aguardar(harness);
  return { harness, tela, el: harness.routeNativeElement as HTMLElement, navegar };
}

function botao(el: HTMLElement, texto: string): HTMLButtonElement {
  return Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes(texto)) as HTMLButtonElement;
}

describe('ConviteTela', () => {
  it('carregando: avisa com role=status', async () => {
    const { el } = await montar({ previa: () => NEVER, listar: () => NEVER }, true);
    expect(el.querySelector('[role="status"]')?.textContent).toContain('Carregando convite');
  });

  it('mostra a prévia (campanha e mestre) e busca pelo código da URL', async () => {
    const previa = vi.fn(() => of(PREVIA));
    const { el } = await montar({ previa, listar: () => of([]) }, true);
    expect(previa).toHaveBeenCalledWith('Abc123');
    expect(el.textContent).toContain('Mesa de sexta');
    expect(el.textContent).toContain('Bia');
  });

  it('código inválido: mostra a mensagem exata e não oferece entrar', async () => {
    const { el } = await montar({ previa: () => throwError(() => INVALIDO), listar: () => of([]) }, true);
    expect(el.querySelector('[role="alert"]')?.textContent).toContain(
      'Convite inválido ou desativado. Peça um link novo ao mestre.',
    );
    expect(botao(el, 'Entrar na campanha')).toBeUndefined();
  });

  it('erro de rede: mostra o motivo e tenta de novo', async () => {
    const previa = vi
      .fn()
      .mockReturnValueOnce(throwError(() => INDISPONIVEL))
      .mockReturnValueOnce(of(PREVIA));
    const { harness, el } = await montar({ previa, listar: () => of([]) }, true);
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Servidor indisponível');

    botao(el, 'Tentar de novo').click();
    await harness.fixture.whenStable();
    harness.detectChanges();

    expect(previa).toHaveBeenCalledTimes(2);
    expect(el.textContent).toContain('Mesa de sexta');
  });

  describe('sem sessão', () => {
    it('oferece Entrar e Criar conta, que voltam ao convite pelo ?volta=', async () => {
      const { el } = await montar({ previa: () => of(PREVIA) }, false);
      const entrar = Array.from(el.querySelectorAll('a')).find((a) => a.textContent?.includes('Entrar'));
      const criar = Array.from(el.querySelectorAll('a')).find((a) => a.textContent?.includes('Criar conta'));
      expect(entrar?.getAttribute('href')).toBe('/entrar?volta=%2Fconvite%2FAbc123');
      expect(criar?.getAttribute('href')).toBe('/cadastrar?volta=%2Fconvite%2FAbc123');
      expect(botao(el, 'Entrar na campanha')).toBeUndefined();
    });

    it('não chama o aceitar nem a lista de campanhas', async () => {
      const aceitar = vi.fn();
      const listar = vi.fn();
      await montar({ previa: () => of(PREVIA), aceitar, listar }, false);
      expect(aceitar).not.toHaveBeenCalled();
      expect(listar).not.toHaveBeenCalled();
    });
  });

  describe('com sessão', () => {
    it('Entrar na campanha aceita o convite e abre a campanha', async () => {
      const aceitar = vi.fn(() => of({ campanhaId: 3 }));
      const { harness, el, navegar } = await montar({ previa: () => of(PREVIA), listar: () => of([]), aceitar }, true);

      botao(el, 'Entrar na campanha').click();
      await harness.fixture.whenStable();

      expect(aceitar).toHaveBeenCalledWith('Abc123');
      expect(navegar).toHaveBeenCalledWith(['/campanhas', 3]);
    });

    it('quem já é membro vê o aviso e um atalho, sem o botão de entrar', async () => {
      const { el } = await montar({ previa: () => of(PREVIA), listar: () => of([MINHA]) }, true);
      expect(el.textContent).toContain('Você já está nesta campanha');
      expect(el.querySelector('a[href="/campanhas/3"]')).not.toBeNull();
      expect(botao(el, 'Entrar na campanha')).toBeUndefined();
    });

    it('erro ao aceitar aparece num alerta e libera o botão', async () => {
      const aceitar = vi.fn(() => throwError(() => INDISPONIVEL));
      const { harness, el } = await montar({ previa: () => of(PREVIA), listar: () => of([]), aceitar }, true);

      botao(el, 'Entrar na campanha').click();
      await harness.fixture.whenStable();
      harness.detectChanges();

      expect(el.querySelector('p[role="alert"]')?.textContent).toContain('Servidor indisponível');
      expect(botao(el, 'Entrar na campanha').disabled).toBe(false);
    });
  });
});

describe('ConviteTela: sessão vencida (Foco 5)', () => {
  it('o 401 sem refresh válido leva a /entrar?volta=/convite/:codigo', async () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter(
          [
            { path: 'convite/:codigo', component: ConviteTela },
            { path: 'entrar', component: TelaEntrar },
          ],
          withComponentInputBinding(),
        ),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    // A pessoa tinha sessão, mas o token de acesso venceu e o refresh também.
    TestBed.inject(Sessao).iniciar(respostaDe());
    const http = TestBed.inject(HttpTestingController);
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/convite/Abc123', ConviteTela);
    http
      .expectOne(`${environment.apiUrl}/convites/Abc123`)
      .flush({ title: 'Não autenticado' }, { status: 401, statusText: 'Unauthorized' });
    http
      .expectOne(`${environment.apiUrl}/auth/refresh`)
      .flush({ title: 'Sessão expirada' }, { status: 401, statusText: 'Unauthorized' });
    await aguardar(harness);

    const router = TestBed.inject(Router);
    expect(router.url).toBe('/entrar?volta=%2Fconvite%2FAbc123');
  });

  it('recarregar a página com refresh guardado restaura a sessão antes de mostrar o convite', async () => {
    localStorage.clear();
    localStorage.setItem('fichas-rpg.refresh', 'refresh-1');
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'convite/:codigo', component: ConviteTela }], withComponentInputBinding()),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    const http = TestBed.inject(HttpTestingController);
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/convite/Abc123', ConviteTela);
    // Sem token na memória: primeiro troca o refresh, depois pede a prévia já com o token novo.
    http.expectOne(`${environment.apiUrl}/auth/refresh`).flush(respostaDe('2'));
    await aguardar(harness);
    const previa = http.expectOne(`${environment.apiUrl}/convites/Abc123`);
    expect(previa.request.headers.get('Authorization')).toBe('Bearer acesso-2');
    previa.flush(PREVIA);
    http.expectOne(`${environment.apiUrl}/campanhas`).flush([]);
    await aguardar(harness);

    expect((harness.routeNativeElement as HTMLElement).textContent).toContain('Entrar na campanha');
    http.verify();
  });
});
