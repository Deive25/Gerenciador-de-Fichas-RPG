import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { Shell } from './shell';

function criarShell(desktop: boolean) {
  const estado: BreakpointState = { matches: desktop, breakpoints: {} };
  TestBed.configureTestingModule({
    imports: [Shell],
    providers: [
      provideRouter([]),
      { provide: BreakpointObserver, useValue: { observe: () => of(estado) } },
    ],
  });
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
        { provide: BreakpointObserver, useValue: { observe: () => largura } },
      ],
    });
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
});
