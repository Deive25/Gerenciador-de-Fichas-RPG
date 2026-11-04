import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NEVER, of, throwError } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { FichaApi } from '../ficha-api';
import { Ficha } from '../ficha.model';
import { Painel } from './painel';

function ficha(id: number, personagem: string, parcial: Partial<Ficha> = {}): Ficha {
  return {
    id, jogador: 'Arthur', personagem, genero: 'F', idade: null,
    vidaAtual: 9, vidaMaxima: 12, sanidadeAtual: 48, sanidadeMaxima: 60,
    forca: null, destreza: null, constituicao: null, inteligencia: null, carisma: null,
    sabedoria: null, tamanho: null, sorte: null, poder: null, pericias: {}, retratoVersao: null,
    ...parcial,
  };
}

const INDISPONIVEL: ApiError = {
  status: 0, titulo: 'Servidor indisponível', detalhe: 'Ele pode estar acordando.', campos: {},
};

const MESA = [
  ficha(1, 'Lía Fontaine', { jogador: 'Bia', sanidadeAtual: 21, sanidadeMaxima: 55 }),
  ficha(2, 'Prof. Hale', { jogador: 'Caio', vidaAtual: 4, vidaMaxima: 10 }),
  ficha(3, 'Jack Moreau'),
];

function montar(listar: ReturnType<typeof vi.fn>, versao = signal(0)) {
  TestBed.configureTestingModule({
    imports: [Painel],
    providers: [provideRouter([]), { provide: FichaApi, useValue: { listar, versaoFichas: versao } }],
  });
  const fixture = TestBed.createComponent(Painel);
  fixture.detectChanges();
  return { fixture, el: fixture.nativeElement as HTMLElement };
}

function botao(el: HTMLElement, texto: string): HTMLButtonElement {
  return Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes(texto)) as HTMLButtonElement;
}

function cartoes(el: HTMLElement): HTMLAnchorElement[] {
  return Array.from(el.querySelectorAll('a.cartao-ficha'));
}

describe('Painel', () => {
  // Migrados de FichaLista (mapa de testes do plano 2.1b).
  it('mostra um cartão por ficha', () => {
    const { el } = montar(vi.fn(() => of([ficha(1, 'Lia'), ficha(2, 'Hale')])));
    const links = cartoes(el);
    expect(links.length).toBe(2);
    expect(links[0].getAttribute('href')).toBe('/fichas/1');
    expect(links[1].getAttribute('href')).toBe('/fichas/2');
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

    botao(el, 'Tentar de novo').click();
    fixture.detectChanges();

    expect(listar).toHaveBeenCalledTimes(2);
    expect(cartoes(el).length).toBe(1);
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

  // Novos.
  it('mostra os contadores', () => {
    const { el } = montar(vi.fn(() => of(MESA)));
    const textos = Array.from(el.querySelectorAll('.contador')).map(
      (c) => `${c.querySelector('.rotulo')?.textContent?.trim()} ${c.querySelector('strong')?.textContent?.trim()}`,
    );
    expect(textos).toEqual(['Investigadores 3', 'Feridos 1', 'Sanidade baixa 1']);
  });

  it('filtra por feridos', () => {
    const { fixture, el } = montar(vi.fn(() => of(MESA)));
    const feridos = botao(el.querySelector('.filtros') as HTMLElement, 'Feridos');

    feridos.click();
    fixture.detectChanges();

    expect(cartoes(el).map((a) => a.getAttribute('href'))).toEqual(['/fichas/2']);
    expect(feridos.getAttribute('aria-pressed')).toBe('true');
    expect(botao(el.querySelector('.filtros') as HTMLElement, 'Todos').getAttribute('aria-pressed')).toBe('false');
  });

  it('busca sem acento', () => {
    const { fixture, el } = montar(vi.fn(() => of(MESA)));
    const busca = el.querySelector('input[type="search"][aria-label="Buscar investigador"]') as HTMLInputElement;

    busca.value = 'lia';
    busca.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(cartoes(el).map((a) => a.getAttribute('href'))).toEqual(['/fichas/1']);
  });

  it('sem resultado mostra limpar filtros, e nao o convite de criar a primeira ficha', () => {
    const { fixture, el } = montar(vi.fn(() => of(MESA)));
    const busca = el.querySelector('input[type="search"]') as HTMLInputElement;
    busca.value = 'zzz';
    busca.dispatchEvent(new Event('input'));
    botao(el.querySelector('.filtros') as HTMLElement, 'Feridos').click();
    fixture.detectChanges();

    expect(el.textContent).toContain('Nenhuma ficha encontrada');
    expect(el.textContent).not.toContain('Nenhuma ficha ainda.');

    botao(el, 'Limpar filtros').click();
    fixture.detectChanges();

    expect(cartoes(el).length).toBe(3);
    expect((el.querySelector('input[type="search"]') as HTMLInputElement).value).toBe('');
    expect(botao(el.querySelector('.filtros') as HTMLElement, 'Todos').getAttribute('aria-pressed')).toBe('true');
  });

  it('enquanto carrega avisa o leitor de tela', () => {
    const { el } = montar(vi.fn(() => NEVER));
    const status = el.querySelector('[role="status"]');
    expect(status?.textContent).toContain('Carregando fichas…');
  });
});
