import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { FichaResumo, RecursoResumo } from '../ficha.model';
import { CartaoFicha } from './cartao-ficha';

const BASE: FichaResumo = {
  id: 7, sistema: 'ORDEM_PARANORMAL', jogador: 'Arthur', personagem: 'Jack Moreau', idade: 34, retratoVersao: null,
  recursos: [
    { codigo: 'PV', atual: 9, maximo: 12 },
    { codigo: 'SAN', atual: 48, maximo: 60 },
    { codigo: 'PE', atual: 5, maximo: 8 },
  ],
};

beforeEach(() => {
  TestBed.configureTestingModule({ imports: [CartaoFicha], providers: [provideRouter([])] });
});

function criar(parcial: Partial<FichaResumo> = {}) {
  const fixture = TestBed.createComponent(CartaoFicha);
  fixture.componentRef.setInput('ficha', { ...BASE, ...parcial });
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

function com(codigo: string, atual: number, maximo: number): RecursoResumo[] {
  return BASE.recursos.map((r) => (r.codigo === codigo ? { codigo, atual, maximo } : r));
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
    expect(criar({ recursos: com('PV', 6, 12) }).querySelector('.etiqueta')?.textContent).toContain('Ferido');
    expect(criar({ recursos: com('PV', 7, 12) }).querySelector('.etiqueta')).toBeNull();
  });

  it('mostra a etiqueta Sanidade baixa', () => {
    expect(criar({ recursos: com('SAN', 30, 60) }).textContent).toContain('Sanidade baixa');
  });

  it('mostra o selo do sistema', () => {
    expect(criar().querySelector('.selo-sistema')?.textContent?.trim()).toBe('Ordem Paranormal');
  });

  it('mostra uma barra por recurso do resumo, com os rotulos Vida, Sanidade e Esforco', () => {
    const barras = Array.from(criar().querySelectorAll('app-barra-status'));
    expect(barras.length).toBe(3);
    expect(barras.map((b) => b.querySelector('.linha span')?.textContent?.trim())).toEqual([
      'Vida',
      'Sanidade',
      'Esforço',
    ]);
  });

  it('um recurso sem maximo mostra o traco', () => {
    const el = criar({ recursos: [{ codigo: 'PE', atual: null, maximo: null }] });
    expect(el.querySelectorAll('app-barra-status').length).toBe(1);
    expect(el.querySelector('app-barra-status')?.textContent).toContain('—');
  });

  it('sem SAN nao mostra a etiqueta Sanidade baixa', () => {
    const el = criar({ recursos: [{ codigo: 'PV', atual: 12, maximo: 12 }] });
    expect(el.textContent).not.toContain('Sanidade baixa');
  });
});
