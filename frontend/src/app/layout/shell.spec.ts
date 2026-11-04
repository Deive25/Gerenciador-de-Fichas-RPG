import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
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

describe('Shell', () => {
  it('mostra a navegacao inferior no celular', () => {
    const el = criarShell(false);
    const nav = el.querySelector('nav.navegacao-inferior');
    expect(nav).not.toBeNull();
    expect(nav?.textContent).toContain('Fichas');
    expect(nav?.textContent).toContain('Nova ficha');
    expect(el.querySelector('.coluna-lateral')).toBeNull();
  });

  it('mostra o layout em duas colunas no computador', () => {
    const el = criarShell(true);
    expect(el.querySelector('.coluna-lateral')).not.toBeNull();
    expect(el.querySelector('nav.navegacao-inferior')).toBeNull();
  });
});
