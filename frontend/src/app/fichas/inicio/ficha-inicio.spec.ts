import { BreakpointObserver } from '@angular/cdk/layout';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { FichaApi } from '../ficha-api';
import { FichaInicio } from './ficha-inicio';

function montar(desktop: boolean) {
  TestBed.configureTestingModule({
    imports: [FichaInicio],
    providers: [
      provideRouter([]),
      { provide: BreakpointObserver, useValue: { observe: () => of({ matches: desktop, breakpoints: {} }) } },
      { provide: FichaApi, useValue: { listar: () => of([]), versaoFichas: signal(0) } },
    ],
  });
  const fixture = TestBed.createComponent(FichaInicio);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('FichaInicio', () => {
  it('no celular mostra a lista', () => {
    const el = montar(false);
    expect(el.querySelector('app-ficha-lista')).not.toBeNull();
  });

  it('no computador nao duplica a lista', () => {
    const el = montar(true);
    expect(el.querySelector('app-ficha-lista')).toBeNull();
    expect(el.textContent).toContain('Selecione uma ficha à esquerda ou crie uma nova.');
  });
});
