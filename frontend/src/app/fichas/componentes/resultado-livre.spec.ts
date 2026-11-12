import { TestBed } from '@angular/core/testing';
import { RolagemLivre } from '../ficha.model';
import { ResultadoLivre } from './resultado-livre';

const LIVRE: RolagemLivre = {
  expressao: '2d6+1d4+3', bonus: 3, total: 11,
  dados: [{ lados: 6, valor: 4 }, { lados: 6, valor: 1 }, { lados: 4, valor: 3 }],
};

function montar(resultado: RolagemLivre) {
  const fixture = TestBed.createComponent(ResultadoLivre);
  fixture.componentRef.setInput('resultado', resultado);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

const texto = (el: Element | null) => el?.textContent?.replace(/\s+/g, ' ').trim();

describe('ResultadoLivre', () => {
  it('mostra "2d6+1d4+3 = 11"', () => {
    expect(texto(montar(LIVRE).querySelector('.titulo'))).toBe('2d6+1d4+3 = 11');
  });

  it('mostra cada dado com o tipo (d6: 4) e o modificador', () => {
    const el = montar(LIVRE);
    expect(Array.from(el.querySelectorAll('.dado:not(.modificador)')).map((d) => texto(d))).toEqual(['d6: 4', 'd6: 1', 'd4: 3']);
    expect(texto(el.querySelector('.modificador'))).toBe('+3');
  });

  it('sem modificador nao mostra o chip do modificador', () => {
    const el = montar({ expressao: '1d20', bonus: 0, total: 14, dados: [{ lados: 20, valor: 14 }] });
    expect(el.querySelector('.modificador')).toBeNull();
    expect(texto(el.querySelector('.titulo'))).toBe('1d20 = 14');
  });

  it('modificador negativo usa o sinal de menos (U+2212)', () => {
    const el = montar({ expressao: '1d10-1', bonus: -1, total: 3, dados: [{ lados: 10, valor: 4 }] });
    expect(texto(el.querySelector('.modificador'))).toBe('−1');
  });

  it('e anunciado aos leitores de tela e tem uma leitura so para eles', () => {
    const el = montar(LIVRE);
    expect(el.querySelector('[role="status"]')).not.toBeNull();
    const leitura = texto(el.querySelector('.so-leitor'))!;
    expect(leitura).toContain('d6 4');
    expect(leitura).toContain('d4 3');
    expect(leitura).toContain('Modificador +3');
  });
});
