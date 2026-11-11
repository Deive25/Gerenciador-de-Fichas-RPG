import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/api-error';
import { FichaApi } from '../../ficha-api';
import { Espacos, FichaDnd } from '../../ficha.model';
import { espacosZerados } from '../../testes/espacos-zerados';
import { EspacosDnd } from './espacos-dnd';

function espacosCom(parcial: Partial<Espacos>): Espacos {
  return { ...espacosZerados(), ...parcial };
}

/** Nível 1 com 4 espaços (1 usado) e nível 3 com 3 espaços (todos usados); os outros sem espaço. */
const ESPACOS = espacosCom({ '1': { total: 4, usados: 1 }, '3': { total: 3, usados: 3 } });

function fichaCom(espacos: Espacos): FichaDnd {
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
    atributoConjuracao: 'INT',
    magias: [],
    espacos,
  };
}

function montar(api: Partial<Record<keyof FichaApi, unknown>> = {}, espacos: Espacos = ESPACOS) {
  TestBed.configureTestingModule({ imports: [EspacosDnd], providers: [{ provide: FichaApi, useValue: api }] });
  const fixture = TestBed.createComponent(EspacosDnd);
  const ficha = fichaCom(espacos);
  fixture.componentRef.setInput('ficha', ficha);
  fixture.detectChanges();
  const emitidos: Espacos[] = [];
  // Faz o papel do detalhe: o que a aba avisa vai para a ficha, e a aba redesenha a partir dela.
  fixture.componentInstance.atualizados.subscribe((e) => {
    emitidos.push(e);
    fixture.componentRef.setInput('ficha', { ...ficha, espacos: e });
    fixture.detectChanges();
  });
  return { fixture, el: fixture.nativeElement as HTMLElement, emitidos };
}

async function clicar(fixture: ComponentFixture<unknown>, b: HTMLElement | null | undefined): Promise<void> {
  b!.click();
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
}

function marcador(el: HTMLElement, rotulo: string): HTMLButtonElement {
  return el.querySelector<HTMLButtonElement>(`button.marcador[aria-label="${rotulo}"]`)!;
}

function botao(el: HTMLElement, texto: string): HTMLButtonElement {
  return Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.trim() === texto) as HTMLButtonElement;
}

function total(el: HTMLElement, nivel: string): HTMLInputElement {
  return el.querySelector<HTMLInputElement>(`[data-campo="total-${nivel}"] input`)!;
}

function digitar(fixture: ComponentFixture<unknown>, el: HTMLElement, nivel: string, valor: string): void {
  const entrada = total(el, nivel);
  entrada.value = valor;
  entrada.dispatchEvent(new Event('input'));
  fixture.detectChanges();
}

