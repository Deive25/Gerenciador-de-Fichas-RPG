import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WritableSignal, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { environment } from '../../environments/environment';
import { GoogleIdentity } from '../core/auth/google-identity';
import { Sessao } from '../core/auth/sessao';
import { respostaDe } from '../core/auth/teste-auth';
import { EstadoDoServidor } from '../core/servidor/estado-do-servidor';
import { Entrar } from './entrar';

const LOGIN = 'http://localhost:8080/auth/login';
const DEMO = 'http://localhost:8080/auth/demo';
const MENSAGEM_429 = 'Muitas tentativas. Tente de novo em alguns minutos.';

/** O botão do Google não baixa o script real nos testes. */
const googleSemRede = { provide: GoogleIdentity, useValue: { carregar: () => new Promise<never>(() => undefined) } };

describe('Entrar: demonstração e servidor acordando', () => {
  let ctl: HttpTestingController;
  let navegar: ReturnType<typeof vi.fn>;
  let aguardando: WritableSignal<boolean>;
  const flagOriginal = environment.demoAtiva;

  function montar(demoAtiva = true, aguarda = false) {
    environment.demoAtiva = demoAtiva;
    aguardando = signal(aguarda);
    sessionStorage.clear();
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        googleSemRede,
        { provide: EstadoDoServidor, useValue: { aguardando, quandoLiberado: () => new Promise<void>(() => undefined) } },
      ],
    });
    navegar = vi.fn().mockResolvedValue(true);
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockImplementation(navegar as never);
    ctl = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(Entrar);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    const botaoDemo = () => el.querySelector<HTMLButtonElement>('button.botao-demo');
    const botaoEntrar = () => el.querySelector<HTMLButtonElement>('button[type="submit"]')!;
    function digitar(seletor: string, valor: string) {
      const input = el.querySelector<HTMLInputElement>(seletor)!;
      input.value = valor;
      input.dispatchEvent(new Event('input'));
    }
    const enviarLogin = () => {
      digitar('input[type="email"]', 'ana@x.com');
      digitar('input[formcontrolname="senha"]', 'segredo123');
      el.querySelector('form')!.dispatchEvent(new Event('submit'));
    };
    return { fixture, el, botaoDemo, botaoEntrar, enviarLogin };
  }

  afterEach(() => {
    environment.demoAtiva = flagOriginal;
    ctl.verify();
  });

  it('mostra "Ver demonstração" abaixo do botão do Google', () => {
    const { el, botaoDemo } = montar();
    expect(botaoDemo()?.textContent?.trim()).toBe('Ver demonstração');
    const google = el.querySelector('app-botao-google')!;
    expect(google.compareDocumentPosition(botaoDemo()!) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('sem demoAtiva o botão não existe', () => {
    expect(montar(false).botaoDemo()).toBeNull();
  });

  it('clicar cria a sessão demo (no sessionStorage) e abre a campanha demo', () => {
    const { fixture, botaoDemo } = montar();
    botaoDemo()!.click();
    const req = ctl.expectOne(DEMO);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({ ...respostaDe('d'), campanhaId: 9 });
    fixture.detectChanges();
    expect(TestBed.inject(Sessao).usuario()?.demo).toBe(true);
    expect(sessionStorage.getItem('fichas-rpg.refresh')).toBe('refresh-d');
    expect(localStorage.getItem('fichas-rpg.refresh')).toBeNull();
    expect(navegar).toHaveBeenCalledWith('/campanhas/9');
  });

  it('trava o botão enquanto o pedido está em voo (sem pedido duplo)', () => {
    const { fixture, botaoDemo } = montar();
    botaoDemo()!.click();
    fixture.detectChanges();
    expect(botaoDemo()!.disabled).toBe(true);
    botaoDemo()!.click();
    ctl.expectOne(DEMO).flush({ ...respostaDe('d'), campanhaId: 9 });
  });

  it('429 mostra a mensagem da API e libera o botão', () => {
    const { fixture, el, botaoDemo } = montar();
    botaoDemo()!.click();
    ctl.expectOne(DEMO).flush({ title: 'Muitas tentativas', detail: MENSAGEM_429 }, { status: 429, statusText: 'x' });
    fixture.detectChanges();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain(MENSAGEM_429);
    expect(botaoDemo()!.disabled).toBe(false);
    expect(navegar).not.toHaveBeenCalled();
  });

  it('429 no login também mostra a mensagem do corpo', () => {
    const { fixture, el, enviarLogin } = montar();
    enviarLogin();
    ctl.expectOne(LOGIN).flush({ title: 'Muitas tentativas', detail: MENSAGEM_429 }, { status: 429, statusText: 'x' });
    fixture.detectChanges();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain(MENSAGEM_429);
  });

  it('404 (demo desligada na API) mostra o erro da API', () => {
    const { fixture, el, botaoDemo } = montar();
    botaoDemo()!.click();
    ctl.expectOne(DEMO).flush({ title: 'Não encontrado', detail: 'Recurso não encontrado.' }, { status: 404, statusText: 'x' });
    fixture.detectChanges();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Recurso não encontrado.');
  });

  it('entrar com conta normal depois da demo apaga o refresh demo da aba', () => {
    const { fixture, enviarLogin } = montar();
    TestBed.inject(Sessao).iniciar(respostaDe('d'), true);
    enviarLogin();
    ctl.expectOne(LOGIN).flush(respostaDe('n'));
    fixture.detectChanges();
    expect(sessionStorage.getItem('fichas-rpg.refresh')).toBeNull();
    expect(localStorage.getItem('fichas-rpg.refresh')).toBe('refresh-n');
  });

  describe('com o servidor acordando', () => {
    it('Entrar e Ver demonstração ficam desabilitados com "aguardando o servidor"', () => {
      const { botaoEntrar, botaoDemo } = montar(true, true);
      expect(botaoEntrar().disabled).toBe(true);
      expect(botaoEntrar().textContent).toMatch(/aguardando o servidor/i);
      expect(botaoDemo()!.disabled).toBe(true);
      expect(botaoDemo()!.textContent).toMatch(/aguardando o servidor/i);
    });

    it('volta ao normal quando a API responde', () => {
      const { fixture, botaoEntrar, botaoDemo } = montar(true, true);
      aguardando.set(false);
      fixture.detectChanges();
      expect(botaoEntrar().disabled).toBe(false);
      expect(botaoEntrar().textContent?.trim()).toBe('Entrar');
      expect(botaoDemo()!.disabled).toBe(false);
      expect(botaoDemo()!.textContent?.trim()).toBe('Ver demonstração');
    });

    it('o envio por Enter também é barrado enquanto aguarda', () => {
      const { enviarLogin } = montar(true, true);
      enviarLogin();
      ctl.expectNone(LOGIN);
    });
  });
});
