import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DOCUMENT } from '@angular/common';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { Sessao } from '../core/auth/sessao';
import { respostaDe } from '../core/auth/teste-auth';
import { Shell } from './shell';

function criarShell(desktop: boolean) {
  const estado: BreakpointState = { matches: desktop, breakpoints: {} };
  TestBed.configureTestingModule({
    imports: [Shell],
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: BreakpointObserver, useValue: { observe: () => of(estado) } },
    ],
  });
  TestBed.inject(Sessao).iniciar(respostaDe());
  const fixture = TestBed.createComponent(Shell);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

@Component({ template: 'tela aberta' })
class TelaContada {
  static criadas = 0;
  constructor() {
    TelaContada.criadas++;
  }
}

describe('Shell', () => {
  it('mostra a marca e o Nova ficha no topo', () => {
    const el = criarShell(false);
    const marca = el.querySelector('header a.marca') as HTMLAnchorElement;
    expect(marca.getAttribute('href')).toBe('/fichas');
    expect(marca.textContent).toContain('✦ Grimório');
    expect(marca.getAttribute('aria-label')).toBe('Grimório — Fichas RPG, ir para o painel');
    const nova = el.querySelector('header a[href="/fichas/nova"]');
    expect(nova?.textContent).toContain('+ Nova ficha');
  });

  it('no computador e o mesmo topo, com um unico router-outlet e sem coluna lateral nem navegacao inferior', () => {
    const el = criarShell(true);
    expect(el.querySelector('header a.marca')).not.toBeNull();
    expect(el.querySelectorAll('router-outlet').length).toBe(1);
    expect(el.querySelector('.coluna-lateral')).toBeNull();
    expect(el.querySelector('nav.navegacao-inferior')).toBeNull();
  });

  it('nao recria a tela aberta ao cruzar 900 px (girar o aparelho nao perde o que foi digitado)', async () => {
    TelaContada.criadas = 0;
    const largura = new BehaviorSubject<BreakpointState>({ matches: false, breakpoints: {} });
    TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([{ path: '**', component: TelaContada }]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: BreakpointObserver, useValue: { observe: () => largura } },
      ],
    });
    TestBed.inject(Sessao).iniciar(respostaDe());
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    await TestBed.inject(Router).navigateByUrl('/fichas/nova');
    fixture.detectChanges();
    expect(TelaContada.criadas).toBe(1);

    largura.next({ matches: true, breakpoints: {} });
    fixture.detectChanges();
    largura.next({ matches: false, breakpoints: {} });
    fixture.detectChanges();

    expect(TelaContada.criadas).toBe(1);
  });

  describe('sessão no topo', () => {
    const LOGOUT = 'http://localhost:8080/auth/logout';

    /** Um DOCUMENT igual ao real, mas com `location` falso, para o teste não recarregar o jsdom. */
    function montar() {
      const assign = vi.fn();
      const documentoFalso = new Proxy(document, {
        get(alvo, chave) {
          if (chave === 'location') {
            return { assign };
          }
          const valor = Reflect.get(alvo, chave, alvo);
          return typeof valor === 'function' ? valor.bind(alvo) : valor;
        },
      });
      TestBed.configureTestingModule({
        imports: [Shell],
        providers: [
          provideRouter([]),
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: DOCUMENT, useValue: documentoFalso },
          { provide: BreakpointObserver, useValue: { observe: () => of({ matches: false, breakpoints: {} }) } },
        ],
      });
      localStorage.clear();
      const fixture = TestBed.createComponent(Shell);
      fixture.detectChanges();
      return { fixture, el: fixture.nativeElement as HTMLElement, assign, ctl: TestBed.inject(HttpTestingController) };
    }

    it('deslogado mostra só a marca', () => {
      const { el } = montar();
      expect(el.querySelector('header a.marca')).not.toBeNull();
      expect(el.querySelector('header a[href="/fichas/nova"]')).toBeNull();
      expect(el.querySelector('header button')).toBeNull();
      expect(el.querySelector('.nome-usuario')).toBeNull();
    });

    it('muda quando a pessoa entra: aparecem Nova ficha, o nome e Sair', () => {
      const { fixture, el } = montar();
      TestBed.inject(Sessao).iniciar(respostaDe());
      fixture.detectChanges();
      expect(el.querySelector('header a[href="/fichas/nova"]')?.textContent).toContain('+ Nova ficha');
      expect(el.querySelector('.nome-usuario')?.textContent).toContain('Ana');
      expect(el.querySelector('header button')?.textContent).toContain('Sair');
    });

    it('logado mostra os links Fichas e Campanhas; deslogado, nenhum', () => {
      const { fixture, el } = montar();
      expect(el.querySelector('header nav')).toBeNull();
      TestBed.inject(Sessao).iniciar(respostaDe());
      fixture.detectChanges();
      const links = Array.from(el.querySelectorAll('header nav a'));
      expect(links.map((a) => [a.textContent?.trim(), a.getAttribute('href')])).toEqual([
        ['Fichas', '/fichas'],
        ['Campanhas', '/campanhas'],
      ]);
      expect(el.querySelector('header nav')?.getAttribute('aria-label')).toBe('Principal');
    });

    it('Sair avisa a API, apaga a sessão e faz a navegação completa para /entrar', () => {
      const { fixture, el, assign, ctl } = montar();
      TestBed.inject(Sessao).iniciar(respostaDe());
      fixture.detectChanges();
      el.querySelector<HTMLButtonElement>('header button')!.click();
      fixture.detectChanges();
      expect(el.querySelector<HTMLButtonElement>('header button')!.disabled).toBe(true);
      const req = ctl.expectOne(LOGOUT);
      expect(req.request.body).toEqual({ refreshToken: 'refresh-1' });
      expect(assign).not.toHaveBeenCalled();
      req.flush(null, { status: 204, statusText: 'No Content' });
      expect(TestBed.inject(Sessao).estaLogado()).toBe(false);
      expect(localStorage.getItem('fichas-rpg.refresh')).toBeNull();
      expect(assign).toHaveBeenCalledWith('/entrar');
    });

    it('Sair ignora o erro da API e sai do mesmo jeito', () => {
      const { fixture, el, assign, ctl } = montar();
      TestBed.inject(Sessao).iniciar(respostaDe());
      fixture.detectChanges();
      el.querySelector<HTMLButtonElement>('header button')!.click();
      ctl.expectOne(LOGOUT).error(new ProgressEvent('error'));
      expect(TestBed.inject(Sessao).estaLogado()).toBe(false);
      expect(assign).toHaveBeenCalledWith('/entrar');
    });
  });
});
