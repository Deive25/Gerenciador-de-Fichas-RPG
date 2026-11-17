import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { environment } from '../../environments/environment';
import { GoogleAccountsId, GoogleIdentity } from '../core/auth/google-identity';
import { Sessao } from '../core/auth/sessao';
import { respostaDe } from '../core/auth/teste-auth';
import { BotaoGoogle } from './botao-google';

const URL_GOOGLE = 'http://localhost:8080/auth/google';

describe('BotaoGoogle', () => {
  let fixture: ComponentFixture<BotaoGoogle>;
  let el: HTMLElement;
  let ctl: HttpTestingController;
  let navegar: ReturnType<typeof vi.fn>;
  let callback: (r: { credential: string }) => void;
  let configInicial: { client_id: string };
  let elementoRenderizado: HTMLElement;
  let opcoes: Record<string, unknown>;

  const falso: GoogleAccountsId = {
    initialize: (cfg) => {
      configInicial = cfg;
      callback = cfg.callback;
    },
    renderButton: (elemento, o) => {
      elementoRenderizado = elemento;
      opcoes = o;
    },
  };

  async function criar(carregar: () => Promise<GoogleAccountsId> = () => Promise.resolve(falso)) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GoogleIdentity, useValue: { carregar } },
      ],
    });
    navegar = vi.fn().mockResolvedValue(true);
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockImplementation(navegar as never);
    ctl = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(BotaoGoogle);
    fixture.componentRef.setInput('volta', '/convite/Abc123');
    fixture.detectChanges();
    await fixture.whenStable();
    await new Promise((r) => setTimeout(r)); // deixa o .then/.catch da promessa do script rodar
    fixture.detectChanges();
    el = fixture.nativeElement;
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => ctl.verify());

  it('renderiza o botão oficial com o client ID', async () => {
    await criar();
    expect(configInicial.client_id).toBe(environment.googleClientId);
    expect(elementoRenderizado).toBeInstanceOf(HTMLElement);
    expect(el.contains(elementoRenderizado)).toBe(true);
    expect(opcoes).toMatchObject({ theme: 'filled_black', text: 'continue_with', shape: 'pill', locale: 'pt-BR' });
    expect(opcoes['width']).toBeGreaterThanOrEqual(200);
    expect(opcoes['width']).toBeLessThanOrEqual(400);
  });

  it('credential vira POST /auth/google e o sucesso vai para a volta', async () => {
    await criar();
    callback({ credential: 'tok' });
    const req = ctl.expectOne(URL_GOOGLE);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ idToken: 'tok' });
    req.flush(respostaDe());
    expect(TestBed.inject(Sessao).estaLogado()).toBe(true);
    expect(navegar).toHaveBeenCalledWith('/convite/Abc123');
  });

  it('volta externo vai para /fichas', async () => {
    await criar();
    fixture.componentRef.setInput('volta', '//mal.com');
    callback({ credential: 'tok' });
    ctl.expectOne(URL_GOOGLE).flush(respostaDe());
    expect(navegar).toHaveBeenCalledWith('/fichas');
  });

  it('401 aparece em role=alert', async () => {
    await criar();
    callback({ credential: 'tok' });
    ctl.expectOne(URL_GOOGLE).flush(
      { title: 'Login com Google recusado', detail: 'Confirme o seu e-mail no Google antes de entrar.' },
      { status: 401, statusText: 'x' },
    );
    fixture.detectChanges();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Confirme o seu e-mail no Google antes de entrar.');
    expect(navegar).not.toHaveBeenCalled();
  });

  it('falha de rede mostra "Não foi possível falar com o servidor."', async () => {
    await criar();
    callback({ credential: 'tok' });
    ctl.expectOne(URL_GOOGLE).error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(el.querySelector('[role="alert"]')?.textContent?.trim()).toBe('Não foi possível falar com o servidor.');
  });

  it('script indisponível mostra o aviso', async () => {
    await criar(() => Promise.reject(new Error('sem rede')));
    expect(el.textContent).toContain('O login com o Google não está disponível agora. Use o e-mail e a senha.');
  });

  it('segundo callback em voo não gera outro POST', async () => {
    await criar();
    callback({ credential: 'tok' });
    callback({ credential: 'tok' });
    ctl.expectOne(URL_GOOGLE).flush(respostaDe());
  });

  it('depois de um erro, uma nova tentativa é aceita', async () => {
    await criar();
    callback({ credential: 'tok' });
    ctl.expectOne(URL_GOOGLE).flush({ title: 'x', detail: 'Falhou' }, { status: 401, statusText: 'x' });
    callback({ credential: 'tok2' });
    ctl.expectOne(URL_GOOGLE).flush(respostaDe());
  });
});
