import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/api-error';
import { FichaApi } from '../../ficha-api';
import { FichaDnd, ItemDnd, Moedas } from '../../ficha.model';
import { InventarioDndAba } from './inventario-dnd-aba';
import { espacosZerados } from '../../testes/espacos-zerados';

const EXISTENTE: ItemDnd = { id: 3, nome: 'Corda', quantidade: 2, peso: 1.5, equipado: false, descricao: 'Seda élfica.\nDez metros.' };

function fichaCom(itens: ItemDnd[], forca = 10): FichaDnd {
  return {
    id: 7, sistema: 'DND', jogador: 'Arthur', personagem: 'Kael', genero: 'M', idade: 27, retratoVersao: null,
    edicao: 'D2024', classe: 'Ladino', especie: 'Halfling', antecedente: 'Criminoso', nivel: 5,
    atributos: { FOR: forca, DES: 14, CON: 12, INT: 10, SAB: 9, CAR: 16 },
    recursos: { PV: { atual: 30, maximo: 38 }, PV_TEMP: { atual: 4, maximo: null } },
    ca: 15, inspiracao: true, salvaguardas: ['DES'],
    pericias: {},
    descricao: { aparencia: null, historia: null, anotacoes: null },
    habilidades: [],
    itens,
    moedas: { pc: 0, pp: 0, pe: 0, po: 0, pl: 0 },
    atributoConjuracao: null,
    magias: [],
    espacos: espacosZerados(),
  };
}

function item(id: number, peso: number, quantidade = 1, equipado = false): ItemDnd {
  return { id, nome: `Item ${id}`, quantidade, peso, equipado, descricao: null };
}

