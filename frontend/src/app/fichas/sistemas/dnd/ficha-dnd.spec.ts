import { TestBed } from '@angular/core/testing';
import { FichaDnd, PedidoTeste } from '../../ficha.model';
import { FichaDndCorpo } from './ficha-dnd';
import { espacosZerados } from '../../testes/espacos-zerados';

const FICHA: FichaDnd = {
  id: 9, sistema: 'DND', jogador: 'Arthur', personagem: 'Kael', genero: 'M', idade: 27, retratoVersao: null,
  edicao: 'D2024', classe: 'Ladino', especie: 'Halfling', antecedente: 'Criminoso', nivel: 5,
  atributos: { FOR: 8, DES: 14, CON: 12, INT: 10, SAB: 9, CAR: 16 },
  recursos: { PV: { atual: 30, maximo: 38 }, PV_TEMP: { atual: 0, maximo: null } },
  ca: 15, inspiracao: true, salvaguardas: ['DES', 'INT'],
  pericias: { FURTIVIDADE: 'ESPECIALISTA', PERCEPCAO: 'PROFICIENTE' },
  descricao: { aparencia: null, historia: null, anotacoes: null },
  habilidades: [],
  itens: [],
  moedas: { pc: 0, pp: 0, pe: 0, po: 0, pl: 0 },
  atributoConjuracao: null,
  magias: [],
  espacos: espacosZerados(),
};

function montar(ficha: FichaDnd = FICHA, rolando = false) {
  const fixture = TestBed.createComponent(FichaDndCorpo);
  fixture.componentRef.setInput('ficha', ficha);
  fixture.componentRef.setInput('rolando', rolando);
  fixture.detectChanges();
  const pedidos: PedidoTeste[] = [];
  fixture.componentInstance.rolar.subscribe((p) => pedidos.push(p));
  return { fixture, el: fixture.nativeElement as HTMLElement, pedidos };
}

function modo(el: HTMLElement, nome: string): HTMLButtonElement {
  return Array.from(el.querySelectorAll<HTMLButtonElement>('.modo button')).find((b) => b.textContent?.trim() === nome)!;
}

