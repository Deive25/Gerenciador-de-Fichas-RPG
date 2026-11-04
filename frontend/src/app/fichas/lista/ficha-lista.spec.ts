import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';
import { ApiError } from '../../core/api-error';
import { FichaApi } from '../ficha-api';
import { Ficha } from '../ficha.model';
import { FichaLista } from './ficha-lista';

function ficha(id: number, personagem: string): Ficha {
  return {
    id, jogador: 'Arthur', personagem, genero: 'F', idade: null,
    vidaAtual: 9, vidaMaxima: 12, sanidadeAtual: 48, sanidadeMaxima: 60,
    forca: null, destreza: null, constituicao: null, inteligencia: null, carisma: null,
    sabedoria: null, tamanho: null, sorte: null, poder: null, pericias: {},
  };
}

const INDISPONIVEL: ApiError = {
  status: 0, titulo: 'Servidor indisponível', detalhe: 'Ele pode estar acordando.', campos: {},
};

function montar(listar: ReturnType<typeof vi.fn>, versao = signal(0)) {
  TestBed.configureTestingModule({
    imports: [FichaLista],
    providers: [provideRouter([]), { provide: FichaApi, useValue: { listar, versaoFichas: versao } }],
  });
  const fixture = TestBed.createComponent(FichaLista);
  fixture.detectChanges();
  return { fixture, el: fixture.nativeElement as HTMLElement };
}

describe('FichaLista', () => {
  it('mostra um card por ficha', () => {
    const { el } = montar(vi.fn(() => of([ficha(1, 'Lia'), ficha(2, 'Hale')])));
    const cards = el.querySelectorAll('a.card-ficha');
    expect(cards.length).toBe(2);
    expect(cards[0].getAttribute('href')).toBe('/fichas/1');
    expect(cards[1].getAttribute('href')).toBe('/fichas/2');
    expect(el.textContent).toContain('Lia');
  });

  it('mostra o estado vazio', () => {
    const { el } = montar(vi.fn(() => of([])));
    expect(el.textContent).toContain('Nenhuma ficha ainda.');
    expect(el.querySelector('a[href="/fichas/nova"]')).not.toBeNull();
  });

  it('mostra erro e permite tentar de novo', () => {
    const listar = vi
      .fn()
      .mockReturnValueOnce(throwError(() => INDISPONIVEL))
      .mockReturnValueOnce(of([ficha(1, 'Lia')]));
    const { fixture, el } = montar(listar);
    expect(el.textContent).toContain('Servidor indisponível');

    (Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes('Tentar de novo')) as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(listar).toHaveBeenCalledTimes(2);
    expect(el.querySelectorAll('a.card-ficha').length).toBe(1);
  });

  it('recarrega quando uma ficha e alterada', () => {
    const versao = signal(0);
    const listar = vi.fn(() => of([ficha(1, 'Lia')]));
    const { fixture } = montar(listar, versao);
    expect(listar).toHaveBeenCalledTimes(1);

    versao.set(1);
    fixture.detectChanges();

    expect(listar).toHaveBeenCalledTimes(2);
  });
});
