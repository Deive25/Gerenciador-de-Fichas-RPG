import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PedidoDeRolagem } from '../ficha.model';
import { DadosAba } from './dados-aba';

function montar(rolando = false) {
  const fixture = TestBed.createComponent(DadosAba);
  fixture.componentRef.setInput('rolando', rolando);
  fixture.detectChanges();
  const pedidos: PedidoDeRolagem[] = [];
  fixture.componentInstance.rolar.subscribe((p) => pedidos.push(p));
  return { fixture, el: fixture.nativeElement as HTMLElement, pedidos };
}

const campo = (el: HTMLElement) => el.querySelector<HTMLInputElement>('input')!;
const botao = (el: HTMLElement, texto: string) =>
  Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.trim() === texto) as HTMLButtonElement;
const dado = (el: HTMLElement, lados: number) => el.querySelector<HTMLElement>(`[data-dado="${lados}"]`)!;
const mais = (el: HTMLElement, lados: number) => dado(el, lados).querySelector<HTMLButtonElement>('button[aria-label^="Mais"]')!;
const menos = (el: HTMLElement, lados: number) => dado(el, lados).querySelector<HTMLButtonElement>('button[aria-label^="Menos"]')!;
const contador = (el: HTMLElement, lados: number) => dado(el, lados).querySelector('.contador')!.textContent!.trim();
const modMais = (el: HTMLElement) => el.querySelector<HTMLButtonElement>('[data-modificador] button[aria-label^="Mais"]')!;
const modMenos = (el: HTMLElement) => el.querySelector<HTMLButtonElement>('[data-modificador] button[aria-label^="Menos"]')!;
const modValor = (el: HTMLElement) => el.querySelector('[data-modificador] .contador')!.textContent!.trim();
const erro = (el: HTMLElement) => el.querySelector('mat-error')?.textContent?.trim() ?? null;

function clicar(fixture: ComponentFixture<unknown>, b: HTMLElement, vezes = 1): void {
  for (let i = 0; i < vezes; i++) {
    b.click();
    fixture.detectChanges();
  }
}

function digitar(fixture: ComponentFixture<unknown>, el: HTMLElement, texto: string): void {
  campo(el).value = texto;
  campo(el).dispatchEvent(new Event('input'));
  fixture.detectChanges();
  fixture.detectChanges(); // o estado de erro do campo (Material) assenta na passada seguinte
}

