import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { App } from './app';
import { FichaApi } from './fichas/ficha-api';

function montar() {
  const listar = vi.fn(() => of([]));
  TestBed.configureTestingModule({
    imports: [App],
    providers: [provideRouter([]), { provide: FichaApi, useValue: { listar, versaoFichas: signal(0) } }],
  });
  const fixture = TestBed.createComponent(App);
  fixture.detectChanges();
  return { el: fixture.nativeElement as HTMLElement, listar, fixture };
}

describe('App', () => {
  it('deve criar o componente raiz', () => {
    expect(montar().fixture.componentInstance).toBeTruthy();
  });

  // Reescrito no 2.1b: a coluna lateral com a lista saiu de propósito (spec do redesign §3.1).
  // Quem lista as fichas agora é só o painel da rota /fichas.
  it('nao cria lista propria nem chama a API', () => {
    const { el, listar } = montar();
    expect(el.querySelector('app-shell')).not.toBeNull();
    expect(el.querySelector('app-ficha-lista, app-painel')).toBeNull();
    expect(listar).not.toHaveBeenCalled();
  });
});
