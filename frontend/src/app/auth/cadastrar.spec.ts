import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Sessao } from '../core/auth/sessao';
import { respostaDe } from '../core/auth/teste-auth';
import { Cadastrar } from './cadastrar';

const REGISTRO = 'http://localhost:8080/auth/registro';

describe('Cadastrar', () => {
  let fixture: ComponentFixture<Cadastrar>;
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
  function preencher(nome = 'Ana', email = 'ana@x.com', senha = 'segredo123') {
    digitar('input[formcontrolname="nome"]', nome);
    digitar('input[type="email"]', email);
    digitar('input[formcontrolname="senha"]', senha);
  }
  const erroDoEmail = () => el.querySelector('mat-form-field:has(input[type="email"]) mat-error')?.textContent;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    navegar = vi.fn().mockResolvedValue(true);
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockImplementation(navegar as never);
    ctl = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Cadastrar);
    fixture.detectChanges();
    el = fixture.nativeElement;
  });
  afterEach(() => ctl.verify());

  it('tem os autocomplete da spec e o link para entrar', () => {
    expect(el.querySelector('input[formcontrolname="nome"]')?.getAttribute('autocomplete')).toBe('name');
    expect(el.querySelector('input[type="email"]')?.getAttribute('autocomplete')).toBe('email');
    expect(el.querySelector('input[formcontrolname="senha"]')?.getAttribute('autocomplete')).toBe('new-password');
    expect(el.querySelector('a[href="/entrar"]')).not.toBeNull();
  });

  it('campos vazios: Obrigatório e nada é enviado', () => {
    enviar();
    expect(el.querySelectorAll('mat-error').length).toBe(3);
    ctl.expectNone(REGISTRO);
  });

  it('senha curta mostra o mínimo e não envia', () => {
    preencher('Ana', 'ana@x.com', '1234567');
    enviar();
    expect(el.textContent).toContain('Mínimo de 8 caracteres');
    ctl.expectNone(REGISTRO);
  });

  it('senha com mais de 72 caracteres mostra o máximo', () => {
    preencher('Ana', 'ana@x.com', 'a'.repeat(73));
    enviar();
    expect(el.textContent).toContain('Máximo de 72 caracteres');
    ctl.expectNone(REGISTRO);
  });

  it('sucesso: já entra na conta e navega', () => {
    preencher('  Ana  ', 'ana@x.com', 'segredo123');
    enviar();
    const req = ctl.expectOne(REGISTRO);
    expect(req.request.body).toEqual({ nome: 'Ana', email: 'ana@x.com', senha: 'segredo123' });
    req.flush(respostaDe(), { status: 201, statusText: 'Created' });
    expect(TestBed.inject(Sessao).estaLogado()).toBe(true);
    expect(navegar).toHaveBeenCalledWith('/fichas');
  });

  it('409 aparece no campo de e-mail', () => {
    preencher();
    enviar();
    ctl.expectOne(REGISTRO).flush(
      { title: 'Conflito', detail: 'E-mail já cadastrado', errors: { email: 'E-mail já cadastrado' } },
      { status: 409, statusText: 'x' },
    );
    fixture.detectChanges();
    expect(erroDoEmail()).toContain('E-mail já cadastrado');
    expect(el.querySelector('[role="alert"]')).toBeNull();
  });

  it('409 sem errors também vai para o campo de e-mail', () => {
    preencher();
    enviar();
    ctl.expectOne(REGISTRO).flush({ title: 'Conflito', detail: 'E-mail já cadastrado' }, { status: 409, statusText: 'x' });
    fixture.detectChanges();
    expect(erroDoEmail()).toContain('E-mail já cadastrado');
  });

  it('400 por campo aparece em cada campo', () => {
    preencher();
    enviar();
    ctl.expectOne(REGISTRO).flush(
      { title: 'Dados inválidos', errors: { nome: 'Nome inválido', senha: 'Senha fraca' } },
      { status: 400, statusText: 'x' },
    );
    fixture.detectChanges();
    const texto = el.textContent ?? '';
    expect(texto).toContain('Nome inválido');
    expect(texto).toContain('Senha fraca');
  });

  it('erro sem campo (servidor fora do ar) vai para a mensagem geral', () => {
    preencher();
    enviar();
    ctl.expectOne(REGISTRO).error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Não foi possível falar com o servidor');
  });

  it('bloqueia o formulário enquanto envia', () => {
    preencher();
    enviar();
    expect(el.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(true);
    expect(el.querySelector<HTMLInputElement>('input[formcontrolname="nome"]')!.disabled).toBe(true);
    ctl.expectOne(REGISTRO).flush(respostaDe());
  });

  it('mostrar senha alterna o tipo e o aria-pressed', () => {
    const botao = el.querySelector<HTMLButtonElement>('button.mostrar-senha')!;
    botao.click();
    fixture.detectChanges();
    expect(el.querySelector<HTMLInputElement>('input[formcontrolname="senha"]')!.type).toBe('text');
    expect(botao.getAttribute('aria-pressed')).toBe('true');
  });
});
