import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/api-error';
import { FichaApi } from '../../ficha-api';
import { FichaOrdem, RitualOrdem } from '../../ficha.model';
import { RituaisOrdemAba } from './rituais-ordem-aba';

function ritual(parcial: Partial<RitualOrdem> & { id: number }): RitualOrdem {
  return {
    nome: `Ritual ${parcial.id}`, circulo: 1, elemento: 'SANGUE', execucao: null, alcance: null, alvo: null,
    duracao: null, resistencia: null, descricao: null, ...parcial,
  };
}

const CIRCULO_1 = ritual({ id: 3, nome: 'Cicatrização', circulo: 1, elemento: 'SANGUE' });
const CIRCULO_2 = ritual({
  id: 4, nome: 'Rasgar a Realidade', circulo: 2, elemento: 'MORTE', execucao: 'Padrão', alcance: 'Curto', alvo: '1 ser',
  duracao: 'Cena', resistencia: 'Vontade parcial', descricao: 'Abre uma fenda.\nCuidado.',
});

/** NEX 10 e Presença 2: DT 10 + 2 + 2 = 14. */
function fichaCom(rituais: RitualOrdem[], nex = 10, presenca = 2): FichaOrdem {
  return {
    id: 7, sistema: 'ORDEM_PARANORMAL', jogador: 'Arthur', personagem: 'Detetive Lia', genero: 'F', idade: null,
    retratoVersao: null, classe: 'OCULTISTA', origem: null, nex,
    atributos: { AGI: 3, FOR: 2, INT: 4, PRE: presenca, VIG: 0 },
    recursos: { PV: { atual: 9, maximo: 12 }, SAN: { atual: 48, maximo: 60 }, PE: { atual: 5, maximo: 10 } },
    pericias: {},
    descricao: { aparencia: null, historia: null, anotacoes: null },
    habilidades: [],
    itens: [],
    rituais,
    ataques: [],
  };
}

function montar(ficha: FichaOrdem, api: Partial<Record<keyof FichaApi, unknown>> = {}) {
  TestBed.configureTestingModule({ imports: [RituaisOrdemAba], providers: [{ provide: FichaApi, useValue: api }] });
  const fixture = TestBed.createComponent(RituaisOrdemAba);
  fixture.componentRef.setInput('ficha', ficha);
  fixture.detectChanges();
  const emitidas: RitualOrdem[][] = [];
  fixture.componentInstance.atualizada.subscribe((l) => emitidas.push(l));
  return { fixture, el: fixture.nativeElement as HTMLElement, emitidas };
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
  const entrada = el.querySelector<HTMLInputElement | HTMLTextAreaElement>(
    `input[formControlName="${campo}"], textarea[formControlName="${campo}"]`,
  )!;
  entrada.value = valor;
  entrada.dispatchEvent(new Event('input'));
}

function resumo(el: HTMLElement, indice: number): string {
  return el.querySelectorAll('app-lista-editavel .cartao')[indice].querySelector('.conta')!.textContent!.replace(/\s+/g, ' ').trim();
}