describe('FichaDndCorpo', () => {
  it('tem 6 atributos, 6 salvaguardas e 18 pericias rolaveis, com nome acessivel', () => {
    const { el } = montar();
    const rolar = Array.from(el.querySelectorAll('button[aria-label^="Rolar "]')).map((b) => b.getAttribute('aria-label'));
    expect(rolar.length).toBe(30);
    expect(rolar.filter((n) => n!.startsWith('Rolar salvaguarda de ')).length).toBe(6);
    // DES 14 → +2; nível 5 → proficiência +3; especialista: +2 + 6 = +8.
    expect(rolar).toContain('Rolar Furtividade, +8');
    expect(rolar).toContain('Rolar Percepção, +2'); // SAB 9 → −1, +3 = +2
    expect(rolar).toContain('Rolar Atletismo, −1'); // FOR 8 → −1
    expect(rolar).toContain('Rolar Força, −1');
    expect(rolar).toContain('Rolar Destreza, +2');
    expect(rolar).toContain('Rolar salvaguarda de Destreza, +5');
    expect(rolar).toContain('Rolar salvaguarda de Força, −1');
  });

  it('tem 24 dicas, todas botoes "Sobre ..."', () => {
    const { el } = montar();
    const gatilhos = Array.from(el.querySelectorAll('.mat-mdc-tooltip-trigger'));
    expect(gatilhos.length).toBe(24);
    for (const gatilho of gatilhos) {
      expect(gatilho.tagName).toBe('BUTTON');
      expect(gatilho.getAttribute('aria-label')).toMatch(/^Sobre /);
    }
  });

  it('mostra o selo P ou E nas pericias com grau e nada nas outras', () => {
    const { el } = montar();
    const item = (nome: string) => el.querySelector(`button[aria-label^="Rolar ${nome}, "]`)?.closest('li') as HTMLElement;
    expect(item('Furtividade').querySelector('.selo')?.textContent?.trim()).toBe('E');
    expect(item('Percepção').querySelector('.selo')?.textContent?.trim()).toBe('P');
    expect(item('Acrobacia').querySelector('.selo')).toBeNull();
  });

  it('marca a proficiencia nas salvaguardas', () => {
    const { el } = montar();
    const item = (nome: string) =>
      el.querySelector(`button[aria-label^="Rolar salvaguarda de ${nome}, "]`)?.closest('li') as HTMLElement;
    expect(item('Destreza').querySelector('.selo')).not.toBeNull();
    expect(item('Inteligência').querySelector('.selo')).not.toBeNull();
    expect(item('Força').querySelector('.selo')).toBeNull();
  });

  it('o bloco de identidade mostra edicao, classe, especie, antecedente, nivel, proficiencia, CA e inspiracao', () => {
    const { el } = montar();
    const texto = (el.querySelector('.identidade') as HTMLElement).textContent!;
    for (const parte of ['D&D 2024', 'Ladino', 'Espécie', 'Halfling', 'Criminoso', '5', '+3', '15', 'Sim']) {
      expect(texto).toContain(parte);
    }
  });

  it('classe nula vira traco e sem inspiracao vira Nao', () => {
    const { el } = montar({ ...FICHA, classe: null, inspiracao: false, ca: null });
    const texto = (el.querySelector('.identidade') as HTMLElement).textContent!;
    expect(texto).toContain('—');
    expect(texto).toContain('Não');
  });

  it('o rotulo da especie e Raca em 2014 e Especie em 2024', () => {
    const em2014 = (montar({ ...FICHA, edicao: 'D2014' }).el.querySelector('.identidade') as HTMLElement).textContent;
    expect(em2014).toContain('Raça');
    expect(em2014).not.toContain('Espécie');
    TestBed.resetTestingModule();
    const em2024 = (montar().el.querySelector('.identidade') as HTMLElement).textContent;
    expect(em2024).toContain('Espécie');
    expect(em2024).not.toContain('Raça');
  });

  it('o seletor de modo tem 3 botoes com aria-pressed e Normal e o padrao', () => {
    const { el } = montar();
    const botoes = Array.from(el.querySelectorAll<HTMLButtonElement>('.modo button'));
    expect(botoes.map((b) => b.textContent?.trim())).toEqual(['Normal', 'Vantagem', 'Desvantagem']);
    expect(botoes.every((b) => b.type === 'button')).toBe(true);
    expect(botoes.map((b) => b.getAttribute('aria-pressed'))).toEqual(['true', 'false', 'false']);
    expect(el.querySelector('[role="group"]')?.getAttribute('aria-labelledby')).toBeTruthy();
  });

  it('com Normal, o pedido nao leva modo', () => {
    const { el, pedidos } = montar();
    (el.querySelector('button[aria-label="Rolar Furtividade, +8"]') as HTMLButtonElement).click();
    expect(pedidos).toEqual([{ alvo: 'PERICIA', codigo: 'FURTIVIDADE' }]);
    expect('modo' in pedidos[0]).toBe(false);
  });

  it('com Vantagem pressionada, o pedido leva o modo e o seletor volta a Normal', () => {
    const { fixture, el, pedidos } = montar();
    modo(el, 'Vantagem').click();
    fixture.detectChanges();
    expect(modo(el, 'Vantagem').getAttribute('aria-pressed')).toBe('true');
    expect(modo(el, 'Normal').getAttribute('aria-pressed')).toBe('false');

    (el.querySelector('button[aria-label="Rolar Furtividade, +8"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(pedidos).toEqual([{ alvo: 'PERICIA', codigo: 'FURTIVIDADE', modo: 'VANTAGEM' }]);
    expect(modo(el, 'Normal').getAttribute('aria-pressed')).toBe('true');
    expect(modo(el, 'Vantagem').getAttribute('aria-pressed')).toBe('false');
  });

  it('desvantagem, atributo e salvaguarda emitem o alvo certo', () => {
    const { fixture, el, pedidos } = montar();
    modo(el, 'Desvantagem').click();
    fixture.detectChanges();
    (el.querySelector('button[aria-label="Rolar salvaguarda de Destreza, +5"]') as HTMLButtonElement).click();
    (el.querySelector('button[aria-label="Rolar Carisma, +3"]') as HTMLButtonElement).click();

    expect(pedidos).toEqual([
      { alvo: 'SALVAGUARDA', codigo: 'DES', modo: 'DESVANTAGEM' },
      { alvo: 'ATRIBUTO', codigo: 'CAR' },
    ]);
  });

  it('com rolando verdadeiro, todos os botoes de rolar ficam desabilitados', () => {
    const { el } = montar(FICHA, true);
    const botoes = Array.from(el.querySelectorAll<HTMLButtonElement>('button[aria-label^="Rolar "]'));
    expect(botoes.length).toBe(30);
    expect(botoes.every((b) => b.disabled)).toBe(true);
  });
});
