import { TestBed } from '@angular/core/testing';
import { FichaOrdem, PedidoTeste } from '../../ficha.model';
import { FichaOrdemCorpo } from './ficha-ordem';

const FICHA: FichaOrdem = {
  id: 7, sistema: 'ORDEM_PARANORMAL', jogador: 'Arthur', personagem: 'Detetive Lia', genero: 'F', idade: 34,
  retratoVersao: null, classe: 'OCULTISTA', origem: 'Acadêmica', nex: 10,
  atributos: { AGI: 3, FOR: 1, INT: 4, PRE: 2, VIG: 0 },
  recursos: {
    PV: { atual: 9, maximo: 12 },
    SAN: { atual: 48, maximo: 60 },
    PE: { atual: 5, maximo: 10 },
  },
  pericias: { INVESTIGACAO: 'TREINADO', OCULTISMO: 'VETERANO', VONTADE: 'EXPERT' },
  descricao: { aparencia: null, historia: null, anotacoes: null },
  habilidades: [],
  itens: [],
  rituais: [],
};

function montar(ficha: FichaOrdem = FICHA, rolando = false) {
  const fixture = TestBed.createComponent(FichaOrdemCorpo);
  fixture.componentRef.setInput('ficha', ficha);
  fixture.componentRef.setInput('rolando', rolando);
  fixture.detectChanges();
  return { fixture, el: fixture.nativeElement as HTMLElement };
}

describe('FichaOrdemCorpo', () => {
  it('mostra classe, NEX, origem e Defesa (10 + AGI)', () => {
    const { el } = montar();
    const bloco = el.querySelector('.identidade') as HTMLElement;
    expect(bloco.textContent).toContain('Ocultista');
    expect(bloco.textContent).toContain('10%');
    expect(bloco.textContent).toContain('Acadêmica');
    expect(bloco.textContent).toContain('Defesa');
    expect(bloco.textContent).toContain('13');
  });

  it('ficha convertida: classe nula vira A definir e origem vazia nao quebra', () => {
    const { el } = montar({ ...FICHA, classe: null, origem: null });
    expect((el.querySelector('.identidade') as HTMLElement).textContent).toContain('A definir');
  });

  it('os 5 atributos tem valor e botao de rolar com nome acessivel', () => {
    const { el } = montar();
    for (const nome of [
      'Rolar Agilidade, 3', 'Rolar Força, 1', 'Rolar Intelecto, 4', 'Rolar Presença, 2', 'Rolar Vigor, 0',
    ]) {
      expect(el.querySelector(`button[aria-label="${nome}"]`)).not.toBeNull();
    }
  });

  it('mostra as 28 pericias com selo do grau, bonus e botao de rolar', () => {
    const { el } = montar();
    const rolarPericias = el.querySelectorAll('.lista-pericias button[aria-label^="Rolar "]');
    expect(rolarPericias.length).toBe(28);
    expect(el.querySelectorAll('button[aria-label^="Rolar "]').length).toBe(33);

    const item = (nome: string) => el.querySelector(`button[aria-label^="Rolar ${nome}, +"]`)?.closest('.pericia') as HTMLElement;
    expect(item('Investigação').querySelector('.selo')?.textContent?.trim()).toBe('T');
    expect(item('Investigação').querySelector('.valor')?.textContent?.trim()).toBe('+5');
    expect(item('Ocultismo').querySelector('.selo')?.textContent?.trim()).toBe('V');
    expect(item('Ocultismo').querySelector('.valor')?.textContent?.trim()).toBe('+10');
    expect(item('Vontade').querySelector('.selo')?.textContent?.trim()).toBe('E');
    expect(item('Vontade').querySelector('.valor')?.textContent?.trim()).toBe('+15');
    expect(el.querySelector('button[aria-label="Rolar Investigação, +5"]')).not.toBeNull();
    expect(el.querySelector('button[aria-label="Rolar Vontade, +15"]')).not.toBeNull();
  });

  it('pericia destreinada fica esmaecida, sem selo, com +0, e continua rolavel', () => {
    const { el } = montar();
    const rolar = el.querySelector('button[aria-label="Rolar Acrobacia, +0"]') as HTMLButtonElement;
    const item = rolar.closest('.pericia') as HTMLElement;
    expect(rolar.disabled).toBe(false);
    expect(item.classList).toContain('sem-valor');
    expect(item.querySelector('.selo')).toBeNull();
    expect(item.querySelector('.valor')?.textContent?.trim()).toBe('+0');
    expect(el.querySelector('button[aria-label="Rolar Investigação, +5"]')?.closest('.pericia')?.classList).not.toContain(
      'sem-valor',
    );
  });

  // Revisão final do 2.1b: com opacity .5 o texto ficava em 4,29:1, abaixo do AA (4,5:1).
  // "Esmaecida" passa a ser a cor de texto suave (5,13:1), sem opacidade.
  it('pericia destreinada fica esmaecida sem perder o contraste AA (sem opacidade)', () => {
    const { el } = montar();
    const item = el.querySelector('button[aria-label="Rolar Acrobacia, +0"]')?.closest('.pericia') as HTMLElement;
    expect(getComputedStyle(item.querySelector('.nome') as HTMLElement).opacity).not.toBe('0.5');
    expect(getComputedStyle(item.querySelector('.valor') as HTMLElement).opacity).not.toBe('0.5');
  });

  it('as dicas ficam em botoes pequenos, nunca no nome do item', () => {
    const { el } = montar();
    const gatilhos = Array.from(el.querySelectorAll('.mat-mdc-tooltip-trigger'));
    // 5 atributos + 28 perícias.
    expect(gatilhos.length).toBe(33);
    for (const gatilho of gatilhos) {
      expect(gatilho.tagName).toBe('BUTTON');
      expect(gatilho.getAttribute('aria-label')).toMatch(/^Sobre /);
    }
  });

  it('rolar emite o pedido de atributo e o de pericia', () => {
    const { fixture, el } = montar();
    const pedidos: PedidoTeste[] = [];
    fixture.componentInstance.rolar.subscribe((p) => pedidos.push(p));

    (el.querySelector('button[aria-label="Rolar Agilidade, 3"]') as HTMLButtonElement).click();
    (el.querySelector('button[aria-label="Rolar Investigação, +5"]') as HTMLButtonElement).click();

    expect(pedidos).toEqual([
      { alvo: 'ATRIBUTO', codigo: 'AGI' },
      { alvo: 'PERICIA', codigo: 'INVESTIGACAO' },
    ]);
  });

  it('com rolando verdadeiro, todos os botoes de rolar ficam desabilitados', () => {
    const { el } = montar(FICHA, true);
    const botoes = Array.from(el.querySelectorAll<HTMLButtonElement>('button[aria-label^="Rolar "]'));
    expect(botoes.length).toBe(33);
    expect(botoes.every((b) => b.disabled)).toBe(true);
  });
});
