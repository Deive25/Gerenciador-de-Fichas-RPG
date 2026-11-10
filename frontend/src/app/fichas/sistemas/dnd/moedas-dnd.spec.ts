import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/api-error';
import { FichaApi } from '../../ficha-api';
import { FichaDnd, Moedas } from '../../ficha.model';
import { MoedasDnd } from './moedas-dnd';

const MOEDAS: Moedas = { pc: 1, pp: 2, pe: 3, po: 4, pl: 5 };

const FICHA: FichaDnd = {
  id: 7, sistema: 'DND', jogador: 'Arthur', personagem: 'Kael', genero: 'M', idade: 27, retratoVersao: null,
  edicao: 'D2024', classe: 'Ladino', especie: 'Halfling', antecedente: 'Criminoso', nivel: 5,
  atributos: { FOR: 10, DES: 14, CON: 12, INT: 10, SAB: 9, CAR: 16 },
  recursos: { PV: { atual: 30, maximo: 38 }, PV_TEMP: { atual: 4, maximo: null } },
  ca: 15, inspiracao: true, salvaguardas: ['DES'],
  pericias: {},
  descricao: { aparencia: null, historia: null, anotacoes: null },
  habilidades: [],
  itens: [],
  moedas: MOEDAS,
};

const NOMES = ['pc', 'pp', 'pe', 'po', 'pl'];

function montar(api: Partial<Record<keyof FichaApi, unknown>> = {}) {
  TestBed.configureTestingModule({ imports: [MoedasDnd], providers: [{ provide: FichaApi, useValue: api }] });
  const fixture = TestBed.createComponent(MoedasDnd);
  fixture.componentRef.setInput('ficha', FICHA);
  fixture.detectChanges();
  const emitidas: Moedas[] = [];
  fixture.componentInstance.atualizadas.subscribe((m) => emitidas.push(m));
  return { fixture, el: fixture.nativeElement as HTMLElement, emitidas };
}

function campo(el: HTMLElement, nome: string): HTMLInputElement {
  return el.querySelector<HTMLInputElement>(`[data-campo="${nome}"] input`)!;
}

function digitar(fixture: ComponentFixture<unknown>, el: HTMLElement, nome: string, valor: string): void {
  const entrada = campo(el, nome);
  entrada.value = valor;
  entrada.dispatchEvent(new Event('input'));
  fixture.detectChanges();
}

function salvar(el: HTMLElement): HTMLButtonElement {
  return Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes('Salvar moedas')) as HTMLButtonElement;
}

async function clicar(fixture: ComponentFixture<unknown>, b: HTMLElement): Promise<void> {
  b.click();
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
}

