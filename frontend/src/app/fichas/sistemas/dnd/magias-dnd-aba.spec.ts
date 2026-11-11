import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/api-error';
import { FichaApi } from '../../ficha-api';
import { AtributoConjuracao, Espacos, FichaDnd, MagiaDnd } from '../../ficha.model';
import { espacosZerados } from '../../testes/espacos-zerados';
import { MagiasDndAba } from './magias-dnd-aba';

function magia(parcial: Partial<MagiaDnd> & { id: number }): MagiaDnd {
  return {
    nome: `Magia ${parcial.id}`, nivel: 1, escola: null, tempo: null, alcance: null, componentes: null, duracao: null,
    concentracao: false, ritual: false, preparada: false, descricao: null, ...parcial,
  };
}

const TRUQUE = magia({ id: 3, nome: 'Raio de Fogo', nivel: 0, escola: 'Evocação' });
const BOLA = magia({
  id: 4, nome: 'Bola de Fogo', nivel: 3, escola: 'Evocação', tempo: '1 ação', alcance: '45 m', componentes: 'V, S, M',
  duracao: 'Instantânea', concentracao: true, ritual: true, preparada: true, descricao: 'Explode.\nQueima.',
});

/** Nível 5 e INT 16: proficiência 3 e modificador +3, então CD 14 e ataque +6. */
function fichaCom(magias: MagiaDnd[], atributoConjuracao: AtributoConjuracao | null = 'INT'): FichaDnd {
  return {
    id: 7, sistema: 'DND', jogador: 'Arthur', personagem: 'Kael', genero: 'M', idade: 27, retratoVersao: null,
    edicao: 'D2024', classe: 'Mago', especie: 'Elfo', antecedente: 'Sábio', nivel: 5,
    atributos: { FOR: 10, DES: 14, CON: 12, INT: 16, SAB: 9, CAR: 10 },
    recursos: { PV: { atual: 30, maximo: 38 }, PV_TEMP: { atual: 4, maximo: null } },
    ca: 15, inspiracao: true, salvaguardas: ['INT'],
    pericias: {},
    descricao: { aparencia: null, historia: null, anotacoes: null },
    habilidades: [],
    itens: [],
    moedas: { pc: 0, pp: 0, pe: 0, po: 0, pl: 0 },
    atributoConjuracao,
    magias,
    espacos: espacosZerados(),
  };
}

