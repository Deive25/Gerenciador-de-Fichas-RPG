import { TestBed } from '@angular/core/testing';
import { ModoTeste } from '../ficha.model';
import { SeletorDeModo } from './seletor-de-modo';

function montar() {
  const fixture = TestBed.createComponent(SeletorDeModo);
  fixture.detectChanges();
  const el = fixture.nativeElement as HTMLElement;
  const botao = (nome: string) =>
    Array.from(el.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent?.trim() === nome)!;
  return { fixture, el, botao };
}

describe('SeletorDeModo', () => {
  it('tem os 3 modos, com Normal pressionado de inicio', () => {
    const { el } = montar();
    const botoes = Array.from(el.querySelectorAll<HTMLButtonElement>('.modo button'));
    expect(botoes.map((b) => b.textContent?.trim())).toEqual(['Normal', 'Vantagem', 'Desvantagem']);
    expect(botoes.map((b) => b.getAttribute('aria-pressed'))).toEqual(['true', 'false', 'false']);
    expect(botoes.every((b) => b.type === 'button')).toBe(true);
  });

  it('clicar escolhe o modo (two-way) e passa o pressionado', () => {
    const { fixture, botao } = montar();
    botao('Desvantagem').click();
    fixture.detectChanges();
    expect(fixture.componentInstance.modo()).toBe<ModoTeste>('DESVANTAGEM');
    expect(botao('Desvantagem').getAttribute('aria-pressed')).toBe('true');
    expect(botao('Normal').getAttribute('aria-pressed')).toBe('false');
  });

  it('quem usa pode voltar o modo a Normal', () => {
    const { fixture, botao } = montar();
    botao('Vantagem').click();
    fixture.componentInstance.modo.set('NORMAL');
    fixture.detectChanges();
    expect(botao('Normal').getAttribute('aria-pressed')).toBe('true');
  });

  it('o grupo e rotulado pelo titulo, com um id diferente em cada instancia', () => {
    const a = montar();
    const b = montar();
    const idA = a.el.querySelector('[role="group"]')!.getAttribute('aria-labelledby')!;
    const idB = b.el.querySelector('[role="group"]')!.getAttribute('aria-labelledby')!;
    expect(a.el.querySelector(`#${idA}`)?.textContent).toContain('Modo da próxima rolagem');
    expect(idA).not.toBe(idB);
  });

  it('os botoes tem 44 px de altura minima', () => {
    const { el } = montar();
    document.body.appendChild(el);
    expect(getComputedStyle(el.querySelector('button')!).minHeight).toBe('44px');
    el.remove();
  });
});
