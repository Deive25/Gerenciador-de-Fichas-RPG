import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { FichaApi } from '../ficha-api';
import { Ficha, FichaDnd, FichaOrdem, Habilidade } from '../ficha.model';
import { HabilidadesAba } from './habilidades-aba';
import { espacosZerados } from '../testes/espacos-zerados';

const EXISTENTE: Habilidade = { id: 3, nome: 'Ataque especial', tipo: 'CLASSE', descricao: 'Gasta 2 PE.\nCausa +1d6.' };

const ORDEM: FichaOrdem = {
  id: 7, sistema: 'ORDEM_PARANORMAL', jogador: 'Arthur', personagem: 'Detetive Lia', genero: 'F', idade: null,
  retratoVersao: null, classe: 'OCULTISTA', origem: null, nex: 10,
  atributos: { AGI: 3, FOR: 1, INT: 4, PRE: 2, VIG: 0 },
  recursos: { PV: { atual: 9, maximo: 12 }, SAN: { atual: 48, maximo: 60 }, PE: { atual: 5, maximo: 10 } },
  pericias: {},
  descricao: { aparencia: null, historia: null, anotacoes: null },
  habilidades: [EXISTENTE],
  itens: [],
  rituais: [],
};

const DND: FichaDnd = {
  id: 9, sistema: 'DND', jogador: 'Arthur', personagem: 'Kael', genero: 'F', idade: null, retratoVersao: null,
  edicao: 'D2014', classe: 'Ladino', especie: 'Halfling', antecedente: null, nivel: 5,
  atributos: { FOR: 8, DES: 14, CON: 12, INT: 10, SAB: 9, CAR: 16 },
  recursos: { PV: { atual: 30, maximo: 38 }, PV_TEMP: { atual: 0, maximo: null } },
  ca: 15, inspiracao: false, salvaguardas: [], pericias: {},
  descricao: { aparencia: null, historia: null, anotacoes: null },
  habilidades: [],
  itens: [],
  moedas: { pc: 0, pp: 0, pe: 0, po: 0, pl: 0 },
  atributoConjuracao: null,
  magias: [],
  espacos: espacosZerados(),
};

function montar(ficha: Ficha, api: Partial<Record<keyof FichaApi, unknown>> = {}) {
  TestBed.configureTestingModule({ imports: [HabilidadesAba], providers: [{ provide: FichaApi, useValue: api }] });
  const fixture = TestBed.createComponent(HabilidadesAba);
  fixture.componentRef.setInput('ficha', ficha);
  fixture.detectChanges();
  const emitidas: Habilidade[][] = [];
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

async function rotulosDoSelect(fixture: ComponentFixture<unknown>): Promise<string[]> {
  const select = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);
  await select.open();
  return Promise.all((await select.getOptions()).map((o) => o.getText()));
}

async function escolherTipo(fixture: ComponentFixture<unknown>, rotulo: string): Promise<void> {
  const select = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);
  await select.open();
  await select.clickOptions({ text: rotulo });
}

function preencher(el: HTMLElement, nome: string, descricao = ''): void {
  const campo = el.querySelector<HTMLInputElement>('input[formControlName="nome"]')!;
  campo.value = nome;
  campo.dispatchEvent(new Event('input'));
  const area = el.querySelector<HTMLTextAreaElement>('textarea')!;
  area.value = descricao;
  area.dispatchEvent(new Event('input'));
}

