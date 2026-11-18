import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WritableSignal, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { environment } from '../../environments/environment';
import { Sessao } from '../core/auth/sessao';
import { respostaDe } from '../core/auth/teste-auth';
import { EstadoDoServidor } from '../core/servidor/estado-do-servidor';
import { CampanhaApi } from './campanha-api';
import { PreviaConvite } from './campanha.model';
import { ConviteTela } from './convite';

const DEMO = 'http://localhost:8080/auth/demo';
const PREVIA_DEMO: PreviaConvite = { campanhaId: 5, campanha: 'Mesa de demonstração', mestre: 'Visitante 1234', demo: true };
const PREVIA_REAL: PreviaConvite = { campanhaId: 3, campanha: 'Mesa de sexta', mestre: 'Bia', demo: false };

describe('ConviteTela: convite de demonstração', () => {
  let ctl: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;
  let aguardando: WritableSignal<boolean>;
  const flagOriginal = environment.demoAtiva;

  async function montar(previa: PreviaConvite, opcoes: { logado?: boolean; demoAtiva?: boolean; aguarda?: boolean } = {}) {
    environment.demoAtiva = opcoes.demoAtiva ?? true;
    aguardando = signal(opcoes.aguarda ?? false);
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'convite/:codigo', component: ConviteTela }], withComponentInputBinding()),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: CampanhaApi, useValue: { previa: () => of(previa), listar: () => of([]) } },
        { provide: EstadoDoServidor, useValue: { aguardando, quandoLiberado: () => Promise.resolve() } },
      ],
    });
    if (opcoes.logado) {
      TestBed.inject(Sessao).iniciar(respostaDe());
    }
    ctl = TestBed.inject(HttpTestingController);
    const harness = await RouterTestingHarness.create();
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true) as never;
    await harness.navigateByUrl('/convite/Abc123', ConviteTela);
    await new Promise<void>((r) => setTimeout(r, 0));
    await harness.fixture.whenStable();
    harness.detectChanges();
    const el = harness.routeNativeElement as HTMLElement;
    const botaoDemo = () => el.querySelector<HTMLButtonElement>('button.botao-demo');
    return { harness, el, botaoDemo };
  }

  afterEach(() => {
    environment.demoAtiva = flagOriginal;
    ctl.verify();
  });

  it('sem sessão e com convite demo: oferece "Entrar como jogador de demonstração"', async () => {
    const { botaoDemo } = await montar(PREVIA_DEMO);
    expect(botaoDemo()?.textContent?.trim()).toBe('Entrar como jogador de demonstração');
  });

  it('clicar chama /auth/demo com o código, guarda a sessão demo e abre a campanha', async () => {
    const { harness, botaoDemo } = await montar(PREVIA_DEMO);
    botaoDemo()!.click();
    const req = ctl.expectOne(DEMO);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ codigoConvite: 'Abc123' });
    req.flush({ ...respostaDe('j'), campanhaId: 5 });
    harness.detectChanges();
    expect(TestBed.inject(Sessao).usuario()?.demo).toBe(true);
    expect(sessionStorage.getItem('fichas-rpg.refresh')).toBe('refresh-j');
    expect(localStorage.getItem('fichas-rpg.refresh')).toBeNull();
    expect(navegar).toHaveBeenCalledWith(['/campanhas', 5]);
  });

  it('erro (429 ou 404) mostra a mensagem da API e libera o botão', async () => {
    const { harness, el, botaoDemo } = await montar(PREVIA_DEMO);
    botaoDemo()!.click();
    ctl.expectOne(DEMO).flush(
      { title: 'Muitas tentativas', detail: 'Muitas tentativas. Tente de novo em alguns minutos.' },
      { status: 429, statusText: 'x' },
    );
    harness.detectChanges();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Muitas tentativas. Tente de novo em alguns minutos.');
    expect(botaoDemo()!.disabled).toBe(false);
    expect(navegar).not.toHaveBeenCalled();
  });

  it('convite de campanha real não mostra o botão de demonstração', async () => {
    expect((await montar(PREVIA_REAL)).botaoDemo()).toBeNull();
  });

  it('prévia sem o campo demo (API antiga) também não mostra', async () => {
    const { demo: _, ...semCampo } = PREVIA_DEMO;
    expect((await montar(semCampo)).botaoDemo()).toBeNull();
  });

  it('com demoAtiva desligada não mostra', async () => {
    expect((await montar(PREVIA_DEMO, { demoAtiva: false })).botaoDemo()).toBeNull();
  });

  it('com sessão não mostra (quem já entrou usa "Entrar na campanha")', async () => {
    const { botaoDemo, el } = await montar(PREVIA_DEMO, { logado: true });
    expect(botaoDemo()).toBeNull();
    expect(el.textContent).toContain('Entrar na campanha');
  });

  it('com o servidor acordando o botão fica desabilitado com "aguardando o servidor"', async () => {
    const { harness, botaoDemo } = await montar(PREVIA_DEMO, { aguarda: true });
    expect(botaoDemo()!.disabled).toBe(true);
    expect(botaoDemo()!.textContent).toMatch(/aguardando o servidor/i);
    aguardando.set(false);
    harness.detectChanges();
    expect(botaoDemo()!.disabled).toBe(false);
  });
});