function montar(ficha: FichaDnd, api: Partial<Record<keyof FichaApi, unknown>> = {}) {
  TestBed.configureTestingModule({ imports: [InventarioDndAba], providers: [{ provide: FichaApi, useValue: api }] });
  const fixture = TestBed.createComponent(InventarioDndAba);
  fixture.componentRef.setInput('ficha', ficha);
  fixture.detectChanges();
  const emitidas: ItemDnd[][] = [];
  const moedas: Moedas[] = [];
  fixture.componentInstance.atualizada.subscribe((l) => emitidas.push(l));
  fixture.componentInstance.moedasAtualizadas.subscribe((m) => moedas.push(m));
  return { fixture, el: fixture.nativeElement as HTMLElement, emitidas, moedas };
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

/** `moeda` escolhe o campo das moedas (que não leva formControlName no DOM) em vez do formulário do item. */
function digitar(el: HTMLElement, campo: string, valor: string, moeda = false): void {
  const entrada = el.querySelector<HTMLInputElement>(moeda ? `[data-campo="${campo}"] input` : `input[formControlName="${campo}"]`)!;
  entrada.value = valor;
  entrada.dispatchEvent(new Event('input'));
}

describe('InventarioDndAba', () => {
  describe('peso', () => {
    it('FOR 10 e nenhum item: Peso 0/75 kg, sem aviso, e o paragrafo existe com a lista vazia', () => {
      const { el } = montar(fichaCom([]));
      const carga = el.querySelector('p.carga')!;
      expect(carga.getAttribute('role')).toBe('status');
      expect(carga.textContent?.trim()).toBe('Peso: 0/75 kg');
      expect(carga.classList.contains('acima')).toBe(false);
      expect(el.textContent).toContain('Nenhum item ainda.');
    });

    it('somando exatamente 75 nao ha aviso', () => {
      const { el } = montar(fichaCom([item(1, 25, 2), item(2, 25)]));
      const carga = el.querySelector('p.carga')!;
      expect(carga.textContent?.trim()).toBe('Peso: 75/75 kg');
      expect(carga.classList.contains('acima')).toBe(false);
    });

    it('somando 75,5 mostra Acima da capacidade. e a classe acima', () => {
      const { el } = montar(fichaCom([item(1, 25, 3), item(2, 0.5)]));
      const carga = el.querySelector('p.carga')!;
      expect(carga.textContent?.trim()).toBe('Peso: 75,5/75 kg Acima da capacidade.');
      expect(carga.classList.contains('acima')).toBe(true);
    });

    it('tres itens de 0,1 kg somam 0,3 sem ruido de ponto flutuante', () => {
      const { el } = montar(fichaCom([item(1, 0.1), item(2, 0.1), item(3, 0.1)]));
      expect(el.querySelector('p.carga')?.textContent?.trim()).toBe('Peso: 0,3/75 kg');
    });
  });

  it('o cartao mostra 2 x 1,5 kg e o selo Equipado so no item equipado', () => {
    const espada: ItemDnd = { id: 4, nome: 'Espada', quantidade: 1, peso: 3, equipado: true, descricao: null };
    const { el } = montar(fichaCom([EXISTENTE, espada]));
    const cartoes = Array.from(el.querySelectorAll('.cabeca'));
    expect(el.querySelector('.conta')?.textContent?.trim()).toBe('2 × 1,5 kg');
    expect(el.querySelector('.descricao')?.textContent).toContain('Seda élfica.\nDez metros.');
    expect(Array.from(el.querySelectorAll('.selo')).map((s) => s.textContent?.trim())).toEqual(['Equipado']);
    expect(cartoes.length).toBe(2);
    expect(cartoes[0].querySelector('.selo')).toBeNull();
    expect(cartoes[1].querySelector('.selo')).not.toBeNull();
  });

  it.each([
    ['virgula', '1,5'],
    ['ponto', '1.5'],
  ])('adicionar com o peso com %s chama criarItem com peso 1.5 e equipado false e emite a lista com o item no fim', async (_r, texto) => {
    const nova: ItemDnd = { id: 4, nome: 'Lanterna', quantidade: 1, peso: 1.5, equipado: false, descricao: null };
    const criarItem = vi.fn(() => of(nova));
    const { fixture, el, emitidas } = montar(fichaCom([EXISTENTE]), { criarItem });
    await clicar(fixture, botao(el, 'Adicionar item'));
    digitar(el, 'nome', 'Lanterna');
    digitar(el, 'peso', texto);
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarItem).toHaveBeenCalledWith(7, { nome: 'Lanterna', quantidade: 1, peso: 1.5, equipado: false, descricao: null });
    expect(emitidas).toEqual([[EXISTENTE, nova]]);
    expect(el.querySelector('app-lista-editavel form')).toBeNull();
  });

  it('o formulario novo comeca em quantidade 1 e peso 0', async () => {
    const { fixture, el } = montar(fichaCom([]));
    await clicar(fixture, botao(el, 'Adicionar item'));
    expect(el.querySelector<HTMLInputElement>('input[formControlName="quantidade"]')!.value).toBe('1');
    expect(el.querySelector<HTMLInputElement>('input[formControlName="peso"]')!.value).toBe('0');
    expect(el.querySelector<HTMLInputElement>('input[formControlName="peso"]')!.getAttribute('inputmode')).toBe('decimal');
  });

  it('adicionar com o peso 0 envia peso 0', async () => {
    const criarItem = vi.fn(() => of(item(5, 0)));
    const { fixture, el } = montar(fichaCom([]), { criarItem });
    await clicar(fixture, botao(el, 'Adicionar item'));
    digitar(el, 'nome', 'Pena');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarItem).toHaveBeenCalledWith(7, expect.objectContaining({ peso: 0 }));
  });

  it('marcar Equipado envia equipado true', async () => {
    const criarItem = vi.fn(() => of(item(5, 1, 1, true)));
    const { fixture, el } = montar(fichaCom([]), { criarItem });
    await clicar(fixture, botao(el, 'Adicionar item'));
    digitar(el, 'nome', 'Espada');
    await clicar(fixture, el.querySelector('mat-checkbox input'));
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarItem).toHaveBeenCalledWith(7, expect.objectContaining({ equipado: true }));
  });

  it('editar e salvar chama atualizarItem e emite a lista com o item trocado', async () => {
    const trocado: ItemDnd = { ...EXISTENTE, nome: 'Corda longa', peso: 2 };
    const atualizarItem = vi.fn(() => of(trocado));
    const { fixture, el, emitidas } = montar(fichaCom([EXISTENTE]), { atualizarItem });
    await clicar(fixture, el.querySelector('button[aria-label="Editar Corda"]'));
    expect(el.querySelector<HTMLInputElement>('input[formControlName="nome"]')!.value).toBe('Corda');
    expect(el.querySelector<HTMLInputElement>('input[formControlName="peso"]')!.value).toBe('1.5');
    digitar(el, 'nome', 'Corda longa');
    digitar(el, 'peso', '2');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(atualizarItem).toHaveBeenCalledWith(7, 3, {
      nome: 'Corda longa', quantidade: 2, peso: 2, equipado: false, descricao: EXISTENTE.descricao,
    });
    expect(emitidas).toEqual([[trocado]]);
  });

  it('editar e cancelar nao chama a API', async () => {
    const api = { atualizarItem: vi.fn(), criarItem: vi.fn(), excluirItem: vi.fn() };
    const { fixture, el, emitidas } = montar(fichaCom([EXISTENTE]), api);
    await clicar(fixture, el.querySelector('button[aria-label="Editar Corda"]'));
    digitar(el, 'nome', 'Outro');
    await clicar(fixture, botao(el, 'Cancelar'));

    expect(api.atualizarItem).not.toHaveBeenCalled();
    expect(emitidas).toEqual([]);
    expect(el.querySelector('app-lista-editavel form')).toBeNull();
    expect(el.textContent).not.toContain('Outro');
  });

  it('remover chama excluirItem so depois de Sim, remover', async () => {
    const excluirItem = vi.fn(() => of(undefined));
    const { fixture, el, emitidas } = montar(fichaCom([EXISTENTE]), { excluirItem });
    await clicar(fixture, el.querySelector('button[aria-label="Remover Corda"]'));
    expect(excluirItem).not.toHaveBeenCalled();
    await clicar(fixture, botao(el, 'Sim, remover'));

    expect(excluirItem).toHaveBeenCalledWith(7, 3);
    expect(emitidas).toEqual([[]]);
  });

  it.each([
    ['peso 1,555', 'peso', '1,555'],
    ['peso abc', 'peso', 'abc'],
    ['peso vazio', 'peso', ''],
    ['peso 1e3', 'peso', '1e3'],
    ['quantidade 0', 'quantidade', '0'],
    ['nome vazio', 'nome', ''],
  ])('%s bloqueia o envio', async (_rotulo, campo, valor) => {
    const criarItem = vi.fn();
    const { fixture, el } = montar(fichaCom([]), { criarItem });
    await clicar(fixture, botao(el, 'Adicionar item'));
    digitar(el, 'nome', 'Lanterna');
    digitar(el, campo, valor);
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarItem).not.toHaveBeenCalled();
    expect(el.querySelector('app-lista-editavel form')).not.toBeNull();
  });

  it('erro do servidor no campo peso aparece no campo', async () => {
    const erro: ApiError = { status: 400, titulo: 'Dados inválidos', detalhe: 'Corrija.', campos: { peso: 'valor inválido' } };
    const { fixture, el, emitidas } = montar(fichaCom([]), { criarItem: vi.fn(() => throwError(() => erro)) });
    await clicar(fixture, botao(el, 'Adicionar item'));
    digitar(el, 'nome', 'Lanterna');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(emitidas).toEqual([]);
    expect(el.querySelector('[data-campo="peso"]')?.textContent).toContain('valor inválido');
  });

  it('clique duplo em salvar chama a API uma vez so', async () => {
    const envio = new Subject<ItemDnd>();
    const criarItem = vi.fn(() => envio);
    const { fixture, el } = montar(fichaCom([]), { criarItem });
    await clicar(fixture, botao(el, 'Adicionar item'));
    digitar(el, 'nome', 'Lanterna');
    const salvar = botao(el, 'Salvar');
    salvar.click();
    salvar.click();
    fixture.detectChanges();

    expect(criarItem).toHaveBeenCalledTimes(1);
  });

  it('as moedas emitidas pelo app-moedas-dnd saem em moedasAtualizadas', async () => {
    const resposta: Moedas = { pc: 0, pp: 0, pe: 0, po: 10, pl: 0 };
    const { fixture, el, moedas } = montar(fichaCom([]), { atualizarMoedas: vi.fn(() => of(resposta)) });
    digitar(el, 'po', '10', true);
    fixture.detectChanges();
    await clicar(fixture, botao(el, 'Salvar moedas'));

    expect(moedas).toEqual([resposta]);
  });
});
