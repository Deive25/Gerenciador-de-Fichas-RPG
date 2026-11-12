import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/api-error';
import { FichaApi } from '../../ficha-api';
import { AtaqueDnd, FichaDnd, PedidoDeRolagem } from '../../ficha.model';
import { espacosZerados } from '../../testes/espacos-zerados';
import { AtaquesDndAba } from './ataques-dnd-aba';

function ataque(parcial: Partial<AtaqueDnd> & { id: number }): AtaqueDnd {
  return {
    nome: `Ataque ${parcial.id}`, atributo: 'FOR', proficiente: false, bonus: 0, dano: '1d8', tipoDano: null, alcance: null,
    ...parcial,
  };
}

const ESPADA = ataque({ id: 3, nome: 'Espada longa', atributo: 'FOR', proficiente: true, bonus: 0, dano: '1d8+3', tipoDano: 'cortante' });
const ARCO = ataque({ id: 4, nome: 'Arco curto', atributo: 'DES', proficiente: false, bonus: 1, dano: '1d6+2', tipoDano: 'perfurante', alcance: '24/96 m' });

/** Nivel 5 (proficiencia +3), FOR 16 (+3) e DES 14 (+2). */
function fichaCom(ataques: AtaqueDnd[], atributos: FichaDnd['atributos'] = { FOR: 16, DES: 14, CON: 12, INT: 10, SAB: 9, CAR: 10 }): FichaDnd {
  return {
    id: 9, sistema: 'DND', jogador: 'Arthur', personagem: 'Kael', genero: 'M', idade: 27, retratoVersao: null,
    edicao: 'D2024', classe: 'Guerreiro', especie: 'Humano', antecedente: 'Soldado', nivel: 5,
    atributos,
    recursos: { PV: { atual: 30, maximo: 38 }, PV_TEMP: { atual: 0, maximo: null } },
    ca: 15, inspiracao: false, atributoConjuracao: null, salvaguardas: [], pericias: {},
    descricao: { aparencia: null, historia: null, anotacoes: null },
    habilidades: [], itens: [], moedas: { pc: 0, pp: 0, pe: 0, po: 0, pl: 0 }, magias: [], espacos: espacosZerados(),
    ataques,
  };
}

function montar(ficha: FichaDnd, api: Partial<Record<keyof FichaApi, unknown>> = {}, rolando = false) {
  TestBed.configureTestingModule({ imports: [AtaquesDndAba], providers: [{ provide: FichaApi, useValue: api }] });
  const fixture = TestBed.createComponent(AtaquesDndAba);
  fixture.componentRef.setInput('ficha', ficha);
  fixture.componentRef.setInput('rolando', rolando);
  fixture.detectChanges();
  const emitidas: AtaqueDnd[][] = [];
  const pedidos: PedidoDeRolagem[] = [];
  fixture.componentInstance.atualizada.subscribe((l) => emitidas.push(l));
  fixture.componentInstance.rolar.subscribe((p) => pedidos.push(p));
  return { fixture, el: fixture.nativeElement as HTMLElement, emitidas, pedidos };
}

function botao(el: HTMLElement, texto: string): HTMLButtonElement {
  return Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.trim() === texto) as HTMLButtonElement;
}

async function clicar(fixture: ComponentFixture<unknown>, b: HTMLElement | null | undefined): Promise<void> {
  b!.click();
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
}

function digitar(el: HTMLElement, campo: string, valor: string): void {
  const entrada = el.querySelector<HTMLInputElement>(`input[formControlName="${campo}"]`)!;
  entrada.value = valor;
  entrada.dispatchEvent(new Event('input'));
}

function resumo(el: HTMLElement, indice: number): string {
  return el.querySelectorAll('app-lista-editavel .cartao')[indice].querySelector('.conta')!.textContent!.replace(/\s+/g, ' ').trim();
}

const modo = (el: HTMLElement, nome: string) =>
  Array.from(el.querySelectorAll<HTMLButtonElement>('.modo button')).find((b) => b.textContent?.trim() === nome)!;