describe('DadosAba', () => {
  it('comeca vazia: sem texto, todos os contadores em 0, modificador 0 e Rolar desabilitado', () => {
    const { el } = montar();
    expect(campo(el).value).toBe('');
    for (const lados of [4, 6, 8, 10, 12, 20, 100]) {
      expect(contador(el, lados)).toBe('0');
    }
    expect(modValor(el)).toBe('0');
    expect(botao(el, 'Rolar').disabled).toBe(true);
    expect(erro(el)).toBeNull();
  });

  it('tem uma linha por dado (d4 a d100) com - e + de 44 px', () => {
    const { el } = montar();
    document.body.appendChild(el);
    const nomes = Array.from(el.querySelectorAll('[data-dado] .nome')).map((n) => n.textContent?.trim());
    expect(nomes).toEqual(['d4', 'd6', 'd8', 'd10', 'd12', 'd20', 'd100']);
    const passos = Array.from(el.querySelectorAll<HTMLButtonElement>('button.passo'));
    expect(passos).toHaveLength(16); // 7 linhas x 2 + o modificador
    for (const p of passos) {
      expect(getComputedStyle(p).minHeight).toBe('44px');
      expect(getComputedStyle(p).minWidth).toBe('44px');
    }
    el.remove();
  });

  it('os botoes tem nome acessivel por dado', () => {
    const { el } = montar();
    expect(mais(el, 6).getAttribute('aria-label')).toBe('Mais um d6');
    expect(menos(el, 100).getAttribute('aria-label')).toBe('Menos um d100');
    expect(modMais(el).getAttribute('aria-label')).toBe('Mais um no modificador');
  });

  describe('botoes reescrevem o texto na forma canonica', () => {
    it('um dado: d6 + vira 1d6', () => {
      const { fixture, el } = montar();
      clicar(fixture, mais(el, 6));
      expect(campo(el).value).toBe('1d6');
      expect(contador(el, 6)).toBe('1');
    });

    it('dados de menos lados primeiro, depois o modificador: 1d4+2d6+3', () => {
      const { fixture, el } = montar();
      clicar(fixture, mais(el, 6), 2);
      clicar(fixture, mais(el, 4));
      clicar(fixture, modMais(el), 3);
      expect(campo(el).value).toBe('1d4+2d6+3');
      expect(modValor(el)).toBe('+3');
    });

    it('modificador negativo: 1d10-1, mostrado com o sinal de menos', () => {
      const { fixture, el } = montar();
      clicar(fixture, mais(el, 10));
      clicar(fixture, modMenos(el));
      expect(campo(el).value).toBe('1d10-1');
      expect(modValor(el)).toBe('−1');
    });

    it('o - tira um dado e, em 0, fica desabilitado', () => {
      const { fixture, el } = montar();
      expect(menos(el, 8).disabled).toBe(true);
      clicar(fixture, mais(el, 8), 2);
      clicar(fixture, menos(el, 8));
      expect(campo(el).value).toBe('1d8');
      clicar(fixture, menos(el, 8));
      expect(campo(el).value).toBe('');
      expect(menos(el, 8).disabled).toBe(true);
    });

    it('o + de um dado para em 20, e o total de dados para em 20', () => {
      const { fixture, el } = montar();
      clicar(fixture, mais(el, 6), 20);
      expect(contador(el, 6)).toBe('20');
      expect(mais(el, 6).disabled).toBe(true);
      expect(mais(el, 4).disabled).toBe(true); // ja ha 20 dados somados
      clicar(fixture, menos(el, 6), 10);
      expect(mais(el, 4).disabled).toBe(false);
      clicar(fixture, mais(el, 4), 10);
      expect(campo(el).value).toBe('10d4+10d6');
      expect(mais(el, 4).disabled).toBe(true);
    });

    it('o modificador vai de -99 a 99', () => {
      const { fixture, el } = montar();
      clicar(fixture, modMais(el), 99);
      expect(modValor(el)).toBe('+99');
      expect(modMais(el).disabled).toBe(true);
      clicar(fixture, modMenos(el), 198);
      expect(modValor(el)).toBe('−99');
      expect(modMenos(el).disabled).toBe(true);
    });

    it('so modificador positivo vira o proprio numero e dá para rolar', () => {
      const { fixture, el } = montar();
      clicar(fixture, modMais(el), 3);
      expect(campo(el).value).toBe('3');
      expect(botao(el, 'Rolar').disabled).toBe(false);
    });

    it('modificador negativo sem dados deixa o texto vazio e o Rolar desabilitado', () => {
      const { fixture, el } = montar();
      clicar(fixture, modMenos(el), 2);
      expect(campo(el).value).toBe('');
      expect(modValor(el)).toBe('−2');
      expect(botao(el, 'Rolar').disabled).toBe(true);
    });
  });

  describe('texto valido atualiza os contadores', () => {
    it('2d6+1d4+3 acerta d6: 2, d4: 1 e modificador +3', () => {
      const { fixture, el } = montar();
      digitar(fixture, el, '2d6+1d4+3');
      expect(contador(el, 6)).toBe('2');
      expect(contador(el, 4)).toBe('1');
      expect(contador(el, 8)).toBe('0');
      expect(modValor(el)).toBe('+3');
      expect(erro(el)).toBeNull();
      expect(botao(el, 'Rolar').disabled).toBe(false);
    });

    it('1d6+1d6 soma o mesmo dado: d6: 2', () => {
      const { fixture, el } = montar();
      digitar(fixture, el, '1d6+1d6');
      expect(contador(el, 6)).toBe('2');
    });

    it('nao reescreve o que a pessoa digita enquanto ela digita', () => {
      const { fixture, el } = montar();
      digitar(fixture, el, ' 1D6 + 1d6 ');
      expect(campo(el).value).toBe(' 1D6 + 1d6 ');
    });

    it('depois de digitar, um botao reescreve na forma canonica', () => {
      const { fixture, el } = montar();
      digitar(fixture, el, '1d6+1d6');
      clicar(fixture, mais(el, 4));
      expect(campo(el).value).toBe('1d4+2d6');
    });

    it('apagar o texto zera os contadores e o modificador', () => {
      const { fixture, el } = montar();
      digitar(fixture, el, '2d6+3');
      digitar(fixture, el, '');
      expect(contador(el, 6)).toBe('0');
      expect(modValor(el)).toBe('0');
      expect(erro(el)).toBeNull();
      expect(botao(el, 'Rolar').disabled).toBe(true);
    });
  });

  describe('texto invalido', () => {
    it('mostra o erro abaixo do campo, desabilita Rolar e nao mexe nos contadores', () => {
      const { fixture, el } = montar();
      clicar(fixture, mais(el, 6), 2);
      digitar(fixture, el, '2d7');
      expect(erro(el)).toBe('Não existe d7. Use d4, d6, d8, d10, d12, d20 ou d100.');
      expect(botao(el, 'Rolar').disabled).toBe(true);
      expect(contador(el, 6)).toBe('2');
      expect(campo(el).getAttribute('aria-invalid')).toBe('true');
    });

    it('o erro some quando o texto volta a valer', () => {
      const { fixture, el } = montar();
      digitar(fixture, el, 'abc');
      expect(erro(el)).toContain('Não entendi');
      digitar(fixture, el, '1d6');
      expect(erro(el)).toBeNull();
      expect(botao(el, 'Rolar').disabled).toBe(false);
    });

    it('21 dados somados dá o erro do limite', () => {
      const { fixture, el } = montar();
      digitar(fixture, el, '11d6+10d4');
      expect(erro(el)).toBe('Use no máximo 20 dados no total.');
    });

    it('um botao corrige o texto invalido', () => {
      const { fixture, el } = montar();
      digitar(fixture, el, 'abc');
      clicar(fixture, mais(el, 6));
      expect(campo(el).value).toBe('1d6');
      expect(erro(el)).toBeNull();
    });
  });

  describe('Rolar e Limpar', () => {
    it('Rolar emite o pedido livre com a expressao canonica', () => {
      const { fixture, el, pedidos } = montar();
      digitar(fixture, el, '3D6 + 2');
      clicar(fixture, botao(el, 'Rolar'));
      expect(pedidos).toEqual([{ tipo: 'livre', expressao: '3d6+2' }]);
    });

    it('Rolar depois dos botoes emite o mesmo que o texto mostra', () => {
      const { fixture, el, pedidos } = montar();
      clicar(fixture, mais(el, 6), 2);
      clicar(fixture, mais(el, 4));
      clicar(fixture, modMais(el), 3);
      clicar(fixture, botao(el, 'Rolar'));
      expect(pedidos).toEqual([{ tipo: 'livre', expressao: '1d4+2d6+3' }]);
    });

    it('nao emite com texto invalido ou vazio', () => {
      const { fixture, el, pedidos } = montar();
      botao(el, 'Rolar').click();
      digitar(fixture, el, '1d7');
      botao(el, 'Rolar').click();
      expect(pedidos).toEqual([]);
    });

    it('com rolando verdadeiro o Rolar fica desabilitado, mesmo com texto valido', () => {
      const { fixture, el } = montar(true);
      digitar(fixture, el, '1d6');
      expect(botao(el, 'Rolar').disabled).toBe(true);
    });

    it('o Rolar tem 44 px de altura', () => {
      const { el } = montar();
      document.body.appendChild(el);
      expect(getComputedStyle(botao(el, 'Rolar')).minHeight).toBe('44px');
      expect(getComputedStyle(botao(el, 'Limpar')).minHeight).toBe('44px');
      el.remove();
    });

    it('Limpar zera o texto, os contadores, o modificador e o erro', () => {
      const { fixture, el } = montar();
      clicar(fixture, mais(el, 6), 2);
      clicar(fixture, modMais(el), 3);
      digitar(fixture, el, 'abc');
      clicar(fixture, botao(el, 'Limpar'));
      expect(campo(el).value).toBe('');
      expect(contador(el, 6)).toBe('0');
      expect(modValor(el)).toBe('0');
      expect(erro(el)).toBeNull();
      expect(botao(el, 'Rolar').disabled).toBe(true);
    });
  });
});
