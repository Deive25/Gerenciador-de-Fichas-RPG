import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/api-error';
import { FichaApi } from '../../ficha-api';
import { FichaOrdem, ItemOrdem } from '../../ficha.model';
import { InventarioOrdemAba } from './inventario-ordem-aba';

const EXISTENTE: ItemOrdem = { id: 3, nome: 'Pistola', categoria: 2, espacos: 1, quantidade: 3, descricao: 'Calibre 9mm.\nCarregada.' };

function fichaCom(itens: ItemOrdem[], forca = 2): FichaOrdem {
  return {
    id: 7, sistema: 'ORDEM_PARANORMAL', jogador: 'Arthur', personagem: 'Detetive Lia', genero: 'F', idade: null,
    retratoVersao: null, classe: 'OCULTISTA', origem: null, nex: 10,
    atributos: { AGI: 3, FOR: forca, INT: 4, PRE: 2, VIG: 0 },
    recursos: { PV: { atual: 9, maximo: 12 }, SAN: { atual: 48, maximo: 60 }, PE: { atual: 5, maximo: 10 } },
    pericias: {},
    descricao: { aparencia: null, historia: null, anotacoes: null },
    habilidades: [],
    itens,
    rituais: [],
  };
}

function item(id: number, espacos: number, quantidade: number): ItemOrdem {
  return { id, nome: `Item ${id}`, categoria: 0, espacos, quantidade, descricao: null };
}

function montar(ficha: FichaOrdem, api: Partial<Record<keyof FichaApi, unknown>> = {}) {
  TestBed.configureTestingModule({ imports: [InventarioOrdemAba], providers: [{ provide: FichaApi, useValue: api }] });
  const fixture = TestBed.createComponent(InventarioOrdemAba);
  fixture.componentRef.setInput('ficha', ficha);
  fixture.detectChanges();
  const emitidas: ItemOrdem[][] = [];
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
  const entrada = el.querySelector<HTMLInputElement>(`input[formControlName="${campo}"]`)!;
  entrada.value = valor;
  entrada.dispatchEvent(new Event('input'));
}

async function escolherCategoria(fixture: ComponentFixture<unknown>, rotulo: string): Promise<void> {
  const select = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);
  await select.open();
  await select.clickOptions({ text: rotulo });
}

