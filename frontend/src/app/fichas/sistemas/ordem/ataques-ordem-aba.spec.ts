import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/api-error';
import { FichaApi } from '../../ficha-api';
import { AtaqueOrdem, FichaOrdem, PedidoDeRolagem } from '../../ficha.model';
import { AtaquesOrdemAba } from './ataques-ordem-aba';

function ataque(parcial: Partial<AtaqueOrdem> & { id: number }): AtaqueOrdem {
  return {
    nome: `Ataque ${parcial.id}`, pericia: 'LUTA', bonus: 0, dano: '1d4', tipoDano: null, alcance: null, margem: 20,
    multiplicador: 2, ...parcial,
  };
}

const FACA = ataque({ id: 3, nome: 'Faca', pericia: 'LUTA', bonus: 2, dano: '1d4+1', tipoDano: 'corte', margem: 19, multiplicador: 3 });
const PISTOLA = ataque({
  id: 4, nome: 'Pistola', pericia: 'PONTARIA', bonus: 0, dano: '1d12', tipoDano: 'balístico', alcance: 'Curto', margem: 18, multiplicador: 3,
});

/** Luta treinada (+5) e Pontaria destreinada. */
function fichaCom(ataques: AtaqueOrdem[], pericias: FichaOrdem['pericias'] = { LUTA: 'TREINADO' }): FichaOrdem {
  return {
    id: 7, sistema: 'ORDEM_PARANORMAL', jogador: 'Arthur', personagem: 'Detetive Lia', genero: 'F', idade: null,
    retratoVersao: null, classe: 'COMBATENTE', origem: null, nex: 10,
    atributos: { AGI: 3, FOR: 2, INT: 4, PRE: 2, VIG: 0 },
    recursos: { PV: { atual: 9, maximo: 12 }, SAN: { atual: 48, maximo: 60 }, PE: { atual: 5, maximo: 10 } },
    pericias,
    descricao: { aparencia: null, historia: null, anotacoes: null },
    habilidades: [],
    itens: [],
    rituais: [],
    ataques,
  };
}