describe('EspacosDnd', () => {
  it('mostra marcadores so dos niveis com espaco, com o nome do espaco e se esta usado', () => {
    const { el } = montar();
    expect(Array.from(el.querySelectorAll('[data-nivel]')).map((n) => n.getAttribute('data-nivel'))).toEqual(['1', '3']);
    expect(Array.from(el.querySelectorAll('[data-nivel="1"] button')).map((b) => b.getAttribute('aria-label'))).toEqual([
      '1º nível, espaço 1 de 4, usado',
      '1º nível, espaço 2 de 4, disponível',
      '1º nível, espaço 3 de 4, disponível',
      '1º nível, espaço 4 de 4, disponível',
    ]);
    expect(el.querySelectorAll('[data-nivel="3"] button.usado').length).toBe(3);
    expect(el.querySelector('[data-nivel="1"] .contagem')?.textContent?.trim()).toBe('1/4');
  });

  it('sem nenhum espaco mostra o aviso e nenhum marcador', () => {
    const { el } = montar({}, espacosZerados());
    expect(el.querySelectorAll('button.marcador').length).toBe(0);
    expect(el.textContent).toContain('Nenhum espaço de magia ainda.');
  });

  it('o aviso de status existe vazio antes de qualquer gravacao', () => {
    const { el } = montar();
    const status = el.querySelector('[role="status"]');
    expect(status).not.toBeNull();
    expect(status!.textContent?.trim()).toBe('');
  });

  it('marcar um espaco vazio soma 1 e grava so aquele nivel', async () => {
    const resposta = espacosCom({ '1': { total: 4, usados: 2 }, '3': { total: 3, usados: 3 } });
    const atualizarEspacos = vi.fn(() => of(resposta));
    const { fixture, el, emitidos } = montar({ atualizarEspacos });
    await clicar(fixture, marcador(el, '1º nível, espaço 3 de 4, disponível'));

    expect(atualizarEspacos).toHaveBeenCalledWith(7, { '1': { total: 4, usados: 2 } });
    expect(emitidos).toEqual([resposta]);
    expect(el.querySelectorAll('[data-nivel="1"] button.usado').length).toBe(2);
    expect(el.querySelector('[role="status"]')?.textContent).toContain('1º nível: 2 de 4 espaços usados.');
  });

  it('desmarcar um espaco marcado subtrai 1', async () => {
    const resposta = espacosCom({ '1': { total: 4, usados: 1 }, '3': { total: 3, usados: 2 } });
    const atualizarEspacos = vi.fn(() => of(resposta));
    const { fixture, el, emitidos } = montar({ atualizarEspacos });
    await clicar(fixture, marcador(el, '3º nível, espaço 1 de 3, usado'));

    expect(atualizarEspacos).toHaveBeenCalledWith(7, { '3': { total: 3, usados: 2 } });
    expect(emitidos).toEqual([resposta]);
    expect(marcador(el, '3º nível, espaço 3 de 3, disponível')).not.toBeNull();
  });

  it('enquanto grava os marcadores ficam aria-disabled e um segundo clique nao chama a API', () => {
    const atualizarEspacos = vi.fn(() => new Subject<Espacos>());
    const { fixture, el } = montar({ atualizarEspacos });
    marcador(el, '1º nível, espaço 2 de 4, disponível').click();
    fixture.detectChanges();
    marcador(el, '1º nível, espaço 3 de 4, disponível').click();
    fixture.detectChanges();

    expect(atualizarEspacos).toHaveBeenCalledTimes(1);
    expect(Array.from(el.querySelectorAll('button.marcador')).every((b) => b.getAttribute('aria-disabled') === 'true')).toBe(true);
    expect(botao(el, 'Descanso longo').disabled).toBe(true);
    expect(botao(el, 'Salvar totais').disabled).toBe(true);
  });

  it('erro do servidor ao marcar mostra o alerta e nao muda nada', async () => {
    const erro: ApiError = { status: 0, titulo: 'Sem conexão', detalhe: 'Tente de novo.', campos: {} };
    const { fixture, el, emitidos } = montar({ atualizarEspacos: vi.fn(() => throwError(() => erro)) });
    await clicar(fixture, marcador(el, '1º nível, espaço 2 de 4, disponível'));

    expect(emitidos).toEqual([]);
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Sem conexão');
    expect(el.querySelectorAll('[data-nivel="1"] button.usado').length).toBe(1);
    expect(marcador(el, '1º nível, espaço 2 de 4, disponível').getAttribute('aria-disabled')).toBe('false');
  });

  describe('totais', () => {
    it('os campos comecam com os totais da ficha e o botao so habilita com mudanca valida', () => {
      const { fixture, el } = montar();
      expect(['1', '2', '3', '4'].map((n) => total(el, n).value)).toEqual(['4', '0', '3', '0']);
      expect(botao(el, 'Salvar totais').disabled).toBe(true);
      digitar(fixture, el, '2', '2');
      expect(botao(el, 'Salvar totais').disabled).toBe(false);
      digitar(fixture, el, '2', 'abc');
      expect(botao(el, 'Salvar totais').disabled).toBe(true);
      digitar(fixture, el, '2', '100');
      expect(botao(el, 'Salvar totais').disabled).toBe(true);
    });

    it('cada campo de total tem nome acessivel com o nivel', () => {
      const { el } = montar();
      expect(total(el, '5').getAttribute('aria-label')).toBe('Total de espaços de 5º nível');
    });

    it('aumentar o total manda so o nivel mudado, com os usados que ja tinha', async () => {
      const resposta = espacosCom({ '1': { total: 4, usados: 1 }, '2': { total: 2, usados: 0 }, '3': { total: 3, usados: 3 } });
      const atualizarEspacos = vi.fn(() => of(resposta));
      const { fixture, el, emitidos } = montar({ atualizarEspacos });
      digitar(fixture, el, '2', '2');
      await clicar(fixture, botao(el, 'Salvar totais'));

      expect(atualizarEspacos).toHaveBeenCalledWith(7, { '2': { total: 2, usados: 0 } });
      expect(emitidos).toEqual([resposta]);
      expect(el.querySelector('[role="status"]')?.textContent).toContain('Totais salvos.');
      expect(el.querySelectorAll('[data-nivel="2"] button.marcador').length).toBe(2);
      expect(botao(el, 'Salvar totais').disabled).toBe(true);
    });

    it('total abaixo dos usados manda usados igual ao total', async () => {
      const resposta = espacosCom({ '1': { total: 4, usados: 1 }, '3': { total: 2, usados: 2 } });
      const atualizarEspacos = vi.fn(() => of(resposta));
      const { fixture, el } = montar({ atualizarEspacos });
      digitar(fixture, el, '3', '2');
      await clicar(fixture, botao(el, 'Salvar totais'));

      expect(atualizarEspacos).toHaveBeenCalledWith(7, { '3': { total: 2, usados: 2 } });
    });

    it('total zero tira o nivel da lista de marcadores', async () => {
      const resposta = espacosCom({ '1': { total: 4, usados: 1 } });
      const { fixture, el } = montar({ atualizarEspacos: vi.fn(() => of(resposta)) });
      digitar(fixture, el, '3', '0');
      await clicar(fixture, botao(el, 'Salvar totais'));

      expect(el.querySelector('[data-nivel="3"]')).toBeNull();
    });

    it('enquanto grava os campos ficam desabilitados e voltam no sucesso', async () => {
      const envio = new Subject<Espacos>();
      const { fixture, el } = montar({ atualizarEspacos: vi.fn(() => envio) });
      digitar(fixture, el, '2', '2');
      botao(el, 'Salvar totais').click();
      fixture.detectChanges();
      expect(total(el, '2').disabled).toBe(true);

      envio.next(espacosCom({ '1': { total: 4, usados: 1 }, '2': { total: 2, usados: 0 }, '3': { total: 3, usados: 3 } }));
      envio.complete();
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
      expect(total(el, '2').disabled).toBe(false);
      expect(total(el, '2').value).toBe('2');
    });

    it('erro "3.usados" do servidor aparece no campo do nivel 3 e mantem o digitado', async () => {
      const erro: ApiError = { status: 400, titulo: 'Dados inválidos', detalhe: 'x', campos: { '3.usados': 'não pode passar do total' } };
      const { fixture, el, emitidos } = montar({ atualizarEspacos: vi.fn(() => throwError(() => erro)) });
      digitar(fixture, el, '3', '5');
      await clicar(fixture, botao(el, 'Salvar totais'));

      expect(el.querySelector('[data-campo="total-3"]')?.textContent).toContain('não pode passar do total');
      expect(total(el, '3').value).toBe('5');
      expect(total(el, '3').disabled).toBe(false);
      expect(emitidos).toEqual([]);
    });

    it('erro "3.total" tambem cai no campo do nivel 3', async () => {
      const erro: ApiError = { status: 400, titulo: 'Dados inválidos', detalhe: 'x', campos: { '3.total': 'deve ser 0 ou mais' } };
      const { fixture, el } = montar({ atualizarEspacos: vi.fn(() => throwError(() => erro)) });
      digitar(fixture, el, '3', '5');
      await clicar(fixture, botao(el, 'Salvar totais'));

      expect(el.querySelector('[data-campo="total-3"]')?.textContent).toContain('deve ser 0 ou mais');
    });

    it('erro sem campo conhecido (ex.: nivel 10) vira alerta geral', async () => {
      const erro: ApiError = { status: 400, titulo: 'Dados inválidos', detalhe: 'nível deve estar entre 1 e 9', campos: { '10': 'nível deve estar entre 1 e 9' } };
      const { fixture, el } = montar({ atualizarEspacos: vi.fn(() => throwError(() => erro)) });
      digitar(fixture, el, '3', '5');
      await clicar(fixture, botao(el, 'Salvar totais'));

      expect(el.querySelector('[role="alert"]')?.textContent).toContain('Dados inválidos');
    });
  });

  describe('Descanso longo', () => {
    it('zera os usados de todos os niveis com usados e so deles', async () => {
      const resposta = espacosCom({ '1': { total: 4, usados: 0 }, '3': { total: 3, usados: 0 } });
      const atualizarEspacos = vi.fn(() => of(resposta));
      const { fixture, el, emitidos } = montar({ atualizarEspacos });
      await clicar(fixture, botao(el, 'Descanso longo'));

      expect(atualizarEspacos).toHaveBeenCalledWith(7, { '1': { total: 4, usados: 0 }, '3': { total: 3, usados: 0 } });
      expect(emitidos).toEqual([resposta]);
      expect(el.querySelectorAll('button.marcador.usado').length).toBe(0);
      expect(el.querySelector('[role="status"]')?.textContent).toContain('Descanso longo');
    });

    it('sem nenhum espaco usado o botao fica desabilitado', () => {
      const { el } = montar({}, espacosCom({ '1': { total: 4, usados: 0 } }));
      expect(botao(el, 'Descanso longo').disabled).toBe(true);
    });

    it('erro do servidor mostra o alerta e mantem os usados', async () => {
      const erro: ApiError = { status: 0, titulo: 'Sem conexão', detalhe: 'Tente de novo.', campos: {} };
      const { fixture, el } = montar({ atualizarEspacos: vi.fn(() => throwError(() => erro)) });
      await clicar(fixture, botao(el, 'Descanso longo'));

      expect(el.querySelector('[role="alert"]')?.textContent).toContain('Sem conexão');
      expect(el.querySelectorAll('button.marcador.usado').length).toBe(4);
    });
  });
});