function montar(ficha: FichaDnd, api: Partial<Record<keyof FichaApi, unknown>> = {}) {
  TestBed.configureTestingModule({ imports: [MagiasDndAba], providers: [{ provide: FichaApi, useValue: api }] });
  const fixture = TestBed.createComponent(MagiasDndAba);
  fixture.componentRef.setInput('ficha', ficha);
  fixture.detectChanges();
  const emitidas: MagiaDnd[][] = [];
  const espacos: Espacos[] = [];
  fixture.componentInstance.atualizada.subscribe((l) => emitidas.push(l));
  fixture.componentInstance.espacosAtualizados.subscribe((e) => espacos.push(e));
  return { fixture, el: fixture.nativeElement as HTMLElement, emitidas, espacos };
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

/** Campos do formulário da magia (o dos espaços não leva formControlName com esses nomes). */
function digitar(el: HTMLElement, campo: string, valor: string): void {
  const entrada = el.querySelector<HTMLInputElement | HTMLTextAreaElement>(
    `app-lista-editavel input[formControlName="${campo}"], app-lista-editavel textarea[formControlName="${campo}"]`,
  )!;
  entrada.value = valor;
  entrada.dispatchEvent(new Event('input'));
}

function conta(el: HTMLElement, qual: 'cd' | 'ataque'): string {
  return el.querySelector(`[data-conta="${qual}"] .valor`)!.textContent!.trim();
}

describe('MagiasDndAba', () => {
  describe('CD e ataque magico', () => {
    it('com INT 16 no nivel 5 mostra o atributo, CD 14 e ataque +6, sem a dica', () => {
      const { el } = montar(fichaCom([]));
      expect(el.querySelector('.atributo')?.textContent?.trim()).toBe('Atributo de conjuração: Inteligência');
      expect(conta(el, 'cd')).toBe('14');
      expect(conta(el, 'ataque')).toBe('+6');
      expect(el.querySelector('.dica')).toBeNull();
    });

    it('com Sabedoria 9 (modificador −1) a CD e o ataque descem', () => {
      const { el } = montar(fichaCom([], 'SAB'));
      expect(conta(el, 'cd')).toBe('10');
      expect(conta(el, 'ataque')).toBe('+2');
    });

    it('sem atributo de conjuracao mostra traco nos dois e a dica', () => {
      const { el } = montar(fichaCom([], null));
      expect(conta(el, 'cd')).toBe('—');
      expect(conta(el, 'ataque')).toBe('—');
      expect(el.querySelector('.atributo')?.textContent?.trim()).toBe('Atributo de conjuração: —');
      expect(el.querySelector('.dica')?.textContent?.trim()).toBe('Escolha o atributo de conjuração em Editar ficha.');
    });
  });

  it('a lista vazia diz Nenhuma magia ainda.', () => {
    const { el } = montar(fichaCom([]));
    expect(el.textContent).toContain('Nenhuma magia ainda.');
  });

  it('o cartao mostra "Truque · Evocação" e "3º nível · Evocação", os detalhes e os selos so do que vale', () => {
    const { el } = montar(fichaCom([TRUQUE, BOLA]));
    const cartoes = Array.from(el.querySelectorAll('app-lista-editavel .cartao'));
    expect(cartoes.length).toBe(2);
    expect(cartoes[0].querySelector('.conta')?.textContent?.trim()).toBe('Truque · Evocação');
    expect(cartoes[0].querySelectorAll('.selo').length).toBe(0);
    expect(cartoes[0].querySelector('.detalhes')).toBeNull();
    expect(cartoes[1].querySelector('.conta')?.textContent?.trim()).toBe('3º nível · Evocação');
    expect(Array.from(cartoes[1].querySelectorAll('.selo')).map((s) => s.textContent?.trim())).toEqual([
      'Concentração', 'Ritual', 'Preparada',
    ]);
    expect(cartoes[1].querySelector('.detalhes')?.textContent?.trim()).toBe(
      'Tempo: 1 ação · Alcance: 45 m · Componentes: V, S, M · Duração: Instantânea',
    );
    expect(cartoes[1].querySelector('.descricao')?.textContent).toContain('Explode.\nQueima.');
  });

  it('sem escola o cartao mostra so o nivel', () => {
    const { el } = montar(fichaCom([magia({ id: 9, nivel: 2 })]));
    expect(el.querySelector('app-lista-editavel .conta')?.textContent?.trim()).toBe('2º nível');
  });

  it('adicionar chama criarMagia com os campos e emite a lista com a magia no lugar certo (por nivel)', async () => {
    const nova = magia({ id: 5, nome: 'Mísseis Mágicos', nivel: 1, escola: 'Evocação', preparada: true });
    const criarMagia = vi.fn(() => of(nova));
    const { fixture, el, emitidas } = montar(fichaCom([TRUQUE, BOLA]), { criarMagia });
    await clicar(fixture, botao(el, 'Adicionar magia'));
    digitar(el, 'nome', '  Mísseis Mágicos ');
    digitar(el, 'escola', 'Evocação');
    await clicar(fixture, el.querySelector('mat-checkbox[data-campo="preparada"] input'));
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarMagia).toHaveBeenCalledWith(7, {
      nome: 'Mísseis Mágicos', nivel: 1, escola: 'Evocação', tempo: null, alcance: null, componentes: null, duracao: null,
      concentracao: false, ritual: false, preparada: true, descricao: null,
    });
    expect(emitidas).toEqual([[TRUQUE, nova, BOLA]]);
    expect(el.querySelector('app-lista-editavel form')).toBeNull();
  });

  it('o formulario novo comeca em 1º nível e o select oferece Truque e os nove niveis', async () => {
    const { fixture, el } = montar(fichaCom([]));
    await clicar(fixture, botao(el, 'Adicionar magia'));
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const select = await loader.getHarness(MatSelectHarness.with({ selector: '[data-campo="nivel"] mat-select' }));
    expect(await select.getValueText()).toBe('1º nível');
    await select.open();
    const nomes = await Promise.all((await select.getOptions()).map(async (o) => (await o.getText()).trim()));
    expect(nomes).toEqual(['Truque', '1º nível', '2º nível', '3º nível', '4º nível', '5º nível', '6º nível', '7º nível', '8º nível', '9º nível']);
    await select.close();
  });

  it('escolher Truque envia nivel 0', async () => {
    const criarMagia = vi.fn(() => of(magia({ id: 6, nivel: 0 })));
    const { fixture, el } = montar(fichaCom([]), { criarMagia });
    await clicar(fixture, botao(el, 'Adicionar magia'));
    digitar(el, 'nome', 'Prestidigitação');
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const select = await loader.getHarness(MatSelectHarness.with({ selector: '[data-campo="nivel"] mat-select' }));
    await select.clickOptions({ text: 'Truque' });
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarMagia).toHaveBeenCalledWith(7, expect.objectContaining({ nome: 'Prestidigitação', nivel: 0 }));
  });

  it('editar traz os valores, salva com atualizarMagia e emite a lista reordenada', async () => {
    // A Bola de Fogo vira truque, como o Raio de Fogo: no mesmo nível vale o id.
    const trocada = { ...BOLA, nivel: 0 };
    const atualizarMagia = vi.fn(() => of(trocada));
    const { fixture, el, emitidas } = montar(fichaCom([TRUQUE, BOLA]), { atualizarMagia });
    await clicar(fixture, el.querySelector('button[aria-label="Editar Bola de Fogo"]'));
    expect(el.querySelector<HTMLInputElement>('input[formControlName="nome"]')!.value).toBe('Bola de Fogo');
    expect(el.querySelector<HTMLInputElement>('input[formControlName="componentes"]')!.value).toBe('V, S, M');
    expect(el.querySelector<HTMLInputElement>('mat-checkbox[data-campo="concentracao"] input')!.checked).toBe(true);
    expect(el.querySelector<HTMLTextAreaElement>('textarea[formControlName="descricao"]')!.value).toBe('Explode.\nQueima.');
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const select = await loader.getHarness(MatSelectHarness.with({ selector: '[data-campo="nivel"] mat-select' }));
    expect(await select.getValueText()).toBe('3º nível');
    await select.clickOptions({ text: 'Truque' });
    await clicar(fixture, botao(el, 'Salvar'));

    expect(atualizarMagia).toHaveBeenCalledWith(7, 4, expect.objectContaining({ nome: 'Bola de Fogo', nivel: 0, concentracao: true }));
    // Os dois são truques agora: o id desempata (3 antes de 4).
    expect(emitidas).toEqual([[TRUQUE, trocada]]);
  });

  it('editar e cancelar nao chama a API', async () => {
    const api = { atualizarMagia: vi.fn(), criarMagia: vi.fn(), excluirMagia: vi.fn() };
    const { fixture, el, emitidas } = montar(fichaCom([BOLA]), api);
    await clicar(fixture, el.querySelector('button[aria-label="Editar Bola de Fogo"]'));
    digitar(el, 'nome', 'Outra');
    await clicar(fixture, botao(el, 'Cancelar'));

    expect(api.atualizarMagia).not.toHaveBeenCalled();
    expect(emitidas).toEqual([]);
    expect(el.textContent).not.toContain('Outra');
  });

  it('remover chama excluirMagia so depois de Sim, remover e emite a lista sem ela', async () => {
    const excluirMagia = vi.fn(() => of(undefined));
    const { fixture, el, emitidas } = montar(fichaCom([TRUQUE, BOLA]), { excluirMagia });
    await clicar(fixture, el.querySelector('button[aria-label="Remover Bola de Fogo"]'));
    expect(excluirMagia).not.toHaveBeenCalled();
    await clicar(fixture, botao(el, 'Sim, remover'));

    expect(excluirMagia).toHaveBeenCalledWith(7, 4);
    expect(emitidas).toEqual([[TRUQUE]]);
  });

  it('nome vazio bloqueia o envio', async () => {
    const criarMagia = vi.fn();
    const { fixture, el } = montar(fichaCom([]), { criarMagia });
    await clicar(fixture, botao(el, 'Adicionar magia'));
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarMagia).not.toHaveBeenCalled();
    expect(el.querySelector('app-lista-editavel form')).not.toBeNull();
  });

  it('erro do servidor no campo nome aparece no campo', async () => {
    const erro: ApiError = { status: 400, titulo: 'Dados inválidos', detalhe: 'Corrija.', campos: { nome: 'nome é obrigatório' } };
    const { fixture, el, emitidas } = montar(fichaCom([]), { criarMagia: vi.fn(() => throwError(() => erro)) });
    await clicar(fixture, botao(el, 'Adicionar magia'));
    digitar(el, 'nome', 'Bola');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(emitidas).toEqual([]);
    expect(el.querySelector('[data-campo="nome"]')?.textContent).toContain('nome é obrigatório');
  });

  it('clique duplo em salvar chama a API uma vez so', async () => {
    const criarMagia = vi.fn(() => new Subject<MagiaDnd>());
    const { fixture, el } = montar(fichaCom([]), { criarMagia });
    await clicar(fixture, botao(el, 'Adicionar magia'));
    digitar(el, 'nome', 'Bola');
    const salvar = botao(el, 'Salvar');
    salvar.click();
    salvar.click();
    fixture.detectChanges();

    expect(criarMagia).toHaveBeenCalledTimes(1);
  });

  it('os espacos gravados pelo bloco de espacos saem em espacosAtualizados', async () => {
    const resposta: Espacos = { ...espacosZerados(), '1': { total: 2, usados: 0 } };
    const { fixture, el, espacos } = montar(fichaCom([]), { atualizarEspacos: vi.fn(() => of(resposta)) });
    const campo = el.querySelector<HTMLInputElement>('[data-campo="total-1"] input')!;
    campo.value = '2';
    campo.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await clicar(fixture, botao(el, 'Salvar totais'));

    expect(espacos).toEqual([resposta]);
  });
});