function montar(ficha: FichaOrdem, api: Partial<Record<keyof FichaApi, unknown>> = {}, rolando = false) {
  TestBed.configureTestingModule({ imports: [AtaquesOrdemAba], providers: [{ provide: FichaApi, useValue: api }] });
  const fixture = TestBed.createComponent(AtaquesOrdemAba);
  fixture.componentRef.setInput('ficha', ficha);
  fixture.componentRef.setInput('rolando', rolando);
  fixture.detectChanges();
  const emitidas: AtaqueOrdem[][] = [];
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

describe('AtaquesOrdemAba', () => {
  it('a lista vazia diz Nenhum ataque ainda.', () => {
    expect(montar(fichaCom([])).el.textContent).toContain('Nenhum ataque ainda.');
  });

  it('o cartao mostra pericia, bonus (grau + bonus do ataque), dano, tipo e o critico', () => {
    const { el } = montar(fichaCom([FACA, PISTOLA]));
    // Luta treinada +5 e bonus 2: +7. Pontaria destreinada e bonus 0: +0.
    expect(resumo(el, 0)).toBe('Luta · +7 · 1d4+1 corte · crítico 19/×3');
    expect(resumo(el, 1)).toBe('Pontaria · +0 · 1d12 balístico · crítico 18/×3');
  });

  it('o resumo sem tipo de dano nao deixa espaco sobrando, e o alcance vai nos detalhes', () => {
    const { el } = montar(fichaCom([ataque({ id: 1, nome: 'Soco', dano: '1d4+0', margem: 20, multiplicador: 2 }), PISTOLA]));
    expect(resumo(el, 0)).toBe('Luta · +5 · 1d4+0 · crítico 20/×2');
    const cartoes = Array.from(el.querySelectorAll('app-lista-editavel .cartao'));
    expect(cartoes[0].querySelector('.detalhes')).toBeNull();
    expect(cartoes[1].querySelector('.detalhes')?.textContent?.trim()).toBe('Alcance: Curto');
  });

  it('o bonus usa os valores atuais da ficha: veterano em Luta sobe o resumo', () => {
    const { el } = montar(fichaCom([FACA], { LUTA: 'VETERANO' }));
    expect(resumo(el, 0)).toBe('Luta · +12 · 1d4+1 corte · crítico 19/×3');
  });

  it('bonus negativo usa o sinal de menos', () => {
    const { el } = montar(fichaCom([ataque({ id: 1, pericia: 'PONTARIA', bonus: -3 })], {}));
    expect(resumo(el, 0)).toContain('Pontaria · −3 ·');
  });

  it('o botao Rolar tem alvo de 44 px, nome acessivel e emite o pedido do ataque', () => {
    const { el, pedidos } = montar(fichaCom([FACA]));
    const rolar = el.querySelector<HTMLButtonElement>('button[aria-label="Rolar ataque Faca"]')!;
    expect(rolar).not.toBeNull();
    document.body.appendChild(el);
    expect(getComputedStyle(rolar).minHeight).toBe('44px');
    el.remove();
    rolar.click();
    expect(pedidos).toEqual([{ tipo: 'ataque', ataqueId: 3 }]);
    expect('modo' in pedidos[0]).toBe(false);
  });

  it('com rolando verdadeiro o Rolar fica desabilitado', () => {
    const { el } = montar(fichaCom([FACA, PISTOLA]), {}, true);
    const botoes = Array.from(el.querySelectorAll<HTMLButtonElement>('button[aria-label^="Rolar ataque"]'));
    expect(botoes).toHaveLength(2);
    expect(botoes.every((b) => b.disabled)).toBe(true);
  });

  it('adicionar chama criarAtaque com os padroes de Ordem (Luta, bonus 0, margem 20, x2) e emite a lista', async () => {
    const novo = ataque({ id: 5, nome: 'Machado', dano: '1d8' });
    const criarAtaque = vi.fn(() => of(novo));
    const { fixture, el, emitidas } = montar(fichaCom([FACA]), { criarAtaque });
    await clicar(fixture, botao(el, 'Adicionar ataque'));
    digitar(el, 'nome', '  Machado ');
    digitar(el, 'dano', '1d8');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarAtaque).toHaveBeenCalledWith(7, {
      nome: 'Machado', pericia: 'LUTA', bonus: 0, dano: '1d8', tipoDano: null, alcance: null, margem: 20, multiplicador: 2,
    });
    expect(emitidas).toEqual([[FACA, novo]]);
    expect(el.querySelector('app-lista-editavel form')).toBeNull();
  });

  it('o seletor oferece Luta e Pontaria e os numeros vao para a API como numeros', async () => {
    const criarAtaque = vi.fn(() => of(ataque({ id: 6 })));
    const { fixture, el } = montar(fichaCom([]), { criarAtaque });
    await clicar(fixture, botao(el, 'Adicionar ataque'));
    digitar(el, 'nome', 'Arco');
    digitar(el, 'dano', '1d6+2');
    digitar(el, 'bonus', '-1');
    digitar(el, 'margem', '19');
    digitar(el, 'multiplicador', '4');
    digitar(el, 'tipoDano', ' perfuração ');
    digitar(el, 'alcance', 'Médio');
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const pericia = await loader.getHarness(MatSelectHarness.with({ selector: '[data-campo="pericia"] mat-select' }));
    await pericia.open();
    const opcoes = await Promise.all((await pericia.getOptions()).map(async (o) => (await o.getText()).trim()));
    expect(opcoes).toEqual(['Luta', 'Pontaria']);
    await pericia.clickOptions({ text: 'Pontaria' });
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarAtaque).toHaveBeenCalledWith(7, {
      nome: 'Arco', pericia: 'PONTARIA', bonus: -1, dano: '1d6+2', tipoDano: 'perfuração', alcance: 'Médio', margem: 19, multiplicador: 4,
    });
  });

  it('editar traz os valores, salva com atualizarAtaque e emite a lista com o ataque trocado', async () => {
    const trocado = { ...FACA, nome: 'Faca de combate', bonus: 3 };
    const atualizarAtaque = vi.fn(() => of(trocado));
    const { fixture, el, emitidas } = montar(fichaCom([FACA, PISTOLA]), { atualizarAtaque });
    await clicar(fixture, el.querySelector('button[aria-label="Editar Faca"]'));
    expect(el.querySelector<HTMLInputElement>('input[formControlName="nome"]')!.value).toBe('Faca');
    expect(el.querySelector<HTMLInputElement>('input[formControlName="bonus"]')!.value).toBe('2');
    expect(el.querySelector<HTMLInputElement>('input[formControlName="margem"]')!.value).toBe('19');
    expect(el.querySelector<HTMLInputElement>('input[formControlName="multiplicador"]')!.value).toBe('3');
    digitar(el, 'nome', 'Faca de combate');
    digitar(el, 'bonus', '3');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(atualizarAtaque).toHaveBeenCalledWith(7, 3, {
      nome: 'Faca de combate', pericia: 'LUTA', bonus: 3, dano: '1d4+1', tipoDano: 'corte', alcance: null, margem: 19, multiplicador: 3,
    });
    expect(emitidas).toEqual([[trocado, PISTOLA]]);
  });

  it('remover chama excluirAtaque so depois de Sim, remover e emite a lista sem ele', async () => {
    const excluirAtaque = vi.fn(() => of(undefined));
    const { fixture, el, emitidas } = montar(fichaCom([FACA, PISTOLA]), { excluirAtaque });
    await clicar(fixture, el.querySelector('button[aria-label="Remover Faca"]'));
    expect(excluirAtaque).not.toHaveBeenCalled();
    await clicar(fixture, botao(el, 'Sim, remover'));

    expect(excluirAtaque).toHaveBeenCalledWith(7, 3);
    expect(emitidas).toEqual([[PISTOLA]]);
  });

  it.each([
    ['nome vazio', { nome: '', dano: '1d4' }],
    ['dano vazio', { nome: 'X', dano: '' }],
    ['dano invalido', { nome: 'X', dano: '1d7' }],
    ['bonus fora de -99 a 99', { nome: 'X', dano: '1d4', bonus: '100' }],
    ['margem 1', { nome: 'X', dano: '1d4', margem: '1' }],
    ['margem 21', { nome: 'X', dano: '1d4', margem: '21' }],
    ['multiplicador 1', { nome: 'X', dano: '1d4', multiplicador: '1' }],
    ['multiplicador 6', { nome: 'X', dano: '1d4', multiplicador: '6' }],
  ])('%s bloqueia o envio', async (_caso, campos) => {
    const criarAtaque = vi.fn();
    const { fixture, el } = montar(fichaCom([]), { criarAtaque });
    await clicar(fixture, botao(el, 'Adicionar ataque'));
    for (const [campo, valor] of Object.entries(campos)) {
      digitar(el, campo, valor);
    }
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarAtaque).not.toHaveBeenCalled();
    expect(el.querySelector('app-lista-editavel form')).not.toBeNull();
  });

  it('dano invalido mostra a mensagem da notacao no campo', async () => {
    const { fixture, el } = montar(fichaCom([]));
    await clicar(fixture, botao(el, 'Adicionar ataque'));
    digitar(el, 'nome', 'X');
    digitar(el, 'dano', '1d7');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(el.querySelector('[data-campo="dano"]')?.textContent).toContain('Não existe d7.');
  });

  it('bonus 99 e -99, margem 2 e multiplicador 5 passam', async () => {
    const criarAtaque = vi.fn(() => of(ataque({ id: 9 })));
    const { fixture, el } = montar(fichaCom([]), { criarAtaque });
    await clicar(fixture, botao(el, 'Adicionar ataque'));
    digitar(el, 'nome', 'X');
    digitar(el, 'dano', '1d4');
    digitar(el, 'bonus', '-99');
    digitar(el, 'margem', '2');
    digitar(el, 'multiplicador', '5');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarAtaque).toHaveBeenCalledWith(7, expect.objectContaining({ bonus: -99, margem: 2, multiplicador: 5 }));
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

  it('erro sem campo (servidor fora do ar) aparece como alerta', async () => {
    const erro: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'Pode estar acordando.', campos: {} };
    const { fixture, el } = montar(fichaCom([FACA]), { excluirAtaque: vi.fn(() => throwError(() => erro)) });
    await clicar(fixture, el.querySelector('button[aria-label="Remover Faca"]'));
    await clicar(fixture, botao(el, 'Sim, remover'));

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Servidor indisponível');
  });

  it('clique duplo em salvar chama a API uma vez so', async () => {
    const criarAtaque = vi.fn(() => new Subject<AtaqueOrdem>());
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
