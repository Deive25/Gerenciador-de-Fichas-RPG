import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { FichaApi } from '../ficha-api';
import { DescricaoFicha, FichaOrdem } from '../ficha.model';
import { DescricaoAba } from './descricao-aba';

const FICHA: FichaOrdem = {
  id: 7, sistema: 'ORDEM_PARANORMAL', jogador: 'Arthur', personagem: 'Detetive Lia', genero: 'F', idade: null,
  retratoVersao: null, classe: 'OCULTISTA', origem: null, nex: 10,
  atributos: { AGI: 3, FOR: 1, INT: 4, PRE: 2, VIG: 0 },
  recursos: { PV: { atual: 9, maximo: 12 }, SAN: { atual: 48, maximo: 60 }, PE: { atual: 5, maximo: 10 } },
  pericias: {},
  descricao: { aparencia: 'Alta e magra', historia: null, anotacoes: null },
  habilidades: [],
  itens: [],
  rituais: [],
};

function montar(api: Partial<Record<keyof FichaApi, unknown>> = {}) {
  TestBed.configureTestingModule({ imports: [DescricaoAba], providers: [{ provide: FichaApi, useValue: api }] });
  const fixture = TestBed.createComponent(DescricaoAba);
  fixture.componentRef.setInput('ficha', FICHA);
  fixture.detectChanges();
  const emitidas: DescricaoFicha[] = [];
  fixture.componentInstance.atualizada.subscribe((d) => emitidas.push(d));
  return { fixture, el: fixture.nativeElement as HTMLElement, emitidas };
}

function campo(el: HTMLElement, nome: string): HTMLTextAreaElement {
  return el.querySelector<HTMLTextAreaElement>(`[data-campo="${nome}"] textarea`)!;
}

function digitar(fixture: ComponentFixture<unknown>, el: HTMLElement, nome: string, texto: string): void {
  const area = campo(el, nome);
  area.value = texto;
  area.dispatchEvent(new Event('input'));
  fixture.detectChanges();
}

function salvar(el: HTMLElement): HTMLButtonElement {
  return Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes('Salvar descrição')) as HTMLButtonElement;
}

async function clicar(fixture: ComponentFixture<unknown>, b: HTMLElement): Promise<void> {
  b.click();
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
}