describe('RituaisOrdemAba', () => {
  it('a lista vazia diz Nenhum ritual ainda.', () => {
    const { el } = montar(fichaCom([]));
    expect(el.textContent).toContain('Nenhum ritual ainda.');
  });

  it('o cartao mostra circulo, elemento, custo em PE e DT', () => {
    const { el } = montar(fichaCom([CIRCULO_1, CIRCULO_2]));
    expect(resumo(el, 0)).toBe('1º círculo · Sangue · 1 PE · DT 14');
    expect(resumo(el, 1)).toBe('2º círculo · Morte · 3 PE · DT 14');
  });

  it('o custo muda com o circulo (1, 3, 6, 10) e a DT com o NEX e a Presenca', () => {
    const rituais = [1, 2, 3, 4].map((circulo) => ritual({ id: circulo, circulo }));
    const { el } = montar(fichaCom(rituais, 99, 3));
    // DT = 10 + 19 + 3 = 32.
    expect([0, 1, 2, 3].map((i) => resumo(el, i))).toEqual([
      '1º círculo · Sangue · 1 PE · DT 32',
      '2º círculo · Sangue · 3 PE · DT 32',
      '3º círculo · Sangue · 6 PE · DT 32',
      '4º círculo · Sangue · 10 PE · DT 32',
    ]);
  });

  it('mostra os detalhes e a descricao so quando existem', () => {
    const { el } = montar(fichaCom([CIRCULO_1, CIRCULO_2]));
    const cartoes = Array.from(el.querySelectorAll('app-lista-editavel .cartao'));
    expect(cartoes[0].querySelector('.detalhes')).toBeNull();
    expect(cartoes[0].querySelector('.descricao')).toBeNull();
    expect(cartoes[1].querySelector('.detalhes')?.textContent?.trim()).toBe(
      'Execução: Padrão · Alcance: Curto · Alvo: 1 ser · Duração: Cena · Resistência: Vontade parcial',
    );
    expect(cartoes[1].querySelector('.descricao')?.textContent).toContain('Abre uma fenda.\nCuidado.');
  });

  it('adicionar chama criarRitual (circulo 1 e Sangue por padrao) e emite a lista com o ritual no lugar certo', async () => {
    const novo = ritual({ id: 5, nome: 'Amaldiçoar Arma', circulo: 1, elemento: 'SANGUE' });
    const criarRitual = vi.fn(() => of(novo));
    const { fixture, el, emitidas } = montar(fichaCom([CIRCULO_1, CIRCULO_2]), { criarRitual });
    await clicar(fixture, botao(el, 'Adicionar ritual'));
    digitar(el, 'nome', '  Amaldiçoar Arma ');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarRitual).toHaveBeenCalledWith(7, {
      nome: 'Amaldiçoar Arma', circulo: 1, elemento: 'SANGUE', execucao: null, alcance: null, alvo: null,
      duracao: null, resistencia: null, descricao: null,
    });
    // Por círculo e criação: o novo (id 5, círculo 1) fica depois do id 3 e antes do círculo 2.
    expect(emitidas).toEqual([[CIRCULO_1, novo, CIRCULO_2]]);
    expect(el.querySelector('app-lista-editavel form')).toBeNull();
  });

  it('os selects oferecem os 4 circulos e os 5 elementos, e a escolha vai para a API', async () => {
    const criarRitual = vi.fn(() => of(ritual({ id: 6, circulo: 4, elemento: 'MEDO' })));
    const { fixture, el } = montar(fichaCom([]), { criarRitual });
    await clicar(fixture, botao(el, 'Adicionar ritual'));
    digitar(el, 'nome', 'Terror');
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const circulo = await loader.getHarness(MatSelectHarness.with({ selector: '[data-campo="circulo"] mat-select' }));
    await circulo.open();
    const circulos = await Promise.all((await circulo.getOptions()).map(async (o) => (await o.getText()).trim()));
    expect(circulos).toEqual(['1º círculo', '2º círculo', '3º círculo', '4º círculo']);
    await circulo.clickOptions({ text: '4º círculo' });
    const elemento = await loader.getHarness(MatSelectHarness.with({ selector: '[data-campo="elemento"] mat-select' }));
    await elemento.open();
    const elementos = await Promise.all((await elemento.getOptions()).map(async (o) => (await o.getText()).trim()));
    expect(elementos).toEqual(['Sangue', 'Morte', 'Conhecimento', 'Energia', 'Medo']);
    await elemento.clickOptions({ text: 'Medo' });
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarRitual).toHaveBeenCalledWith(7, expect.objectContaining({ nome: 'Terror', circulo: 4, elemento: 'MEDO' }));
  });

  it('editar traz os valores, salva com atualizarRitual e emite a lista com o ritual trocado', async () => {
    const trocado = { ...CIRCULO_2, nome: 'Rasgar o Véu', alvo: null };
    const atualizarRitual = vi.fn(() => of(trocado));
    const { fixture, el, emitidas } = montar(fichaCom([CIRCULO_1, CIRCULO_2]), { atualizarRitual });
    await clicar(fixture, el.querySelector('button[aria-label="Editar Rasgar a Realidade"]'));
    expect(el.querySelector<HTMLInputElement>('input[formControlName="nome"]')!.value).toBe('Rasgar a Realidade');
    expect(el.querySelector<HTMLInputElement>('input[formControlName="resistencia"]')!.value).toBe('Vontade parcial');
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const elemento = await loader.getHarness(MatSelectHarness.with({ selector: '[data-campo="elemento"] mat-select' }));
    expect(await elemento.getValueText()).toBe('Morte');
    digitar(el, 'nome', 'Rasgar o Véu');
    digitar(el, 'alvo', '');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(atualizarRitual).toHaveBeenCalledWith(7, 4, expect.objectContaining({
      nome: 'Rasgar o Véu', circulo: 2, elemento: 'MORTE', alvo: null, execucao: 'Padrão',
    }));
    expect(emitidas).toEqual([[CIRCULO_1, trocado]]);
  });

  it('editar e cancelar nao chama a API', async () => {
    const api = { atualizarRitual: vi.fn(), criarRitual: vi.fn(), excluirRitual: vi.fn() };
    const { fixture, el, emitidas } = montar(fichaCom([CIRCULO_2]), api);
    await clicar(fixture, el.querySelector('button[aria-label="Editar Rasgar a Realidade"]'));
    digitar(el, 'nome', 'Outro');
    await clicar(fixture, botao(el, 'Cancelar'));

    expect(api.atualizarRitual).not.toHaveBeenCalled();
    expect(emitidas).toEqual([]);
    expect(el.textContent).not.toContain('Outro');
  });

  it('remover chama excluirRitual so depois de Sim, remover e emite a lista sem ele', async () => {
    const excluirRitual = vi.fn(() => of(undefined));
    const { fixture, el, emitidas } = montar(fichaCom([CIRCULO_1, CIRCULO_2]), { excluirRitual });
    await clicar(fixture, el.querySelector('button[aria-label="Remover Rasgar a Realidade"]'));
    expect(excluirRitual).not.toHaveBeenCalled();
    await clicar(fixture, botao(el, 'Sim, remover'));

    expect(excluirRitual).toHaveBeenCalledWith(7, 4);
    expect(emitidas).toEqual([[CIRCULO_1]]);
  });

  it('nome vazio bloqueia o envio', async () => {
    const criarRitual = vi.fn();
    const { fixture, el } = montar(fichaCom([]), { criarRitual });
    await clicar(fixture, botao(el, 'Adicionar ritual'));
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarRitual).not.toHaveBeenCalled();
    expect(el.querySelector('app-lista-editavel form')).not.toBeNull();
  });

  it('erro do servidor no campo nome aparece no campo', async () => {
    const erro: ApiError = { status: 400, titulo: 'Dados inválidos', detalhe: 'Corrija.', campos: { nome: 'nome é obrigatório' } };
    const { fixture, el, emitidas } = montar(fichaCom([]), { criarRitual: vi.fn(() => throwError(() => erro)) });
    await clicar(fixture, botao(el, 'Adicionar ritual'));
    digitar(el, 'nome', 'Terror');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(emitidas).toEqual([]);
    expect(el.querySelector('[data-campo="nome"]')?.textContent).toContain('nome é obrigatório');
  });

  it('clique duplo em salvar chama a API uma vez so', async () => {
    const criarRitual = vi.fn(() => new Subject<RitualOrdem>());
    const { fixture, el } = montar(fichaCom([]), { criarRitual });
    await clicar(fixture, botao(el, 'Adicionar ritual'));
    digitar(el, 'nome', 'Terror');
    const salvar = botao(el, 'Salvar');
    salvar.click();
    salvar.click();
    fixture.detectChanges();

    expect(criarRitual).toHaveBeenCalledTimes(1);
  });
});