describe('AtaquesDndAba', () => {
  it('a lista vazia diz Nenhum ataque ainda.', () => {
    expect(montar(fichaCom([])).el.textContent).toContain('Nenhum ataque ainda.');
  });

  it('o cartao mostra o bonus (mod + proficiencia + bonus), o dano e o tipo', () => {
    const { el } = montar(fichaCom([ESPADA, ARCO]));
    // FOR 16 = +3 e proficiente (+3 no nivel 5): +6. DES 14 = +2, sem proficiencia e bonus 1: +3.
    expect(resumo(el, 0)).toBe('+6 · 1d8+3 cortante');
    expect(resumo(el, 1)).toBe('+3 · 1d6+2 perfurante');
  });

  it('o alcance vai nos detalhes, so quando existe', () => {
    const { el } = montar(fichaCom([ESPADA, ARCO]));
    const cartoes = Array.from(el.querySelectorAll('app-lista-editavel .cartao'));
    expect(cartoes[0].querySelector('.detalhes')).toBeNull();
    expect(cartoes[1].querySelector('.detalhes')?.textContent?.trim()).toBe('Alcance: 24/96 m');
  });

  it('o bonus usa os valores atuais da ficha: DES 20 sobe o arco', () => {
    const { el } = montar(fichaCom([ARCO], { FOR: 16, DES: 20, CON: 12, INT: 10, SAB: 9, CAR: 10 }));
    expect(resumo(el, 0)).toBe('+6 · 1d6+2 perfurante'); // +5 + 0 + 1
  });

  it('modificador negativo usa o sinal de menos e o dano sem tipo nao deixa espaco sobrando', () => {
    const { el } = montar(fichaCom([ataque({ id: 1, atributo: 'INT', bonus: -2, dano: '1d4' })], { FOR: 16, DES: 14, CON: 12, INT: 8, SAB: 9, CAR: 10 }));
    expect(resumo(el, 0)).toBe('−3 · 1d4');
  });

  it('o botao Rolar tem 44 px, nome acessivel e emite o pedido sem modo no Normal', () => {
    const { el, pedidos } = montar(fichaCom([ESPADA]));
    const rolar = el.querySelector<HTMLButtonElement>('button[aria-label="Rolar ataque Espada longa"]')!;
    document.body.appendChild(el);
    expect(getComputedStyle(rolar).minHeight).toBe('44px');
    el.remove();
    rolar.click();
    expect(pedidos).toEqual([{ tipo: 'ataque', ataqueId: 3 }]);
    expect('modo' in pedidos[0]).toBe(false);
  });

  it('com Vantagem escolhida, o pedido leva o modo e o seletor volta a Normal', () => {
    const { fixture, el, pedidos } = montar(fichaCom([ESPADA]));
    modo(el, 'Vantagem').click();
    fixture.detectChanges();
    expect(modo(el, 'Vantagem').getAttribute('aria-pressed')).toBe('true');

    el.querySelector<HTMLButtonElement>('button[aria-label="Rolar ataque Espada longa"]')!.click();
    fixture.detectChanges();

    expect(pedidos).toEqual([{ tipo: 'ataque', ataqueId: 3, modo: 'VANTAGEM' }]);
    expect(modo(el, 'Normal').getAttribute('aria-pressed')).toBe('true');
    expect(modo(el, 'Vantagem').getAttribute('aria-pressed')).toBe('false');
  });

  it('com Desvantagem, o pedido leva DESVANTAGEM', () => {
    const { fixture, el, pedidos } = montar(fichaCom([ESPADA, ARCO]));
    modo(el, 'Desvantagem').click();
    fixture.detectChanges();
    el.querySelector<HTMLButtonElement>('button[aria-label="Rolar ataque Arco curto"]')!.click();
    expect(pedidos).toEqual([{ tipo: 'ataque', ataqueId: 4, modo: 'DESVANTAGEM' }]);
  });

  it('com rolando verdadeiro o Rolar fica desabilitado', () => {
    const { el } = montar(fichaCom([ESPADA, ARCO]), {}, true);
    const botoes = Array.from(el.querySelectorAll<HTMLButtonElement>('button[aria-label^="Rolar ataque"]'));
    expect(botoes).toHaveLength(2);
    expect(botoes.every((b) => b.disabled)).toBe(true);
  });

  it('adicionar chama criarAtaque com os padroes de D&D (FOR, nao proficiente, bonus 0) e emite a lista', async () => {
    const novo = ataque({ id: 5, nome: 'Adaga', dano: '1d4' });
    const criarAtaque = vi.fn(() => of(novo));
    const { fixture, el, emitidas } = montar(fichaCom([ESPADA]), { criarAtaque });
    await clicar(fixture, botao(el, 'Adicionar ataque'));
    digitar(el, 'nome', ' Adaga ');
    digitar(el, 'dano', '1d4');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarAtaque).toHaveBeenCalledWith(9, {
      nome: 'Adaga', atributo: 'FOR', proficiente: false, bonus: 0, dano: '1d4', tipoDano: null, alcance: null,
    });
    expect(emitidas).toEqual([[ESPADA, novo]]);
    expect(el.querySelector('app-lista-editavel form')).toBeNull();
  });

  it('o seletor de atributo oferece os 6 e a caixa Proficiente vai para a API', async () => {
    const criarAtaque = vi.fn(() => of(ataque({ id: 6 })));
    const { fixture, el } = montar(fichaCom([]), { criarAtaque });
    await clicar(fixture, botao(el, 'Adicionar ataque'));
    digitar(el, 'nome', 'Rapieira');
    digitar(el, 'dano', '1d8+2');
    digitar(el, 'bonus', '-1');
    digitar(el, 'tipoDano', 'perfurante');
    digitar(el, 'alcance', '1,5 m');
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const atributo = await loader.getHarness(MatSelectHarness.with({ selector: '[data-campo="atributo"] mat-select' }));
    await atributo.open();
    const opcoes = await Promise.all((await atributo.getOptions()).map(async (o) => (await o.getText()).trim()));
    expect(opcoes).toHaveLength(6);
    expect(opcoes).toContain('Destreza');
    await atributo.clickOptions({ text: 'Destreza' });
    el.querySelector<HTMLInputElement>('[data-campo="proficiente"] input')!.click();
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarAtaque).toHaveBeenCalledWith(9, {
      nome: 'Rapieira', atributo: 'DES', proficiente: true, bonus: -1, dano: '1d8+2', tipoDano: 'perfurante', alcance: '1,5 m',
    });
  });

  it('editar traz os valores, salva com atualizarAtaque e emite a lista com o ataque trocado', async () => {
    const trocado = { ...ESPADA, bonus: 1 };
    const atualizarAtaque = vi.fn(() => of(trocado));
    const { fixture, el, emitidas } = montar(fichaCom([ESPADA, ARCO]), { atualizarAtaque });
    await clicar(fixture, el.querySelector('button[aria-label="Editar Espada longa"]'));
    expect(el.querySelector<HTMLInputElement>('input[formControlName="nome"]')!.value).toBe('Espada longa');
    expect(el.querySelector<HTMLInputElement>('input[formControlName="dano"]')!.value).toBe('1d8+3');
    expect(el.querySelector<HTMLInputElement>('[data-campo="proficiente"] input')!.checked).toBe(true);
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const atributo = await loader.getHarness(MatSelectHarness.with({ selector: '[data-campo="atributo"] mat-select' }));
    expect(await atributo.getValueText()).toBe('Força');
    digitar(el, 'bonus', '1');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(atualizarAtaque).toHaveBeenCalledWith(9, 3, {
      nome: 'Espada longa', atributo: 'FOR', proficiente: true, bonus: 1, dano: '1d8+3', tipoDano: 'cortante', alcance: null,
    });
    expect(emitidas).toEqual([[trocado, ARCO]]);
  });

  it('remover chama excluirAtaque so depois de Sim, remover e emite a lista sem ele', async () => {
    const excluirAtaque = vi.fn(() => of(undefined));
    const { fixture, el, emitidas } = montar(fichaCom([ESPADA, ARCO]), { excluirAtaque });
    await clicar(fixture, el.querySelector('button[aria-label="Remover Espada longa"]'));
    expect(excluirAtaque).not.toHaveBeenCalled();
    await clicar(fixture, botao(el, 'Sim, remover'));

    expect(excluirAtaque).toHaveBeenCalledWith(9, 3);
    expect(emitidas).toEqual([[ARCO]]);
  });

  it.each([
    ['nome vazio', { nome: '', dano: '1d4' }],
    ['dano vazio', { nome: 'X', dano: '' }],
    ['dano invalido', { nome: 'X', dano: 'd6' }],
    ['bonus 100', { nome: 'X', dano: '1d4', bonus: '100' }],
    ['bonus texto', { nome: 'X', dano: '1d4', bonus: 'abc' }],
  ])('%s bloqueia o envio', async (_caso, campos) => {
    const criarAtaque = vi.fn();
    const { fixture, el } = montar(fichaCom([]), { criarAtaque });
    await clicar(fixture, botao(el, 'Adicionar ataque'));
    for (const [campo, valor] of Object.entries(campos)) {
      digitar(el, campo, valor);
    }
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarAtaque).not.toHaveBeenCalled();
  });

  it('erro do servidor no campo dano aparece no campo e nao emite', async () => {
    const erro: ApiError = { status: 400, titulo: 'Dados inválidos', detalhe: 'Corrija.', campos: { dano: 'dano inválido' } };
    const { fixture, el, emitidas } = montar(fichaCom([]), { criarAtaque: vi.fn(() => throwError(() => erro)) });
    await clicar(fixture, botao(el, 'Adicionar ataque'));
    digitar(el, 'nome', 'X');
    digitar(el, 'dano', '1d4');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(emitidas).toEqual([]);
    expect(el.querySelector('[data-campo="dano"]')?.textContent).toContain('dano inválido');
  });

  it('clique duplo em salvar chama a API uma vez so', async () => {
    const criarAtaque = vi.fn(() => new Subject<AtaqueDnd>());
    const { fixture, el } = montar(fichaCom([]), { criarAtaque });
    await clicar(fixture, botao(el, 'Adicionar ataque'));
    digitar(el, 'nome', 'X');
    digitar(el, 'dano', '1d4');
    const salvar = botao(el, 'Salvar');
    salvar.click();
    salvar.click();
    fixture.detectChanges();

    expect(criarAtaque).toHaveBeenCalledTimes(1);
  });
});