describe('MoedasDnd', () => {
  it('os cinco campos comecam com os valores da ficha, rotulados PC a PL com o nome por extenso no aria-label', () => {
    const { el } = montar();
    expect(NOMES.map((n) => campo(el, n).value)).toEqual(['1', '2', '3', '4', '5']);
    expect(el.querySelector('legend')?.textContent?.trim()).toBe('Moedas');
    expect(NOMES.map((n) => el.querySelector(`[data-campo="${n}"] mat-label`)?.textContent?.trim())).toEqual([
      'PC', 'PP', 'PE', 'PO', 'PL',
    ]);
    expect(NOMES.map((n) => campo(el, n).getAttribute('aria-label'))).toEqual([
      'PC, peças de cobre', 'PP, peças de prata', 'PE, peças de electro', 'PO, peças de ouro', 'PL, peças de platina',
    ]);
  });

  it('sem mudanca o botao fica desabilitado', () => {
    const { fixture, el } = montar();
    expect(salvar(el).disabled).toBe(true);
    digitar(fixture, el, 'po', '10');
    expect(salvar(el).disabled).toBe(false);
  });

  it('mudar o PO para 10 e salvar chama atualizarMoedas com as cinco, emite a resposta e mostra Moedas salvas.', async () => {
    const resposta: Moedas = { ...MOEDAS, po: 10 };
    const atualizarMoedas = vi.fn(() => of(resposta));
    const { fixture, el, emitidas } = montar({ atualizarMoedas });
    digitar(fixture, el, 'po', '10');
    await clicar(fixture, salvar(el));

    expect(atualizarMoedas).toHaveBeenCalledWith(7, { pc: 1, pp: 2, pe: 3, po: 10, pl: 5 });
    expect(emitidas).toEqual([resposta]);
    expect(el.querySelector('[role="status"]')?.textContent).toContain('Moedas salvas.');
    expect(salvar(el).disabled).toBe(true);
  });

  it('o aviso de salvo existe vazio antes de salvar e e o mesmo elemento depois', async () => {
    const { fixture, el } = montar({ atualizarMoedas: vi.fn(() => of({ ...MOEDAS, po: 10 })) });
    const status = el.querySelector('[role="status"]');
    expect(status).not.toBeNull();
    expect(status!.textContent?.trim()).toBe('');

    digitar(fixture, el, 'po', '10');
    await clicar(fixture, salvar(el));
    expect(el.querySelector('[role="status"]')).toBe(status);
    expect(status!.textContent).toContain('Moedas salvas.');
  });

  it.each([
    ['negativo', '-1'],
    ['decimal', '1.5'],
    ['texto', 'abc'],
    ['vazio', ''],
    ['acima do inteiro do servidor', '2147483648'],
  ])('valor %s desabilita o botao', (_rotulo, valor) => {
    const { fixture, el } = montar();
    digitar(fixture, el, 'po', valor);
    expect(salvar(el).disabled).toBe(true);
  });

  it('enquanto grava os campos ficam desabilitados e voltam habilitados no sucesso', async () => {
    const envio = new Subject<Moedas>();
    const { fixture, el } = montar({ atualizarMoedas: vi.fn(() => envio) });
    digitar(fixture, el, 'po', '10');
    salvar(el).click();
    fixture.detectChanges();
    expect(NOMES.map((n) => campo(el, n).disabled)).toEqual([true, true, true, true, true]);

    envio.next({ ...MOEDAS, po: 10 });
    envio.complete();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(NOMES.map((n) => campo(el, n).disabled)).toEqual([false, false, false, false, false]);
    expect(campo(el, 'po').value).toBe('10');
  });

  it('apos um erro os campos voltam habilitados e mantem o que foi digitado', async () => {
    const erro: ApiError = { status: 0, titulo: 'Sem conexão', detalhe: 'Tente de novo.', campos: {} };
    const { fixture, el, emitidas } = montar({ atualizarMoedas: vi.fn(() => throwError(() => erro)) });
    digitar(fixture, el, 'po', '10');
    await clicar(fixture, salvar(el));

    expect(NOMES.map((n) => campo(el, n).disabled)).toEqual([false, false, false, false, false]);
    expect(campo(el, 'po').value).toBe('10');
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Sem conexão');
    expect(emitidas).toEqual([]);
  });

  it('erro do servidor no campo po aparece no campo', async () => {
    const erro: ApiError = { status: 400, titulo: 'Dados inválidos', detalhe: 'x', campos: { po: 'valor inválido' } };
    const { fixture, el } = montar({ atualizarMoedas: vi.fn(() => throwError(() => erro)) });
    digitar(fixture, el, 'po', '10');
    await clicar(fixture, salvar(el));

    expect(el.querySelector('[data-campo="po"]')?.textContent).toContain('valor inválido');
    expect(el.querySelector('[role="status"]')?.textContent?.trim()).toBe('');
  });

  it('clique duplo chama a API uma vez so', () => {
    const atualizarMoedas = vi.fn(() => new Subject<Moedas>());
    const { fixture, el } = montar({ atualizarMoedas });
    digitar(fixture, el, 'po', '10');
    const botao = salvar(el);
    botao.click();
    botao.click();
    fixture.detectChanges();

    expect(atualizarMoedas).toHaveBeenCalledTimes(1);
  });
});

describe('MoedasDnd · teto', () => {
  it('2147483647, o maior inteiro do servidor, e aceito', () => {
    const { fixture, el } = montar();
    digitar(fixture, el, 'po', '2147483647');
    expect(salvar(el).disabled).toBe(false);
  });
});

describe('MoedasDnd · ficha trocada', () => {
  it('moeda digitada e nao salva sobrevive a troca do objeto da ficha com as mesmas moedas', () => {
    const { fixture, el } = montar();
    digitar(fixture, el, 'po', '50');
    const outroItem = { id: 1, nome: 'Corda', quantidade: 1, peso: 1, equipado: false, descricao: null };
    fixture.componentRef.setInput('ficha', { ...FICHA, itens: [outroItem] });
    fixture.detectChanges();

    expect(campo(el, 'po').value).toBe('50');
    expect(salvar(el).disabled).toBe(false);
  });
});
