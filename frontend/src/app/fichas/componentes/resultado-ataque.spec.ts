import { TestBed } from '@angular/core/testing';
import { AtaqueResultado } from '../ficha.model';
import { ResultadoAtaque } from './resultado-ataque';

const ESPADA: AtaqueResultado = {
  ataqueId: 3, nome: 'Espada longa',
  ataque: { dados: [14, 19], mantido: 19, bonus: 5, total: 24 },
  dano: { expressao: '1d8+3', dados: [{ lados: 8, valor: 6 }], bonus: 3, total: 9 },
  critico: false,
};

function montar(resultado: AtaqueResultado, natural?: boolean) {
  const fixture = TestBed.createComponent(ResultadoAtaque);
  fixture.componentRef.setInput('resultado', resultado);
  if (natural !== undefined) {
    fixture.componentRef.setInput('natural', natural);
  }
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

const texto = (el: Element | null) => el?.textContent?.replace(/\s+/g, ' ').trim();

describe('ResultadoAtaque', () => {
  it('mostra o ataque como "Espada longa: 19 + 5 = 24" e o dano como "Dano: 6 + 3 = 9"', () => {
    const el = montar(ESPADA);
    expect(texto(el.querySelector('.titulo-ataque'))).toBe('Espada longa: 19 + 5 = 24');
    expect(texto(el.querySelector('.titulo-dano'))).toBe('Dano: 6 + 3 = 9');
  });

  it('mostra os d20 do ataque com so o mantido destacado', () => {
    const el = montar(ESPADA);
    const d20 = Array.from(el.querySelectorAll('.dados-ataque .dado'));
    expect(d20.map((d) => texto(d))).toEqual(['14', '19']);
    expect(el.querySelectorAll('.dados-ataque .mantido').length).toBe(1);
    expect(texto(el.querySelector('.dados-ataque .mantido'))).toBe('19');
  });

  it('mostra cada dado do dano com o tipo: d8: 6', () => {
    const el = montar({
      ...ESPADA,
      dano: { expressao: '2d8+3', dados: [{ lados: 8, valor: 6 }, { lados: 8, valor: 2 }], bonus: 3, total: 11 },
    });
    expect(Array.from(el.querySelectorAll('.dados-dano .dado')).map((d) => texto(d))).toEqual(['d8: 6', 'd8: 2']);
    expect(texto(el.querySelector('.titulo-dano'))).toBe('Dano: 6 + 2 + 3 = 11');
  });

  it('sem bonus no dano nao escreve o + 0, e bonus negativo usa o sinal de menos', () => {
    const semBonus = montar({ ...ESPADA, dano: { expressao: '1d8', dados: [{ lados: 8, valor: 6 }], bonus: 0, total: 6 } });
    expect(texto(semBonus.querySelector('.titulo-dano'))).toBe('Dano: 6 = 6');
    TestBed.resetTestingModule();
    const negativo = montar({ ...ESPADA, dano: { expressao: '1d10-1', dados: [{ lados: 10, valor: 4 }], bonus: -1, total: 3 } });
    expect(texto(negativo.querySelector('.titulo-dano'))).toBe('Dano: 4 − 1 = 3');
  });

  it('dano so com constante (sem dados) mostra o total', () => {
    const el = montar({ ...ESPADA, dano: { expressao: '5', dados: [], bonus: 5, total: 5 } });
    expect(texto(el.querySelector('.titulo-dano'))).toBe('Dano: 5');
    expect(el.querySelectorAll('.dados-dano .dado').length).toBe(0);
  });

  it('o ataque sem bonus nao escreve o + 0', () => {
    const el = montar({ ...ESPADA, ataque: { dados: [12], mantido: 12, bonus: 0, total: 12 } });
    expect(texto(el.querySelector('.titulo-ataque'))).toBe('Espada longa: 12 = 12');
  });

  it('o selo Critico so aparece quando o ataque foi critico', () => {
    expect(montar(ESPADA).querySelector('.critico')).toBeNull();
    TestBed.resetTestingModule();
    expect(texto(montar({ ...ESPADA, critico: true }).querySelector('.critico'))).toBe('Crítico');
  });

  it('20 natural e 1 natural so aparecem em D&D (natural ligado) e quando o mantido e 20 ou 1', () => {
    const vinte: AtaqueResultado = { ...ESPADA, ataque: { dados: [20, 3], mantido: 20, bonus: 5, total: 25 } };
    expect(montar(vinte, true).textContent).toContain('20 natural');
    TestBed.resetTestingModule();
    expect(montar(vinte).textContent).not.toContain('natural');
    TestBed.resetTestingModule();
    const um: AtaqueResultado = { ...ESPADA, ataque: { dados: [1], mantido: 1, bonus: 5, total: 6 } };
    expect(montar(um, true).textContent).toContain('1 natural');
    TestBed.resetTestingModule();
    expect(montar(ESPADA, true).textContent).not.toContain('natural');
  });

  it('e anunciado aos leitores de tela (role status) e tem uma leitura so para eles', () => {
    const el = montar({ ...ESPADA, critico: true });
    expect(el.querySelector('[role="status"]')).not.toBeNull();
    const leitura = texto(el.querySelector('.so-leitor'))!;
    expect(leitura).toContain('Crítico');
    expect(leitura).toContain('14, 19');
    expect(leitura).toContain('d8 6');
  });
});