describe('HabilidadesAba', () => {
  it('mostra o nome, o selo do tipo e a descricao de cada habilidade', () => {
    const { el } = montar(ORDEM);
    expect(el.textContent).toContain('Ataque especial');
    expect(el.querySelector('.selo')?.textContent?.trim()).toBe('Classe');
    expect(el.querySelector('.descricao')?.textContent).toContain('Gasta 2 PE.\nCausa +1d6.');
  });

  it('sem habilidades mostra Nenhuma habilidade ainda.', () => {
    const { el } = montar(DND);
    expect(el.textContent).toContain('Nenhuma habilidade ainda.');
  });

  it('ficha de Ordem: o select tem os 5 tipos de Ordem', async () => {
    const { fixture, el } = montar(ORDEM);
    await clicar(fixture, botao(el, 'Adicionar habilidade'));
    expect(await rotulosDoSelect(fixture)).toEqual(['Classe', 'Trilha', 'Origem', 'Poder paranormal', 'Outra']);
  });

  it('ficha de D&D 2014: tem os 6 tipos e ESPECIE aparece como Raca', async () => {
    const { fixture, el } = montar(DND);
    await clicar(fixture, botao(el, 'Adicionar habilidade'));
    expect(await rotulosDoSelect(fixture)).toEqual(['Classe', 'Subclasse', 'Raça', 'Antecedente', 'Talento', 'Outra']);
  });

  it('ficha de D&D 2024: ESPECIE aparece como Especie', async () => {
    const { fixture, el } = montar({ ...DND, edicao: 'D2024' });
    await clicar(fixture, botao(el, 'Adicionar habilidade'));
    const textos = await rotulosDoSelect(fixture);
    expect(textos).toContain('Espécie');
    expect(textos).not.toContain('Raça');
  });

  it('adicionar chama criarHabilidade e emite a lista com o item novo no fim', async () => {
    const nova: Habilidade = { id: 4, nome: 'Ataque especial', tipo: 'CLASSE', descricao: null };
    const criarHabilidade = vi.fn(() => of(nova));
    const { fixture, el, emitidas } = montar(ORDEM, { criarHabilidade });
    await clicar(fixture, botao(el, 'Adicionar habilidade'));
    preencher(el, 'Ataque especial');
    await escolherTipo(fixture, 'Classe');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarHabilidade).toHaveBeenCalledWith(7, { nome: 'Ataque especial', tipo: 'CLASSE', descricao: null });
    expect(emitidas).toEqual([[EXISTENTE, nova]]);
    expect(el.querySelector('form')).toBeNull();
  });

  it('editar e cancelar nao chama a API nem muda a lista', async () => {
    const api = { atualizarHabilidade: vi.fn(), criarHabilidade: vi.fn(), excluirHabilidade: vi.fn() };
    const { fixture, el, emitidas } = montar(ORDEM, api);
    await clicar(fixture, el.querySelector('button[aria-label="Editar Ataque especial"]'));
    preencher(el, 'Outro nome');
    await clicar(fixture, botao(el, 'Cancelar'));

    expect(api.atualizarHabilidade).not.toHaveBeenCalled();
    expect(emitidas).toEqual([]);
    expect(el.querySelector('form')).toBeNull();
    expect(el.textContent).toContain('Ataque especial');
    expect(el.textContent).not.toContain('Outro nome');
  });

  it('editar e salvar chama atualizarHabilidade e emite a lista com o item trocado', async () => {
    const trocada: Habilidade = { ...EXISTENTE, nome: 'Ataque feroz' };
    const atualizarHabilidade = vi.fn(() => of(trocada));
    const { fixture, el, emitidas } = montar(ORDEM, { atualizarHabilidade });
    await clicar(fixture, el.querySelector('button[aria-label="Editar Ataque especial"]'));
    expect(el.querySelector<HTMLInputElement>('input[formControlName="nome"]')!.value).toBe('Ataque especial');
    preencher(el, 'Ataque feroz', EXISTENTE.descricao!);
    await clicar(fixture, botao(el, 'Salvar'));

    expect(atualizarHabilidade).toHaveBeenCalledWith(7, 3, {
      nome: 'Ataque feroz',
      tipo: 'CLASSE',
      descricao: EXISTENTE.descricao,
    });
    expect(emitidas).toEqual([[trocada]]);
  });

  it('remover chama excluirHabilidade so depois de Sim, remover', async () => {
    const excluirHabilidade = vi.fn(() => of(undefined));
    const { fixture, el, emitidas } = montar(ORDEM, { excluirHabilidade });
    await clicar(fixture, el.querySelector('button[aria-label="Remover Ataque especial"]'));
    expect(excluirHabilidade).not.toHaveBeenCalled();
    await clicar(fixture, botao(el, 'Sim, remover'));

    expect(excluirHabilidade).toHaveBeenCalledWith(7, 3);
    expect(emitidas).toEqual([[]]);
  });

  it('erro do servidor no tipo aparece no campo tipo', async () => {
    const erro: ApiError = { status: 400, titulo: 'Dados inválidos', detalhe: 'Corrija.', campos: { tipo: 'não existe em D&D' } };
    const { fixture, el, emitidas } = montar(DND, { criarHabilidade: vi.fn(() => throwError(() => erro)) });
    await clicar(fixture, botao(el, 'Adicionar habilidade'));
    preencher(el, 'Golpe');
    await escolherTipo(fixture, 'Classe');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(emitidas).toEqual([]);
    expect(el.querySelector('[data-campo="tipo"]')?.textContent).toContain('não existe em D&D');
    expect(el.querySelector('form')).not.toBeNull();
  });

  it('clique duplo em salvar chama a API uma vez so', async () => {
    const envio = new Subject<Habilidade>();
    const criarHabilidade = vi.fn(() => envio);
    const { fixture, el } = montar(ORDEM, { criarHabilidade });
    await clicar(fixture, botao(el, 'Adicionar habilidade'));
    preencher(el, 'Golpe');
    await escolherTipo(fixture, 'Classe');
    const salvar = botao(el, 'Salvar');
    salvar.click();
    salvar.click();
    fixture.detectChanges();

    expect(criarHabilidade).toHaveBeenCalledTimes(1);
  });

  it('nome vazio bloqueia o envio', async () => {
    const criarHabilidade = vi.fn();
    const { fixture, el } = montar(ORDEM, { criarHabilidade });
    await clicar(fixture, botao(el, 'Adicionar habilidade'));
    await escolherTipo(fixture, 'Classe');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarHabilidade).not.toHaveBeenCalled();
    expect(el.textContent).toContain('Obrigatório');
  });

  it('descricao com 4001 caracteres mostra Maximo de 4000 caracteres e nao envia', async () => {
    const criarHabilidade = vi.fn();
    const { fixture, el } = montar(ORDEM, { criarHabilidade });
    await clicar(fixture, botao(el, 'Adicionar habilidade'));
    preencher(el, 'Golpe', 'a'.repeat(4001));
    await escolherTipo(fixture, 'Classe');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarHabilidade).not.toHaveBeenCalled();
    expect(el.querySelector('[data-campo="descricao"]')?.textContent).toContain('Máximo de 4000 caracteres');
  });
});
