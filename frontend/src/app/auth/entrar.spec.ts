import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Sessao } from '../core/auth/sessao';
import { respostaDe } from '../core/auth/teste-auth';
import { Entrar } from './entrar';

const LOGIN = 'http://localhost:8080/auth/login';

describe('Entrar', () => {
  let fixture: ComponentFixture<Entrar>;
  let el: HTMLElement;
  let ctl: HttpTestingController;
  let navegar: ReturnType<typeof vi.fn>;

  function digitar(seletor: string, valor: string) {
    const input = el.querySelector<HTMLInputElement>(seletor)!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  }
  function enviar() {
    el.querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }
  function preencher(email = 'ana@x.com', senha = 'segredo123') {
    digitar('input[type="email"]', email);
    digitar('input[formcontrolname="senha"]', senha);
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    navegar = vi.fn().mockResolvedValue(true);
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockImplementation(navegar as never);
    ctl = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Entrar);
    fixture.detectChanges();
    el = fixture.nativeElement;
  });
  afterEach(() => ctl.verify());

  it('tem os campos com autocomplete certo e o link para criar conta', () => {
    expect(el.querySelector('input[type="email"]')?.getAttribute('autocomplete')).toBe('email');
    expect(el.querySelector('input[formcontrolname="senha"]')?.getAttribute('autocomplete')).toBe('current-password');
    expect(el.querySelector('a[href="/cadastrar"]')).not.toBeNull();
  });

  it('campos vazios: mostra Obrigatório e não chama a API', () => {
    enviar();
    expect(el.textContent).toContain('Obrigatório');
    ctl.expectNone(LOGIN);
  });

  it('e-mail inválido mostra a mensagem e não chama a API', () => {
    preencher('sem-arroba', 'segredo123');
    enviar();
    expect(el.textContent).toContain('Informe um e-mail válido');
    ctl.expectNone(LOGIN);
  });

  it('sucesso: guarda a sessão e vai para a volta segura', () => {
    fixture.componentRef.setInput('volta', '/fichas/2');
    preencher();
    enviar();
    const req = ctl.expectOne(LOGIN);
    expect(req.request.body).toEqual({ email: 'ana@x.com', senha: 'segredo123' });
    req.flush(respostaDe());
    expect(TestBed.inject(Sessao).estaLogado()).toBe(true);
    expect(navegar).toHaveBeenCalledWith('/fichas/2');
  });

  it('volta externa vira /fichas', () => {
    fixture.componentRef.setInput('volta', '//mal.com');
    preencher();
    enviar();
    ctl.expectOne(LOGIN).flush(respostaDe());
    expect(navegar).toHaveBeenCalledWith('/fichas');
  });

  it('401 mostra a mensagem geral com role=alert e libera o formulário', () => {
    preencher();
    enviar();
    ctl.expectOne(LOGIN).flush({ title: 'Não autorizado', detail: 'E-mail ou senha incorretos' }, { status: 401, statusText: 'x' });
    fixture.detectChanges();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('E-mail ou senha incorretos');
    expect(el.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(false);
    expect(navegar).not.toHaveBeenCalled();
  });

  it('400 aparece no campo', () => {
    preencher();
    enviar();
    ctl.expectOne(LOGIN).flush({ title: 'Dados inválidos', errors: { email: 'E-mail inválido' } }, { status: 400, statusText: 'x' });
    fixture.detectChanges();
    expect(el.querySelector('mat-error')?.textContent).toContain('E-mail inválido');
    expect(el.querySelector('[role="alert"]')).toBeNull();
  });

  it('bloqueia campos e botão enquanto envia, e não envia duas vezes', () => {
    preencher();
    enviar();
    expect(el.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(true);
    expect(el.querySelector<HTMLInputElement>('input[type="email"]')!.disabled).toBe(true);
    enviar();
    ctl.expectOne(LOGIN).flush(respostaDe());
  });

  it('o botão mostrar senha alterna o tipo do campo e o aria-pressed', () => {
    const botao = el.querySelector<HTMLButtonElement>('button.mostrar-senha')!;
    const campo = el.querySelector<HTMLInputElement>('input[formcontrolname="senha"]')!;
    expect(campo.type).toBe('password');
    expect(botao.getAttribute('aria-pressed')).toBe('false');
    botao.click();
    fixture.detectChanges();
    expect(campo.type).toBe('text');
    expect(botao.getAttribute('aria-pressed')).toBe('true');
  });
});
