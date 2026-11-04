import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Ficha } from '../ficha.model';
import { CartaoFicha } from './cartao-ficha';

const BASE: Ficha = {
  id: 7, jogador: 'Arthur', personagem: 'Jack Moreau', genero: 'M', idade: 34,
  vidaAtual: 9, vidaMaxima: 12, sanidadeAtual: 48, sanidadeMaxima: 60,
  forca: null, destreza: null, constituicao: null, inteligencia: null, carisma: null,
  sabedoria: null, tamanho: null, sorte: null, poder: null, pericias: {},
};

beforeEach(() => {
  TestBed.configureTestingModule({ imports: [CartaoFicha], providers: [provideRouter([])] });
});

function criar(parcial: Partial<Ficha> = {}) {
  const fixture = TestBed.createComponent(CartaoFicha);
  fixture.componentRef.setInput('ficha', { ...BASE, ...parcial });
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('CartaoFicha', () => {
  it('o cartao inteiro e um link para a ficha', () => {
    expect(criar().querySelector('a.cartao-ficha')?.getAttribute('href')).toBe('/fichas/7');
  });

  it('mostra o jogador e a idade', () => {
    expect(criar().textContent).toContain('Arthur · 34 anos');
    expect(criar({ idade: null }).textContent).not.toContain('anos');
  });

  it('mostra a etiqueta Ferido so com a vida ate a metade', () => {
    expect(criar({ vidaAtual: 6 }).querySelector('.etiqueta')?.textContent).toContain('Ferido');
    expect(criar({ vidaAtual: 7 }).querySelector('.etiqueta')).toBeNull();
  });

  it('mostra a etiqueta Sanidade baixa', () => {
    expect(criar({ sanidadeAtual: 30 }).textContent).toContain('Sanidade baixa');
  });
});