describe('InventarioOrdemAba', () => {
  describe('carga', () => {
    it('FOR 2 e nenhum item: Carga 0/10 espacos, sem aviso, e o paragrafo existe com a lista vazia', () => {
      const { el } = montar(fichaCom([]));
      const carga = el.querySelector('p.carga')!;
      expect(carga.getAttribute('role')).toBe('status');
      expect(carga.textContent?.trim()).toBe('Carga: 0/10 espaços');
      expect(carga.classList.contains('acima')).toBe(false);
      expect(el.textContent).toContain('Nenhum item ainda.');
    });

    it('FOR 0: o limite e 2', () => {
      const { el } = montar(fichaCom([], 0));
      expect(el.querySelector('p.carga')?.textContent?.trim()).toBe('Carga: 0/2 espaços');
    });

    it('somando exatamente o limite nao ha aviso', () => {
      const { el } = montar(fichaCom([item(1, 2, 3), item(2, 1, 4)]));
      const carga = el.querySelector('p.carga')!;
      expect(carga.textContent?.trim()).toBe('Carga: 10/10 espaços');
      expect(carga.classList.contains('acima')).toBe(false);
    });

    it('uma unidade acima do limite mostra Acima do limite. e a classe acima', () => {
      const { el } = montar(fichaCom([item(1, 2, 3), item(2, 1, 5)]));
      const carga = el.querySelector('p.carga')!;
      expect(carga.textContent?.trim()).toBe('Carga: 11/10 espaços Acima do limite.');
      expect(carga.classList.contains('acima')).toBe(true);
    });
  });

  it('o cartao mostra nome, Categoria II, a conta 3 x 1 espaco e a descricao', () => {
    const { el } = montar(fichaCom([EXISTENTE]));
    expect(el.querySelector('.nome')?.textContent?.trim()).toBe('Pistola');
    expect(el.querySelector('.selo')?.textContent?.trim()).toBe('Categoria II');
    expect(el.querySelector('.conta')?.textContent?.trim()).toBe('3 × 1 espaço');
    expect(el.querySelector('.descricao')?.textContent).toContain('Calibre 9mm.\nCarregada.');
  });

  it('adicionar chama criarItem com categoria 0 e espacos 0 e emite a lista com o item no fim', async () => {
    const nova: ItemOrdem = { id: 4, nome: 'Lanterna', categoria: 0, espacos: 0, quantidade: 1, descricao: null };
    const criarItem = vi.fn(() => of(nova));
    const { fixture, el, emitidas } = montar(fichaCom([EXISTENTE]), { criarItem });
    await clicar(fixture, botao(el, 'Adicionar item'));
    digitar(el, 'nome', 'Lanterna');
    digitar(el, 'espacos', '0');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarItem).toHaveBeenCalledWith(7, { nome: 'Lanterna', categoria: 0, espacos: 0, quantidade: 1, descricao: null });
    expect(emitidas).toEqual([[EXISTENTE, nova]]);
    expect(el.querySelector('form')).toBeNull();
  });

  it('o formulario novo comeca em categoria 0, 1 espaco e quantidade 1', async () => {
    const { fixture, el } = montar(fichaCom([]));
    await clicar(fixture, botao(el, 'Adicionar item'));
    expect(el.querySelector('mat-select')?.textContent?.trim()).toBe('0');
    expect(el.querySelector<HTMLInputElement>('input[formControlName="espacos"]')!.value).toBe('1');
    expect(el.querySelector<HTMLInputElement>('input[formControlName="quantidade"]')!.value).toBe('1');
  });

  it('o select de categoria tem 0, I, II, III e IV', async () => {
    const { fixture, el } = montar(fichaCom([]));
    await clicar(fixture, botao(el, 'Adicionar item'));
    const select = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);
    await select.open();
    expect(await Promise.all((await select.getOptions()).map((o) => o.getText()))).toEqual(['0', 'I', 'II', 'III', 'IV']);
  });

  it('editar e salvar chama atualizarItem e emite a lista com o item trocado', async () => {
    const trocado: ItemOrdem = { ...EXISTENTE, nome: 'Pistola pesada', categoria: 3 };
    const atualizarItem = vi.fn(() => of(trocado));
    const { fixture, el, emitidas } = montar(fichaCom([EXISTENTE]), { atualizarItem });
    await clicar(fixture, el.querySelector('button[aria-label="Editar Pistola"]'));
    expect(el.querySelector<HTMLInputElement>('input[formControlName="nome"]')!.value).toBe('Pistola');
    expect(el.querySelector<HTMLInputElement>('input[formControlName="quantidade"]')!.value).toBe('3');
    digitar(el, 'nome', 'Pistola pesada');
    await escolherCategoria(fixture, 'III');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(atualizarItem).toHaveBeenCalledWith(7, 3, {
      nome: 'Pistola pesada', categoria: 3, espacos: 1, quantidade: 3, descricao: EXISTENTE.descricao,
    });
    expect(emitidas).toEqual([[trocado]]);
  });

  it('editar e cancelar nao chama a API', async () => {
    const api = { atualizarItem: vi.fn(), criarItem: vi.fn(), excluirItem: vi.fn() };
    const { fixture, el, emitidas } = montar(fichaCom([EXISTENTE]), api);
    await clicar(fixture, el.querySelector('button[aria-label="Editar Pistola"]'));
    digitar(el, 'nome', 'Outro');
    await clicar(fixture, botao(el, 'Cancelar'));

    expect(api.atualizarItem).not.toHaveBeenCalled();
    expect(emitidas).toEqual([]);
    expect(el.querySelector('form')).toBeNull();
    expect(el.textContent).not.toContain('Outro');
  });

  it('remover chama excluirItem so depois de Sim, remover', async () => {
    const excluirItem = vi.fn(() => of(undefined));
    const { fixture, el, emitidas } = montar(fichaCom([EXISTENTE]), { excluirItem });
    await clicar(fixture, el.querySelector('button[aria-label="Remover Pistola"]'));
    expect(excluirItem).not.toHaveBeenCalled();
    await clicar(fixture, botao(el, 'Sim, remover'));

    expect(excluirItem).toHaveBeenCalledWith(7, 3);
    expect(emitidas).toEqual([[]]);
  });

  it.each([
    ['quantidade 0', 'quantidade', '0'],
    ['espacos -1', 'espacos', '-1'],
    ['nome vazio', 'nome', ''],
  ])('%s bloqueia o envio', async (_rotulo, campo, valor) => {
    const criarItem = vi.fn();
    const { fixture, el } = montar(fichaCom([]), { criarItem });
    await clicar(fixture, botao(el, 'Adicionar item'));
    digitar(el, 'nome', 'Lanterna');
    digitar(el, campo, valor);
    await clicar(fixture, botao(el, 'Salvar'));

    expect(criarItem).not.toHaveBeenCalled();
    expect(el.querySelector('form')).not.toBeNull();
  });

  it('erro do servidor no campo espacos aparece no campo', async () => {
    const erro: ApiError = { status: 400, titulo: 'Dados inválidos', detalhe: 'Corrija.', campos: { espacos: 'valor inválido' } };
    const { fixture, el, emitidas } = montar(fichaCom([]), { criarItem: vi.fn(() => throwError(() => erro)) });
    await clicar(fixture, botao(el, 'Adicionar item'));
    digitar(el, 'nome', 'Lanterna');
    await clicar(fixture, botao(el, 'Salvar'));

    expect(emitidas).toEqual([]);
    expect(el.querySelector('[data-campo="espacos"]')?.textContent).toContain('valor inválido');
  });

  it('clique duplo em salvar chama a API uma vez so', async () => {
    const envio = new Subject<ItemOrdem>();
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
});
