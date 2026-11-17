import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router, provideRouter } from '@angular/router';
import { Sessao } from '../core/auth/sessao';
import { respostaDe } from '../core/auth/teste-auth';
import { BotaoGoogle } from './botao-google';
import { Cadastrar } from './cadastrar';

const REGISTRO = 'http://localhost:8080/auth/registro';
const SENHA = 'dragão vermelho na taverna';

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
  function preencher(nome = 'Ana', email = 'ana@x.com', senha = SENHA) {
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

  it('senha curta mostra que os requisitos não foram atendidos e não envia', () => {
    preencher('Ana', 'ana@x.com', '1234567');
    enviar();
    expect(el.textContent).toContain('Atenda aos requisitos abaixo');
    ctl.expectNone(REGISTRO);
  });

  it('senha com mais de 72 caracteres mostra o máximo', () => {
    preencher('Ana', 'ana@x.com', 'a'.repeat(73));
    enviar();
    expect(el.textContent).toContain('Máximo de 72 caracteres');
    ctl.expectNone(REGISTRO);
  });

  it('sucesso: já entra na conta e navega', () => {
    preencher('  Ana  ', 'ana@x.com', SENHA);
    enviar();
    const req = ctl.expectOne(REGISTRO);
    expect(req.request.body).toEqual({ nome: 'Ana', email: 'ana@x.com', senha: SENHA });
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

  describe('requisitos da senha', () => {
    const lista = () => el.querySelector('ul.requisitos');
    const item = (texto: string) => [...el.querySelectorAll('ul.requisitos li')].find((li) => li.textContent?.includes(texto))!;
    const marcador = (texto: string) => item(texto).querySelector('[aria-hidden="true"]')!.textContent?.trim();
    const digitarSenha = (valor: string) => {
      digitar('input[formcontrolname="senha"]', valor);
      fixture.detectChanges();
    };

    it('a lista fica dentro de um contêiner aria-live=polite e só aparece depois de digitar a senha', () => {
      expect(el.querySelector('[aria-live="polite"]')).not.toBeNull();
      expect(lista()).toBeNull();
      digitarSenha('d');
      expect(lista()).not.toBeNull();
      expect(el.querySelector('[aria-live="polite"] ul.requisitos')).not.toBeNull();
    });

    it('também aparece ao tentar enviar sem digitar a senha', () => {
      enviar();
      expect(lista()).not.toBeNull();
    });

    it('os itens mudam de ○ para ✓ conforme a senha atende (texto escondido pendente/atendido)', () => {
      digitarSenha('abc');
      expect(el.querySelectorAll('ul.requisitos li').length).toBe(3);
      expect(marcador('15 caracteres')).toBe('○');
      expect(item('15 caracteres').textContent).toContain('pendente');
      digitarSenha(SENHA);
      expect(marcador('15 caracteres')).toBe('✓');
      expect(item('15 caracteres').textContent).toContain('atendido');
      expect(item('15 caracteres').textContent).not.toContain('pendente');
      expect(marcador('Sem sequências')).toBe('✓');
      expect(marcador('Sem o seu nome')).toBe('✓');
    });

    it('o nome e o e-mail digitados alimentam a lista ao vivo', () => {
      digitar('input[formcontrolname="nome"]', 'Arthur Spironello');
      digitarSenha('o arthur rola dados demais');
      expect(marcador('Sem o seu nome')).toBe('○');
      digitar('input[formcontrolname="nome"]', 'Ana');
      fixture.detectChanges();
      expect(marcador('Sem o seu nome')).toBe('✓');
      digitar('input[type="email"]', 'dados.mestre@x.com');
      digitarSenha('rolo dados.mestre ontem');
      expect(marcador('Sem o seu nome')).toBe('○');
    });

    it('a repetição e a sequência deixam o item "padrão" pendente', () => {
      digitarSenha('abcabcabcabcabc');
      expect(marcador('Sem sequências')).toBe('○');
      digitarSenha('123456789012345');
      expect(marcador('Sem sequências')).toBe('○');
    });

    it('o contador N/15 aparece no item de tamanho até chegar a 15', () => {
      digitarSenha('dragao');
      expect(item('15 caracteres').textContent).toContain('6/15');
      digitarSenha('dragao na taver');
      expect(item('15 caracteres').textContent).not.toContain('/15');
    });

    it('enviar com a senha inválida mostra "Atenda aos requisitos abaixo" e não chama a API', () => {
      preencher('Ana', 'ana@x.com', 'abcabcabcabcabc');
      enviar();
      expect(el.querySelector('mat-form-field:has(input[formcontrolname="senha"]) mat-error')?.textContent).toContain(
        'Atenda aos requisitos abaixo',
      );
      ctl.expectNone(REGISTRO);
    });

    it('o erro 400 do servidor (regra 6) aparece no campo, com prioridade', () => {
      preencher();
      enviar();
      ctl.expectOne(REGISTRO).flush(
        { title: 'Dados inválidos', errors: { senha: 'Essa senha é comum demais. Escolha outra' } },
        { status: 400, statusText: 'x' },
      );
      fixture.detectChanges();
      const erro = el.querySelector('mat-form-field:has(input[formcontrolname="senha"]) mat-error')?.textContent;
      expect(erro).toContain('Essa senha é comum demais. Escolha outra');
    });

    it('mostra as dicas dos campos e o novo subtítulo', () => {
      const texto = el.textContent ?? '';
      expect(texto).toContain('Sua conta guarda as suas fichas. Leva menos de um minuto.');
      expect(texto).toContain('Como a mesa vai te ver');
      expect(texto).toContain('Só para entrar. Não mandamos nada.');
      expect(texto).toContain('Uma frase funciona bem: três ou quatro palavras soltas. Espaços valem.');
    });
  });

  describe('aviso de Caps Lock', () => {
    const campo = () => el.querySelector<HTMLInputElement>('input[formcontrolname="senha"]')!;
    const tecla = (tipo: string, capsLigado: boolean) => {
      campo().dispatchEvent(new KeyboardEvent(tipo, { key: 'a', modifierCapsLock: capsLigado }));
      fixture.detectChanges();
    };
    const aviso = () => el.querySelector('[role="status"]')?.textContent?.trim() ?? null;

    it('aparece com o Caps Lock ligado e some ao desligar', () => {
      expect(aviso()).toBeNull();
      tecla('keydown', true);
      expect(aviso()).toBe('Caps Lock está ligado');
      tecla('keyup', false);
      expect(aviso()).toBeNull();
    });

    it('some ao sair do campo', () => {
      tecla('keydown', true);
      campo().dispatchEvent(new Event('blur'));
      fixture.detectChanges();
      expect(aviso()).toBeNull();
    });
  });

  it('mostra o botão do Google com o volta', () => {
    fixture.componentRef.setInput('volta', '/convite/Abc123');
    fixture.detectChanges();
    const botao = fixture.debugElement.query(By.directive(BotaoGoogle));
    expect(botao).not.toBeNull();
    expect((botao.componentInstance as BotaoGoogle).volta()).toBe('/convite/Abc123');
  });
});