describe('DescricaoAba', () => {
  it('os tres campos comecam com os valores da ficha e null aparece vazio', () => {
    const { el } = montar();
    expect(campo(el, 'aparencia').value).toBe('Alta e magra');
    expect(campo(el, 'historia').value).toBe('');
    expect(campo(el, 'anotacoes').value).toBe('');
    expect(el.textContent).toContain('Aparência');
    expect(el.textContent).toContain('História');
    expect(el.textContent).toContain('Anotações');
  });

  it('sem mudanca o botao fica desabilitado', () => {
    const { fixture, el } = montar();
    expect(salvar(el).disabled).toBe(true);
    digitar(fixture, el, 'historia', 'x');
    expect(salvar(el).disabled).toBe(false);
  });

  it('salvar chama atualizarDescricao com o vazio como null, mostra Descricao salva. e emite', async () => {
    const resposta: DescricaoFicha = { aparencia: 'Alta e magra', historia: 'Nasceu em SP', anotacoes: null };
    const atualizarDescricao = vi.fn(() => of(resposta));
    const { fixture, el, emitidas } = montar({ atualizarDescricao });
    digitar(fixture, el, 'historia', 'Nasceu em SP');
    await clicar(fixture, salvar(el));

    expect(atualizarDescricao).toHaveBeenCalledWith(7, { aparencia: 'Alta e magra', historia: 'Nasceu em SP', anotacoes: null });
    expect(el.querySelector('[role="status"]')?.textContent).toContain('Descrição salva.');
    expect(emitidas).toEqual([resposta]);
    expect(salvar(el).disabled).toBe(true);
  });

  it('as quebras de linha chegam a API iguais', async () => {
    const atualizarDescricao = vi.fn((_id: number, d: DescricaoFicha) => of(d));
    const { fixture, el } = montar({ atualizarDescricao });
    digitar(fixture, el, 'anotacoes', 'linha 1\nlinha 2\n\nlinha 4');
    await clicar(fixture, salvar(el));

    expect(atualizarDescricao).toHaveBeenCalledWith(7, expect.objectContaining({ anotacoes: 'linha 1\nlinha 2\n\nlinha 4' }));
  });

  it('erro do servidor no campo aparece no campo', async () => {
    const erro: ApiError = { status: 400, titulo: 'Dados inválidos', detalhe: 'x', campos: { historia: 'Muito longa para o servidor' } };
    const atualizarDescricao = vi.fn(() => throwError(() => erro));
    const { fixture, el, emitidas } = montar({ atualizarDescricao });
    digitar(fixture, el, 'historia', 'abc');
    await clicar(fixture, salvar(el));

    expect(el.querySelector('[data-campo="historia"]')?.textContent).toContain('Muito longa para o servidor');
    expect(emitidas).toEqual([]);
    expect(el.querySelector('[role="status"]')?.textContent?.trim()).toBe('');
  });

  it('erro sem campo mostra o erro geral', async () => {
    const erro: ApiError = { status: 0, titulo: 'Sem conexão', detalhe: 'Tente de novo.', campos: {} };
    const { fixture, el } = montar({ atualizarDescricao: vi.fn(() => throwError(() => erro)) });
    digitar(fixture, el, 'historia', 'abc');
    await clicar(fixture, salvar(el));

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Sem conexão');
  });

  it('clique duplo chama a API uma vez so', () => {
    const envio = new Subject<DescricaoFicha>();
    const atualizarDescricao = vi.fn(() => envio);
    const { fixture, el } = montar({ atualizarDescricao });
    digitar(fixture, el, 'historia', 'abc');
    const botao = salvar(el);
    botao.click();
    botao.click();
    fixture.detectChanges();

    expect(atualizarDescricao).toHaveBeenCalledTimes(1);
  });

  function desabilitados(el: HTMLElement): boolean[] {
    return ['aparencia', 'historia', 'anotacoes'].map((n) => campo(el, n).disabled);
  }

  it('enquanto salva os tres campos ficam desabilitados e voltam com o valor salvo no sucesso', async () => {
    const envio = new Subject<DescricaoFicha>();
    const { fixture, el } = montar({ atualizarDescricao: vi.fn(() => envio) });
    digitar(fixture, el, 'historia', 'abc');
    salvar(el).click();
    fixture.detectChanges();
    expect(desabilitados(el)).toEqual([true, true, true]);

    envio.next({ aparencia: 'Alta e magra', historia: 'abc', anotacoes: null });
    envio.complete();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(desabilitados(el)).toEqual([false, false, false]);
    expect(campo(el, 'historia').value).toBe('abc');
    expect(campo(el, 'aparencia').value).toBe('Alta e magra');
  });

  it('apos um erro os campos voltam habilitados e mantem o que foi digitado', async () => {
    const erro: ApiError = { status: 0, titulo: 'Sem conexão', detalhe: 'Tente de novo.', campos: {} };
    const { fixture, el } = montar({ atualizarDescricao: vi.fn(() => throwError(() => erro)) });
    digitar(fixture, el, 'historia', 'texto digitado');
    await clicar(fixture, salvar(el));

    expect(desabilitados(el)).toEqual([false, false, false]);
    expect(campo(el, 'historia').value).toBe('texto digitado');
  });

  it('o aviso de salvo existe vazio antes de salvar e ganha o texto depois', async () => {
    const atualizarDescricao = vi.fn((_id: number, d: DescricaoFicha) => of(d));
    const { fixture, el } = montar({ atualizarDescricao });
    const status = el.querySelector('[role="status"]');
    expect(status).not.toBeNull();
    expect(status!.textContent?.trim()).toBe('');

    digitar(fixture, el, 'historia', 'abc');
    await clicar(fixture, salvar(el));
    expect(el.querySelector('[role="status"]')).toBe(status);
    expect(status!.textContent).toContain('Descrição salva.');
  });

  it('o contador mostra N/4000', () => {
    const { fixture, el } = montar();
    digitar(fixture, el, 'historia', 'doze letras!');
    expect(el.querySelector('[data-campo="historia"]')?.textContent).toContain('12/4000');
  });

  it('4001 caracteres mostram Maximo de 4000 caracteres e nao enviam', async () => {
    const atualizarDescricao = vi.fn();
    const { fixture, el } = montar({ atualizarDescricao });
    digitar(fixture, el, 'historia', 'a'.repeat(4001));
    await clicar(fixture, salvar(el));

    expect(atualizarDescricao).not.toHaveBeenCalled();
    expect(el.querySelector('[data-campo="historia"]')?.textContent).toContain('Máximo de 4000 caracteres');
  });
});
