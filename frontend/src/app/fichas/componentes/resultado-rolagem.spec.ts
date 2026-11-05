import { TestBed } from '@angular/core/testing';
import { TesteResultado } from '../ficha.model';
import { ResultadoRolagem } from './resultado-rolagem';

function montar(resultado: TesteResultado, natural?: boolean) {
  const fixture = TestBed.createComponent(ResultadoRolagem);
  fixture.componentRef.setInput('resultado', resultado);
  if (natural !== undefined) {
    fixture.componentRef.setInput('natural', natural);
  }
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('ResultadoRolagem', () => {
  it('mostra nome, mantido, bonus e total, e cada dado rolado', () => {
    const el = montar({
      alvo: 'PERICIA', codigo: 'INVESTIGACAO', nome: 'Investigação', dados: [4, 19, 9], mantido: 19, bonus: 5, total: 24,
    });
    expect(el.textContent).toContain('Investigação: 19 + 5 = 24');
    const dados = Array.from(el.querySelectorAll('.dado'));
    expect(dados.map((d) => d.textContent?.trim())).toEqual(['4', '19', '9']);
    expect(el.querySelectorAll('.mantido').length).toBe(1);
    expect(el.querySelector('.mantido')?.textContent?.trim()).toBe('19');
  });

  it('sem bonus (atributo) nao escreve o + 0', () => {
    const el = montar({ alvo: 'ATRIBUTO', codigo: 'FOR', nome: 'Força', dados: [12, 3], mantido: 12, bonus: 0, total: 12 });
    expect(el.textContent).toContain('Força: 12 = 12');
    expect(el.textContent).not.toContain('+ 0');
  });

  it('com atributo 0 rola 2 dados e o mantido e o menor', () => {
    const el = montar({
      alvo: 'PERICIA', codigo: 'LUTA', nome: 'Luta', dados: [18, 6], mantido: 6, bonus: 15, total: 21,
    });
    expect(el.textContent).toContain('Luta: 6 + 15 = 21');
    expect(el.querySelectorAll('.dado').length).toBe(2);
    expect(el.querySelector('.mantido')?.textContent?.trim()).toBe('6');
  });

  it('dados repetidos marcam so um como mantido', () => {
    const el = montar({ alvo: 'ATRIBUTO', codigo: 'AGI', nome: 'Agilidade', dados: [17, 17], mantido: 17, bonus: 0, total: 17 });
    expect(el.querySelectorAll('.mantido').length).toBe(1);
  });

  it('e anunciado aos leitores de tela', () => {
    const el = montar({ alvo: 'ATRIBUTO', codigo: 'AGI', nome: 'Agilidade', dados: [17], mantido: 17, bonus: 0, total: 17 });
    expect(el.querySelector('[role="status"]')).not.toBeNull();
  });

  it('bonus negativo usa o sinal de menos (U+2212): Força: 7 − 1 = 6', () => {
    const el = montar({ alvo: 'ATRIBUTO', codigo: 'FOR', nome: 'Força', dados: [7], mantido: 7, bonus: -1, total: 6 });
    expect(el.textContent).toContain('Força: 7 − 1 = 6');
    expect(el.textContent).not.toContain('+ -');
  });

  it('mostra 20 natural quando natural esta ligado e o mantido e 20', () => {
    const el = montar({ alvo: 'PERICIA', codigo: 'FURTIVIDADE', nome: 'Furtividade', dados: [20, 3], mantido: 20, bonus: 8, total: 28 }, true);
    expect(el.textContent).toContain('20 natural');
  });

  it('mostra 1 natural quando natural esta ligado e o mantido e 1', () => {
    const el = montar({ alvo: 'PERICIA', codigo: 'FURTIVIDADE', nome: 'Furtividade', dados: [1], mantido: 1, bonus: 8, total: 9 }, true);
    expect(el.textContent).toContain('1 natural');
    expect(el.textContent).not.toContain('20 natural');
  });

  it('nao mostra nada de natural com natural falso (padrao) ou com mantido comum', () => {
    const vinte: TesteResultado = { alvo: 'PERICIA', codigo: 'LUTA', nome: 'Luta', dados: [20], mantido: 20, bonus: 5, total: 25 };
    expect(montar(vinte).textContent).not.toContain('natural');
    TestBed.resetTestingModule();
    expect(montar(vinte, false).textContent).not.toContain('natural');
    TestBed.resetTestingModule();
    expect(montar({ ...vinte, dados: [12], mantido: 12, total: 17 }, true).textContent).not.toContain('natural');
  });
});
