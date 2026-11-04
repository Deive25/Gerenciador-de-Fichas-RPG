import { BreakpointObserver } from '@angular/cdk/layout';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { App } from './app';
import { FichaApi } from './fichas/ficha-api';

function montar(desktop: boolean) {
  const listar = vi.fn(() => of([]));
  TestBed.configureTestingModule({
    imports: [App],
    providers: [
      provideRouter([]),
      { provide: BreakpointObserver, useValue: { observe: () => of({ matches: desktop, breakpoints: {} }) } },
      { provide: FichaApi, useValue: { listar, versaoFichas: signal(0) } },
    ],
  });
  const fixture = TestBed.createComponent(App);
  fixture.detectChanges();
  return { el: fixture.nativeElement as HTMLElement, listar, fixture };
}

describe('App', () => {
  it('deve criar o componente raiz', () => {
    expect(montar(false).fixture.componentInstance).toBeTruthy();
  });

  it('no celular nao cria a lista lateral nem chama a API por ela', () => {
    const { el, listar } = montar(false);
    expect(el.querySelector('app-ficha-lista')).toBeNull();
    expect(listar).not.toHaveBeenCalled();
  });

  it('no computador mostra a lista na coluna lateral', () => {
    const { el, listar } = montar(true);
    expect(el.querySelector('.coluna-lateral app-ficha-lista')).not.toBeNull();
    expect(listar).toHaveBeenCalledTimes(1);
  });
});
