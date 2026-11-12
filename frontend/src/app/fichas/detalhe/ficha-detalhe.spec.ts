import { OverlayContainer } from '@angular/cdk/overlay';
import { Provider, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { RetratoCache } from '../retrato-cache';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { FichaApi } from '../ficha-api';
import { AtaqueResultado, FichaDnd, FichaOrdem, RolagemLivre, TesteResultado } from '../ficha.model';
import { FichaDetalhe } from './ficha-detalhe';
import { espacosZerados } from '../testes/espacos-zerados';
import { DadoAnimado } from '../dado/dado-animado';
import { d20sDoTeste } from '../dado/dados-lancados';
import { LancadorDeDados } from '../dado/lancador-de-dados';

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
  ataques: [],
};

const RESULTADO: TesteResultado = {
  alvo: 'PERICIA', codigo: 'INVESTIGACAO', nome: 'Investigação', dados: [4, 19, 9], mantido: 19, bonus: 5, total: 24,
};

const FICHA_DND: FichaDnd = {
  id: 9, sistema: 'DND', jogador: 'Arthur', personagem: 'Kael', genero: 'M', idade: 27, retratoVersao: null,
  edicao: 'D2024', classe: 'Ladino', especie: 'Halfling', antecedente: 'Criminoso', nivel: 5,
  atributos: { FOR: 8, DES: 14, CON: 12, INT: 10, SAB: 9, CAR: 16 },
  recursos: { PV: { atual: 30, maximo: 38 }, PV_TEMP: { atual: 4, maximo: null } },
  ca: 15, inspiracao: true, salvaguardas: ['DES'],
  pericias: { FURTIVIDADE: 'ESPECIALISTA' },
  descricao: { aparencia: null, historia: null, anotacoes: null },
  habilidades: [],
  itens: [],
  moedas: { pc: 0, pp: 0, pe: 0, po: 0, pl: 0 },
  atributoConjuracao: null,
  magias: [],
  espacos: espacosZerados(),
  ataques: [],
};

const RESULTADO_DND: TesteResultado = {
  alvo: 'PERICIA', codigo: 'FURTIVIDADE', nome: 'Furtividade', dados: [17, 6], mantido: 17, bonus: 8, total: 25,
};

function montar(api: Partial<Record<keyof FichaApi, unknown>>, extras: Provider[] = []) {
  TestBed.configureTestingModule({
    imports: [FichaDetalhe],
    providers: [
      provideRouter([]),
      { provide: FichaApi, useValue: { versaoFichas: signal(0), ...api } },
      // Por padrão, plano B (sem dado 3D): o cartão aparece direto, como antes do 2.1f.
      { provide: DadoAnimado, useValue: { preparar: () => Promise.resolve(null) } },
      ...extras,
    ],
  });
  const router = TestBed.inject(Router);
  const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);
  const fixture = TestBed.createComponent(FichaDetalhe);
  fixture.componentRef.setInput('id', '7');
  fixture.detectChanges();
  return { fixture, el: fixture.nativeElement as HTMLElement, navegar };
}

/** A rolagem agora passa por uma promessa (o preparar() do dado): espera ela e redesenha. */
async function aguardar(fixture: ComponentFixture<FichaDetalhe>): Promise<void> {
  await new Promise<void>((resolve) => setTimeout(resolve, 0));
  fixture.detectChanges();
}

function botao(el: HTMLElement, texto: string): HTMLButtonElement {
  return Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes(texto)) as HTMLButtonElement;
}

/** O menu ⋮ abre num overlay do CDK, fora do elemento do componente. */
function itensDoMenu(): HTMLElement[] {
  return Array.from(TestBed.inject(OverlayContainer).getContainerElement().querySelectorAll('[mat-menu-item]'));
}

function abrirMenu(fixture: ComponentFixture<FichaDetalhe>): void {
  (fixture.nativeElement.querySelector('button[aria-label="Mais ações"]') as HTMLButtonElement).click();
  fixture.detectChanges();
}

function pedirExclusao(fixture: ComponentFixture<FichaDetalhe>): void {
  abrirMenu(fixture);
  itensDoMenu()
    .find((item) => item.textContent?.includes('Excluir'))!
    .click();
  fixture.detectChanges();
}

describe('FichaDetalhe · retrato', () => {
  const JPEG = new Blob(['jpeg'], { type: 'image/jpeg' });

  function escolherArquivo(fixture: ComponentFixture<FichaDetalhe>): void {
    const input = fixture.nativeElement.querySelector('input[type="file"]') as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [new File(['x'], 'foto.jpg', { type: 'image/jpeg' })] });
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  it('escolher uma foto abre o recorte, envia o JPEG e recarrega a ficha', () => {
    const buscar = vi.fn(() => of(FICHA));
    const enviarRetrato = vi.fn(() => of(undefined));
    const abrir = vi.fn(() => ({ afterClosed: () => of(JPEG) }));
    const { fixture } = montar({ buscarFicha: buscar, enviarRetrato }, [{ provide: MatDialog, useValue: { open: abrir } }]);

    escolherArquivo(fixture);

    expect(abrir).toHaveBeenCalledTimes(1);
    expect(enviarRetrato).toHaveBeenCalledWith(7, JPEG);
    expect(buscar).toHaveBeenCalledTimes(2);
  });

  it('cancelar o recorte nao envia nada', () => {
    const enviarRetrato = vi.fn(() => of(undefined));
    const abrir = vi.fn(() => ({ afterClosed: () => of(undefined) }));
    const { fixture } = montar({ buscarFicha: vi.fn(() => of(FICHA)), enviarRetrato }, [
      { provide: MatDialog, useValue: { open: abrir } },
    ]);

    escolherArquivo(fixture);

    expect(enviarRetrato).not.toHaveBeenCalled();
  });

  it('Remover imagem so aparece com retrato e pede confirmacao', () => {
    const removerRetrato = vi.fn(() => of(undefined));
    const esquecer = vi.fn();
    const comRetrato = { ...FICHA, retratoVersao: 100 };
    const buscar = vi.fn(() => of(comRetrato));
    const { fixture, el } = montar({ buscarFicha: buscar, removerRetrato }, [
      { provide: RetratoCache, useValue: { url: () => of('blob:x'), esquecer } },
    ]);

    botao(el, 'Remover imagem').click();
    fixture.detectChanges();
    expect(removerRetrato).not.toHaveBeenCalled();
    botao(el, 'Sim, remover').click();
    fixture.detectChanges();

    expect(removerRetrato).toHaveBeenCalledWith(7);
    expect(esquecer).toHaveBeenCalledWith(7);
    expect(buscar).toHaveBeenCalledTimes(2);
  });

  it('sem retrato nao mostra Remover imagem', () => {
    const { el } = montar({ buscarFicha: vi.fn(() => of(FICHA)) });
    expect(botao(el, 'Remover imagem')).toBeUndefined();
    expect(botao(el, 'Trocar imagem')).toBeDefined();
  });

  it('erro ao enviar a foto aparece na tela', () => {
    const erro: ApiError = { status: 413, titulo: 'Imagem grande demais', detalhe: 'A imagem pode ter no máximo 200 KB.', campos: {} };
    const abrir = vi.fn(() => ({ afterClosed: () => of(JPEG) }));
    const { fixture, el } = montar(
      { buscarFicha: vi.fn(() => of(FICHA)), enviarRetrato: vi.fn(() => throwError(() => erro)) },
      [{ provide: MatDialog, useValue: { open: abrir } }],
    );

    escolherArquivo(fixture);

    expect(el.textContent).toContain('Imagem grande demais: A imagem pode ter no máximo 200 KB.');
  });
});

describe('FichaDetalhe', () => {
  it('mostra jogador, idade e genero', () => {
    const { el } = montar({ buscarFicha: vi.fn(() => of(FICHA)) });
    expect(el.textContent).toContain('Arthur · 34 anos · F');
  });

  it('desenha os tres aneis de recursos', () => {
    const { el } = montar({ buscarFicha: vi.fn(() => of(FICHA)) });
    expect(el.querySelectorAll('app-anel-status').length).toBe(3);
    expect(el.textContent).toContain('9/12');
    expect(el.textContent).toContain('5/10');
  });

  it('o link de voltar tem alvo de toque de 44 px', () => {
    const { el } = montar({ buscarFicha: vi.fn(() => of(FICHA)) });
    expect(getComputedStyle(el.querySelector('a.voltar') as HTMLElement).minHeight).toBe('44px');
  });

  it('o menu de acoes tem Editar (link) e Excluir', () => {
    const { fixture } = montar({ buscarFicha: vi.fn(() => of(FICHA)) });
    abrirMenu(fixture);
    const [editar, excluir] = itensDoMenu();
    expect(editar.textContent).toContain('Editar');
    expect(editar.getAttribute('href')).toBe('/fichas/7/editar');
    expect(excluir.textContent).toContain('Excluir');
  });

  it('rola a pericia e mostra o resultado com cada dado', async () => {
    const testar = vi.fn(() => of(RESULTADO));
    const { fixture, el } = montar({ buscarFicha: vi.fn(() => of(FICHA)), testar });

    (el.querySelector('button[aria-label="Rolar Investigação, +5"]') as HTMLButtonElement).click();
    await aguardar(fixture);

    expect(testar).toHaveBeenCalledWith(7, { alvo: 'PERICIA', codigo: 'INVESTIGACAO' });
    expect(el.textContent).toContain('Investigação: 19 + 5 = 24');
    expect(el.querySelectorAll('.dado').length).toBe(3);
    expect(el.querySelectorAll('.mantido').length).toBe(1);
  });

  it('rola o atributo', async () => {
    const resultado: TesteResultado = {
      alvo: 'ATRIBUTO', codigo: 'AGI', nome: 'Agilidade', dados: [8, 15, 2], mantido: 15, bonus: 0, total: 15,
    };
    const testar = vi.fn(() => of(resultado));
    const { fixture, el } = montar({ buscarFicha: vi.fn(() => of(FICHA)), testar });

    (el.querySelector('button[aria-label="Rolar Agilidade, 3"]') as HTMLButtonElement).click();
    await aguardar(fixture);

    expect(testar).toHaveBeenCalledWith(7, { alvo: 'ATRIBUTO', codigo: 'AGI' });
    expect(el.textContent).toContain('Agilidade: 15 = 15');
  });

  it('nao envia duas rolagens com toque duplo', async () => {
    const resposta = new Subject<TesteResultado>();
    const testar = vi.fn(() => resposta);
    const { fixture, el } = montar({ buscarFicha: vi.fn(() => of(FICHA)), testar });
    const rolar = el.querySelector('button[aria-label="Rolar Investigação, +5"]') as HTMLButtonElement;

    rolar.click();
    fixture.detectChanges();
    rolar.click();
    fixture.detectChanges();

    expect(testar).toHaveBeenCalledTimes(1);
    expect(rolar.disabled).toBe(true);
    // Enquanto rola, nenhum outro botão de rolar dispara outra requisição.
    expect((el.querySelector('button[aria-label="Rolar Agilidade, 3"]') as HTMLButtonElement).disabled).toBe(true);

    resposta.next(RESULTADO);
    resposta.complete();
    await aguardar(fixture);
    expect(rolar.disabled).toBe(false);
  });

  it('exclui apos confirmar', () => {
    const excluir = vi.fn(() => of(undefined));
    const { fixture, el, navegar } = montar({ buscarFicha: vi.fn(() => of(FICHA)), excluir });

    pedirExclusao(fixture);
    fixture.detectChanges();
    botao(el, 'Cancelar').click();
    fixture.detectChanges();
    expect(excluir).not.toHaveBeenCalled();

    pedirExclusao(fixture);
    fixture.detectChanges();
    botao(el, 'Sim, excluir').click();
    fixture.detectChanges();

    expect(excluir).toHaveBeenCalledWith(7);
    expect(navegar).toHaveBeenCalledWith(['/fichas']);
  });

  it('tentar de novo limpa o erro anterior quando a ficha carrega', () => {
    const indisponivel: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'Pode estar acordando.', campos: {} };
    const buscar = vi
      .fn()
      .mockReturnValueOnce(throwError(() => indisponivel))
      .mockReturnValueOnce(of(FICHA));
    const { fixture, el } = montar({ buscarFicha: buscar });
    expect(el.textContent).toContain('Servidor indisponível');

    botao(el, 'Tentar de novo').click();
    fixture.detectChanges();

    expect(el.textContent).toContain('Detetive Lia');
    expect(el.textContent).not.toContain('Servidor indisponível');
  });

  it('rolagem bem-sucedida limpa o erro da rolagem anterior', async () => {
    const indisponivel: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'Pode estar acordando.', campos: {} };
    const testar = vi
      .fn()
      .mockReturnValueOnce(throwError(() => indisponivel))
      .mockReturnValueOnce(of(RESULTADO));
    const { fixture, el } = montar({ buscarFicha: vi.fn(() => of(FICHA)), testar });
    const rolar = async () => {
      (el.querySelector('button[aria-label="Rolar Investigação, +5"]') as HTMLButtonElement).click();
      await aguardar(fixture);
    };

    await rolar();
    expect(el.textContent).toContain('Servidor indisponível');
    await rolar();

    expect(el.textContent).toContain('Investigação: 19 + 5 = 24');
    expect(el.textContent).not.toContain('Servidor indisponível');
  });

  it('sim excluir continua bloqueado ate a navegacao terminar', () => {
    const excluir = vi.fn(() => of(undefined));
    const { fixture, el, navegar } = montar({ buscarFicha: vi.fn(() => of(FICHA)), excluir });
    navegar.mockReturnValue(new Promise<boolean>(() => undefined));

    pedirExclusao(fixture);
    fixture.detectChanges();
    const confirmar = botao(el, 'Sim, excluir');
    confirmar.click();
    fixture.detectChanges();
    confirmar.click();
    fixture.detectChanges();

    expect(excluir).toHaveBeenCalledTimes(1);
    expect(confirmar.disabled).toBe(true);
  });

  it('ficha inexistente', () => {
    const erro: ApiError = { status: 404, titulo: 'Ficha não encontrada', detalhe: 'Não existe ficha com o id 7.', campos: {} };
    const { el } = montar({ buscarFicha: vi.fn(() => throwError(() => erro)) });
    expect(el.textContent).toContain('Ficha não encontrada');
    expect(el.querySelector('a[href="/fichas"]')?.textContent).toContain('Voltar para as fichas');
  });

  it('ficha de Ordem mostra o corpo de Ordem e nao o de D&D', () => {
    const { el } = montar({ buscarFicha: vi.fn(() => of(FICHA)) });
    expect(el.querySelector('app-ficha-ordem')).not.toBeNull();
    expect(el.querySelector('app-ficha-dnd')).toBeNull();
    expect(el.textContent).not.toContain('Sistema ainda sem tela');
  });
});

describe('FichaDetalhe · D&D', () => {
  const montarDnd = (api: Partial<Record<keyof FichaApi, unknown>> = {}) =>
    montar({ buscarFicha: vi.fn(() => of(FICHA_DND)), ...api });

  it('abre o corpo de D&D, com dois aneis (Vida e Temporario)', () => {
    const { el } = montarDnd();
    expect(el.querySelector('app-ficha-dnd')).not.toBeNull();
    expect(el.querySelector('app-ficha-ordem')).toBeNull();
    expect(el.querySelectorAll('app-anel-status').length).toBe(2);
    expect(el.textContent).toContain('Kael');
    expect(el.textContent).toContain('Arthur · 27 anos · M');
  });

  it('rolar Furtividade com Vantagem chama testar com o modo e mostra os 2 dados', async () => {
    const testar = vi.fn(() => of(RESULTADO_DND));
    const { fixture, el } = montarDnd({ testar });

    const botoesModo = Array.from(el.querySelectorAll<HTMLButtonElement>('.modo button'));
    botoesModo.find((b) => b.textContent?.trim() === 'Vantagem')!.click();
    fixture.detectChanges();
    (el.querySelector('button[aria-label="Rolar Furtividade, +8"]') as HTMLButtonElement).click();
    await aguardar(fixture);

    expect(testar).toHaveBeenCalledWith(9, { alvo: 'PERICIA', codigo: 'FURTIVIDADE', modo: 'VANTAGEM' });
    expect(el.textContent).toContain('Furtividade: 17 + 8 = 25');
    expect(el.querySelectorAll('.dado').length).toBe(2);
    expect(el.querySelectorAll('.mantido').length).toBe(1);
  });

  it('depois de rolar, o seletor volta a Normal', async () => {
    const { fixture, el } = montarDnd({ testar: vi.fn(() => of(RESULTADO_DND)) });
    const modo = (nome: string) =>
      Array.from(el.querySelectorAll<HTMLButtonElement>('.modo button')).find((b) => b.textContent?.trim() === nome)!;

    modo('Desvantagem').click();
    fixture.detectChanges();
    expect(modo('Desvantagem').getAttribute('aria-pressed')).toBe('true');
    (el.querySelector('button[aria-label="Rolar Furtividade, +8"]') as HTMLButtonElement).click();
    await aguardar(fixture);

    expect(modo('Normal').getAttribute('aria-pressed')).toBe('true');
    expect(modo('Desvantagem').getAttribute('aria-pressed')).toBe('false');
  });

  it('20 natural aparece no resultado de D&D, mas nao no de Ordem', async () => {
    const vinte: TesteResultado = { ...RESULTADO_DND, dados: [20], mantido: 20, total: 28 };
    const dnd = montarDnd({ testar: vi.fn(() => of(vinte)) });
    (dnd.el.querySelector('button[aria-label="Rolar Furtividade, +8"]') as HTMLButtonElement).click();
    await aguardar(dnd.fixture);
    expect(dnd.el.textContent).toContain('20 natural');

    TestBed.resetTestingModule();
    const ordem = montar({ buscarFicha: vi.fn(() => of(FICHA)), testar: vi.fn(() => of({ ...RESULTADO, mantido: 20, dados: [20] })) });
    (ordem.el.querySelector('button[aria-label="Rolar Investigação, +5"]') as HTMLButtonElement).click();
    await aguardar(ordem.fixture);
    expect(ordem.el.textContent).not.toContain('natural');
  });

  it('nao envia duas rolagens com toque duplo em D&D', () => {
    const resposta = new Subject<TesteResultado>();
    const testar = vi.fn(() => resposta);
    const { fixture, el } = montarDnd({ testar });
    const rolar = el.querySelector('button[aria-label="Rolar Furtividade, +8"]') as HTMLButtonElement;

    rolar.click();
    fixture.detectChanges();
    rolar.click();
    fixture.detectChanges();

    expect(testar).toHaveBeenCalledTimes(1);
    expect((el.querySelector('button[aria-label="Rolar Carisma, +3"]') as HTMLButtonElement).disabled).toBe(true);
  });
});

describe('FichaDetalhe · abas', () => {
  const rotulos = (el: HTMLElement) => Array.from(el.querySelectorAll('[role=tab]')).map((t) => t.textContent?.trim());
  const selecionada = (el: HTMLElement) => el.querySelector('[role=tab][aria-selected=true]')?.textContent?.trim();

  it.each([
    ['Ordem', FICHA, ['Ficha', 'Ataques', 'Dados', 'Inventário', 'Rituais', 'Habilidades', 'Descrição']],
    ['D&D', FICHA_DND, ['Ficha', 'Ataques', 'Dados', 'Inventário', 'Magias', 'Habilidades', 'Descrição']],
  ])('ficha de %s tem as abas na ordem certa', (_nome, ficha, esperadas) => {
    const { el } = montar({ buscarFicha: vi.fn(() => of(ficha)) });
    expect(rotulos(el)).toEqual(esperadas);
  });

  it('na aba Inventario de Ordem mostra os itens e um item novo aparece e muda a carga sem recarregar', async () => {
    const existente = { id: 3, nome: 'Pistola', categoria: 2, espacos: 1, quantidade: 3, descricao: null };
    const nova = { id: 4, nome: 'Lanterna', categoria: 0, espacos: 1, quantidade: 2, descricao: null };
    const buscarFicha = vi.fn(() => of({ ...FICHA, itens: [existente] }));
    const { fixture, el } = montar({ buscarFicha });
    fixture.componentRef.setInput('aba', 'inventario');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(selecionada(el)).toBe('Inventário');
    expect(el.textContent).toContain('Pistola');
    expect(el.querySelector('p.carga')?.textContent?.trim()).toBe('Carga: 3/5 espaços');

    const aba = fixture.debugElement.query((d) => d.name === 'app-inventario-ordem-aba').componentInstance as {
      atualizada: { emit(lista: unknown[]): void };
    };
    aba.atualizada.emit([existente, nova]);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(el.textContent).toContain('Lanterna');
    expect(el.querySelector('p.carga')?.textContent?.trim()).toBe('Carga: 5/5 espaços');
    expect(buscarFicha).toHaveBeenCalledTimes(1);
  });

  it('na aba Inventario de D&D mostra os itens e as moedas, e um item novo muda o peso total sem recarregar', async () => {
    const existente = { id: 3, nome: 'Espada', quantidade: 1, peso: 3, equipado: true, descricao: null };
    const nova = { id: 4, nome: 'Corda', quantidade: 2, peso: 2.5, equipado: false, descricao: null };
    const buscarFicha = vi.fn(() => of({ ...FICHA_DND, itens: [existente], moedas: { pc: 0, pp: 0, pe: 0, po: 12, pl: 0 } }));
    const { fixture, el } = montar({ buscarFicha });
    fixture.componentRef.setInput('aba', 'inventario');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(selecionada(el)).toBe('Inventário');
    expect(el.textContent).toContain('Espada');
    expect(el.querySelector<HTMLInputElement>('[data-campo="po"] input')!.value).toBe('12');
    expect(el.querySelector('p.carga')?.textContent?.trim()).toBe('Peso: 3/60 kg');

    const aba = fixture.debugElement.query((d) => d.name === 'app-inventario-dnd-aba').componentInstance as {
      atualizada: { emit(lista: unknown[]): void };
    };
    aba.atualizada.emit([existente, nova]);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(el.textContent).toContain('Corda');
    expect(el.querySelector('p.carga')?.textContent?.trim()).toBe('Peso: 8/60 kg');
    expect(buscarFicha).toHaveBeenCalledTimes(1);
  });

  it('na aba Inventario de D&D as moedas emitidas passam a ficha da aba, sem recarregar a ficha', async () => {
    const buscarFicha = vi.fn(() => of(FICHA_DND));
    const { fixture } = montar({ buscarFicha });
    fixture.componentRef.setInput('aba', 'inventario');
    fixture.detectChanges();
    await fixture.whenStable();

    const aba = fixture.debugElement.query((d) => d.name === 'app-inventario-dnd-aba').componentInstance as {
      moedasAtualizadas: { emit(moedas: unknown): void };
      ficha(): { moedas: unknown };
    };
    expect(aba.ficha().moedas).toEqual({ pc: 0, pp: 0, pe: 0, po: 0, pl: 0 });
    const novas = { pc: 0, pp: 0, pe: 0, po: 25, pl: 1 };
    aba.moedasAtualizadas.emit(novas);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(aba.ficha().moedas).toEqual(novas);
    expect(buscarFicha).toHaveBeenCalledTimes(1);
  });

  it('na aba Magias de D&D mostra a CD, e uma magia nova, os espacos e o atributo chegam a aba sem recarregar', async () => {
    const existente = { id: 3, nome: 'Raio de Fogo', nivel: 0, escola: null, tempo: null, alcance: null, componentes: null,
      duracao: null, concentracao: false, ritual: false, preparada: false, descricao: null };
    const nova = { ...existente, id: 4, nome: 'Bola de Fogo', nivel: 3 };
    const buscarFicha = vi.fn(() => of({ ...FICHA_DND, atributoConjuracao: 'CAR', magias: [existente] }));
    const { fixture, el } = montar({ buscarFicha });
    fixture.componentRef.setInput('aba', 'magias');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(selecionada(el)).toBe('Magias');
    expect(el.textContent).toContain('Raio de Fogo');
    // Nível 5 (proficiência 3) e Carisma 16 (+3): CD 14.
    expect(el.querySelector('[data-conta="cd"] .valor')?.textContent?.trim()).toBe('14');

    const aba = fixture.debugElement.query((d) => d.name === 'app-magias-dnd-aba').componentInstance as {
      atualizada: { emit(lista: unknown[]): void };
      espacosAtualizados: { emit(espacos: unknown): void };
    };
    aba.atualizada.emit([existente, nova]);
    aba.espacosAtualizados.emit({ ...espacosZerados(), '2': { total: 3, usados: 1 } });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(el.textContent).toContain('Bola de Fogo');
    expect(el.querySelectorAll('[data-nivel="2"] button.marcador').length).toBe(3);
    expect(el.querySelectorAll('[data-nivel="2"] button.marcador.usado').length).toBe(1);
    expect(buscarFicha).toHaveBeenCalledTimes(1);
  });

  it('na aba Rituais de Ordem mostra os rituais e um ritual novo aparece sem recarregar', async () => {
    const existente = { id: 3, nome: 'Cicatrização', circulo: 1, elemento: 'SANGUE', execucao: null, alcance: null, alvo: null,
      duracao: null, resistencia: null, descricao: null };
    const nova = { ...existente, id: 4, nome: 'Terror', circulo: 2, elemento: 'MEDO' };
    const buscarFicha = vi.fn(() => of({ ...FICHA, rituais: [existente] }));
    const { fixture, el } = montar({ buscarFicha });
    fixture.componentRef.setInput('aba', 'rituais');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(selecionada(el)).toBe('Rituais');
    expect(el.textContent).toContain('Cicatrização');

    const aba = fixture.debugElement.query((d) => d.name === 'app-rituais-ordem-aba').componentInstance as {
      atualizada: { emit(lista: unknown[]): void };
    };
    aba.atualizada.emit([existente, nova]);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(el.textContent).toContain('Terror');
    // NEX 10 e Presença 2: DT 14; 2º círculo custa 3 PE.
    expect(el.textContent).toContain('2º círculo · Medo · 3 PE · DT 14');
    expect(buscarFicha).toHaveBeenCalledTimes(1);
  });

  it.each([
    ['D&D', FICHA_DND, 'rituais'],
    ['Ordem', FICHA, 'magias'],
  ])('ficha de %s: a aba do outro sistema na URL abre a Ficha', (_nome, ficha, aba) => {
    const { fixture, el } = montar({ buscarFicha: vi.fn(() => of(ficha)) });
    fixture.componentRef.setInput('aba', aba);
    fixture.detectChanges();
    expect(selecionada(el)).toBe('Ficha');
  });

  it('sem aba na URL abre a Ficha, com os botoes Rolar', () => {
    const { el } = montar({ buscarFicha: vi.fn(() => of(FICHA)) });
    expect(selecionada(el)).toBe('Ficha');
    expect(botao(el, 'Rolar')).toBeDefined();
  });

  it('aba desconhecida abre a Ficha', () => {
    const { fixture, el } = montar({ buscarFicha: vi.fn(() => of(FICHA)) });
    fixture.componentRef.setInput('aba', 'xyz');
    fixture.detectChanges();
    expect(selecionada(el)).toBe('Ficha');
  });

  it('com aba=habilidades abre a aba Habilidades', async () => {
    const { fixture, el } = montar({ buscarFicha: vi.fn(() => of(FICHA)) });
    fixture.componentRef.setInput('aba', 'habilidades');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(selecionada(el)).toBe('Habilidades');
    expect(el.querySelector('app-habilidades-aba')).not.toBeNull();
  });

  it('na aba Habilidades mostra as habilidades da ficha e uma nova aparece sem recarregar', async () => {
    const existente = { id: 3, nome: 'Ataque especial', tipo: 'CLASSE' as const, descricao: null };
    const nova = { id: 4, nome: 'Faro paranormal', tipo: 'PARANORMAL' as const, descricao: null };
    const buscarFicha = vi.fn(() => of({ ...FICHA, habilidades: [existente] }));
    const { fixture, el } = montar({ buscarFicha });
    fixture.componentRef.setInput('aba', 'habilidades');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(el.textContent).toContain('Ataque especial');

    // A aba avisa a lista nova (depois de gravar na API); o detalhe a coloca na ficha sem recarregar.
    const aba = fixture.debugElement.query((d) => d.name === 'app-habilidades-aba').componentInstance as {
      atualizada: { emit(lista: unknown[]): void };
    };
    aba.atualizada.emit([existente, nova]);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(el.textContent).toContain('Faro paranormal');
    expect(buscarFicha).toHaveBeenCalledTimes(1);
  });

  it('na aba Descricao mostra a descricao da ficha e a nova fica na ficha sem recarregar', async () => {
    const buscarFicha = vi.fn(() => of({ ...FICHA, descricao: { aparencia: 'Alta', historia: null, anotacoes: null } }));
    const { fixture, el } = montar({ buscarFicha });
    fixture.componentRef.setInput('aba', 'descricao');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(el.querySelector<HTMLTextAreaElement>('[data-campo="aparencia"] textarea')?.value).toBe('Alta');

    const aba = fixture.debugElement.query((d) => d.name === 'app-descricao-aba').componentInstance as {
      atualizada: { emit(d: unknown): void };
    };
    aba.atualizada.emit({ aparencia: 'Baixa', historia: null, anotacoes: null });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.componentInstance.ficha()?.descricao.aparencia).toBe('Baixa');
    expect(buscarFicha).toHaveBeenCalledTimes(1);
  });

  it('clicar em Descricao poe a aba na URL sem criar entrada no historico', async () => {
    const { fixture, el, navegar } = montar({ buscarFicha: vi.fn(() => of(FICHA)) });
    const aba = Array.from(el.querySelectorAll<HTMLElement>('[role=tab]')).find((t) => t.textContent?.includes('Descrição'))!;
    aba.click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(navegar).toHaveBeenCalledWith([], {
      queryParams: { aba: 'descricao' },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  });

  it('retrato, aneis e resultado da rolagem ficam fora das abas', async () => {
    const { fixture, el } = montar({
      buscarFicha: vi.fn(() => of(FICHA)),
      testar: vi.fn(() => of(RESULTADO)),
    });
    botao(el, 'Rolar')?.click();
    await aguardar(fixture);
    await fixture.whenStable();
    const grupo = el.querySelector('mat-tab-group')!;
    expect(el.querySelector('app-medalhao')).not.toBeNull();
    expect(el.querySelector('app-resultado-rolagem')).not.toBeNull();
    expect(el.querySelectorAll('app-anel-status').length).toBe(3);
    for (const seletor of ['app-medalhao', 'app-resultado-rolagem', 'app-anel-status']) {
      expect(grupo.querySelector(seletor)).toBeNull();
    }
  });
});

describe('FichaDetalhe · dado animado', () => {
  /** Lançador falso: o lancar fica pendente (os dados rolando) até o teste mandar parar. */
  function lancadorFalso() {
    let parar!: () => void;
    const lancador: LancadorDeDados = {
      lancar: vi.fn(() => new Promise<void>((resolve) => (parar = resolve))),
      encerrar: vi.fn(),
    };
    return { lancador, parar: () => parar() };
  }

  function montarComDado(opcoes: { ficha?: FichaOrdem | FichaDnd; resultado?: TesteResultado; testar?: unknown } = {}) {
    const { lancador, parar } = lancadorFalso();
    const preparar = vi.fn(() => Promise.resolve<LancadorDeDados | null>(lancador));
    const ficha = opcoes.ficha ?? FICHA;
    const testar = opcoes.testar ?? vi.fn(() => of(opcoes.resultado ?? RESULTADO));
    const montado = montar({ buscarFicha: vi.fn(() => of(ficha)), testar }, [
      { provide: DadoAnimado, useValue: { preparar } },
    ]);
    return { ...montado, lancador, parar, preparar, testar };
  }

  const rolarInvestigacao = (el: HTMLElement) =>
    (el.querySelector('button[aria-label="Rolar Investigação, +5"]') as HTMLButtonElement).click();

  it('o cartao so aparece depois do terminou, e nao na resposta do POST', async () => {
    const { fixture, el, lancador } = montarComDado();

    rolarInvestigacao(el);
    await aguardar(fixture);

    expect(el.querySelector('app-camada-dos-dados')).not.toBeNull();
    expect(el.querySelector('app-resultado-rolagem')).toBeNull();
    expect(lancador.lancar).toHaveBeenCalledWith(expect.any(HTMLElement), d20sDoTeste([4, 19, 9], 1));

    (el.querySelector('app-camada-dos-dados') as HTMLElement).click(); // toque: pula
    fixture.detectChanges();

    expect(el.querySelector('app-camada-dos-dados')).toBeNull();
    expect(el.textContent).toContain('Investigação: 19 + 5 = 24');
    expect(lancador.encerrar).toHaveBeenCalled();
  });

  it('o cartao aparece quando os dados param e passa a pausa', async () => {
    const { fixture, el, parar } = montarComDado();
    rolarInvestigacao(el);
    await aguardar(fixture);

    vi.useFakeTimers();
    try {
      parar();
      await vi.advanceTimersByTimeAsync(600);
      fixture.detectChanges();
    } finally {
      vi.useRealTimers();
    }

    expect(el.querySelector('app-camada-dos-dados')).toBeNull();
    expect(el.querySelector('app-resultado-rolagem')).not.toBeNull();
  });

  it('Rolar fica desabilitado ate o cartao aparecer', async () => {
    const { fixture, el } = montarComDado();
    const rolar = el.querySelector('button[aria-label="Rolar Investigação, +5"]') as HTMLButtonElement;

    rolar.click();
    await aguardar(fixture);
    expect(rolar.disabled).toBe(true);

    (el.querySelector('app-camada-dos-dados') as HTMLElement).click();
    fixture.detectChanges();
    expect(rolar.disabled).toBe(false);
  });

  it('com o plano B (preparar devolve null) o cartao aparece direto e a camada nao abre', async () => {
    const preparar = vi.fn(() => Promise.resolve(null));
    const { fixture, el } = montar({ buscarFicha: vi.fn(() => of(FICHA)), testar: vi.fn(() => of(RESULTADO)) }, [
      { provide: DadoAnimado, useValue: { preparar } },
    ]);

    rolarInvestigacao(el);
    await aguardar(fixture);

    expect(preparar).toHaveBeenCalledOnce();
    expect(el.querySelector('app-camada-dos-dados')).toBeNull();
    expect(el.textContent).toContain('Investigação: 19 + 5 = 24');
  });

  it('com o POST falhando, nao pede o lancador e o erro aparece como hoje', async () => {
    const indisponivel: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'Pode estar acordando.', campos: {} };
    const { fixture, el, preparar } = montarComDado({ testar: vi.fn(() => throwError(() => indisponivel)) });

    rolarInvestigacao(el);
    await aguardar(fixture);

    expect(preparar).not.toHaveBeenCalled();
    expect(el.querySelector('app-camada-dos-dados')).toBeNull();
    expect(el.textContent).toContain('Servidor indisponível');
    expect((el.querySelector('button[aria-label="Rolar Investigação, +5"]') as HTMLButtonElement).disabled).toBe(false);
  });

  it('D&D com vantagem leva os 2 dados a camada, e o seletor volta a Normal', async () => {
    const { fixture, el, lancador } = montarComDado({ ficha: FICHA_DND, resultado: RESULTADO_DND });

    const modo = (nome: string) =>
      Array.from(el.querySelectorAll<HTMLButtonElement>('.modo button')).find((b) => b.textContent?.trim() === nome)!;
    modo('Vantagem').click();
    fixture.detectChanges();
    (el.querySelector('button[aria-label="Rolar Furtividade, +8"]') as HTMLButtonElement).click();
    await aguardar(fixture);

    expect(lancador.lancar).toHaveBeenCalledWith(expect.any(HTMLElement), d20sDoTeste([17, 6], 0));
    expect(modo('Normal').getAttribute('aria-pressed')).toBe('true');
  });

  it('sair da ficha durante a animacao encerra a cena', async () => {
    const { fixture, el, lancador } = montarComDado();
    rolarInvestigacao(el);
    await aguardar(fixture);

    fixture.destroy();

    expect(lancador.encerrar).toHaveBeenCalled();
  });

  it('trocar de ficha durante a animacao fecha a camada e o cartao da outra nao aparece', async () => {
    const buscarFicha = vi.fn((id: number) => of(id === 7 ? FICHA : { ...FICHA, id: 8, personagem: 'Outro' }));
    const { lancador } = lancadorFalso();
    const preparar = vi.fn(() => Promise.resolve<LancadorDeDados | null>(lancador));
    const { fixture, el } = montar({ buscarFicha, testar: vi.fn(() => of(RESULTADO)) }, [
      { provide: DadoAnimado, useValue: { preparar } },
    ]);
    rolarInvestigacao(el);
    await aguardar(fixture);

    fixture.componentRef.setInput('id', '8');
    fixture.detectChanges();
    await aguardar(fixture);

    expect(lancador.encerrar).toHaveBeenCalled();
    expect(el.querySelector('app-camada-dos-dados')).toBeNull();
    expect(el.querySelector('app-resultado-rolagem')).toBeNull();
    expect(el.textContent).toContain('Outro');
  });

  it('se a ficha for deixada antes de o preparar() terminar, a camada nem abre', async () => {
    let entregar!: (l: LancadorDeDados) => void;
    const lancador: LancadorDeDados = { lancar: vi.fn(() => Promise.resolve()), encerrar: vi.fn() };
    const preparar = vi.fn(() => new Promise<LancadorDeDados>((resolve) => (entregar = resolve)));
    const { fixture, el } = montar({ buscarFicha: vi.fn(() => of(FICHA)), testar: vi.fn(() => of(RESULTADO)) }, [
      { provide: DadoAnimado, useValue: { preparar } },
    ]);
    rolarInvestigacao(el);
    await aguardar(fixture);

    fixture.destroy();
    entregar(lancador);
    await new Promise<void>((resolve) => setTimeout(resolve, 0));

    expect(lancador.lancar).not.toHaveBeenCalled();
    expect(lancador.encerrar).toHaveBeenCalled();
  });
});

describe('FichaDetalhe · ataque e rolagem livre', () => {
  const ATAQUE: AtaqueResultado = {
    ataqueId: 3, nome: 'Espada longa',
    ataque: { dados: [14, 19], mantido: 19, bonus: 5, total: 24 },
    dano: { expressao: '1d8+3', dados: [{ lados: 8, valor: 6 }], bonus: 3, total: 9 },
    critico: false,
  };
  const LIVRE: RolagemLivre = {
    expressao: '2d6+1d4+3', bonus: 3, total: 11,
    dados: [{ lados: 6, valor: 4 }, { lados: 6, valor: 1 }, { lados: 4, valor: 3 }],
  };

  function montarComLancador(ficha: FichaOrdem | FichaDnd, api: Partial<Record<keyof FichaApi, unknown>>) {
    const lancador: LancadorDeDados = { lancar: vi.fn(() => new Promise<void>(() => undefined)), encerrar: vi.fn() };
    const preparar = vi.fn(() => Promise.resolve<LancadorDeDados | null>(lancador));
    const montado = montar({ buscarFicha: vi.fn(() => of(ficha)), ...api }, [{ provide: DadoAnimado, useValue: { preparar } }]);
    return { ...montado, lancador, preparar };
  }

  it('ataque de Ordem com critico: chama rolarAtaque, lanca os d20 e o dano em vinho e so entao mostra o cartao', async () => {
    const rolarAtaque = vi.fn(() => of({ ...ATAQUE, critico: true }));
    const { fixture, el, lancador } = montarComLancador(FICHA, { rolarAtaque });

    fixture.componentInstance.rolar({ tipo: 'ataque', ataqueId: 3 });
    await aguardar(fixture);

    expect(rolarAtaque).toHaveBeenCalledWith(7, 3, undefined);
    expect(lancador.lancar).toHaveBeenCalledWith(expect.any(HTMLElement), [
      { lados: 20, valor: 14, cor: 'comum' },
      { lados: 20, valor: 19, cor: 'mantido' },
      { lados: 8, valor: 6, cor: 'dano' },
    ]);
    expect(el.querySelector('app-resultado-ataque')).toBeNull();

    (el.querySelector('app-camada-dos-dados') as HTMLElement).click();
    fixture.detectChanges();

    expect(el.querySelector('app-camada-dos-dados')).toBeNull();
    expect(el.textContent).toContain('Espada longa: 19 + 5 = 24');
    expect(el.textContent).toContain('Dano: 6 + 3 = 9');
    expect(el.textContent).toContain('Crítico');
    expect(el.textContent).not.toContain('natural');
  });

  it('D&D com vantagem leva o modo ao servidor e mostra o 20 natural', async () => {
    const rolarAtaque = vi.fn(() => of({ ...ATAQUE, ataque: { dados: [20, 7], mantido: 20, bonus: 5, total: 25 } }));
    const { fixture, el } = montar({ buscarFicha: vi.fn(() => of(FICHA_DND)), rolarAtaque });

    fixture.componentInstance.rolar({ tipo: 'ataque', ataqueId: 3, modo: 'VANTAGEM' });
    await aguardar(fixture);

    expect(rolarAtaque).toHaveBeenCalledWith(9, 3, 'VANTAGEM');
    expect(el.textContent).toContain('20 natural');
  });

  it('rolagem livre: chama rolarLivre, lanca todos os dados comuns e mostra o cartao livre depois', async () => {
    const rolarLivre = vi.fn(() => of(LIVRE));
    const { fixture, el, lancador } = montarComLancador(FICHA_DND, { rolarLivre });

    fixture.componentInstance.rolar({ tipo: 'livre', expressao: '2d6+1d4+3' });
    await aguardar(fixture);

    expect(rolarLivre).toHaveBeenCalledWith(9, '2d6+1d4+3');
    expect(lancador.lancar).toHaveBeenCalledWith(expect.any(HTMLElement), [
      { lados: 6, valor: 4, cor: 'comum' },
      { lados: 6, valor: 1, cor: 'comum' },
      { lados: 4, valor: 3, cor: 'comum' },
    ]);
    expect(el.querySelector('app-resultado-livre')).toBeNull();

    (el.querySelector('app-camada-dos-dados') as HTMLElement).click();
    fixture.detectChanges();

    expect(el.textContent).toContain('2d6+1d4+3 = 11');
    expect(el.textContent).toContain('d4: 3');
  });

  it('mais de 20 dados na cena vai direto ao plano B: cartao na hora, sem pedir o lancador', async () => {
    const muitos: RolagemLivre = {
      expressao: '21d6', bonus: 0, total: 21, dados: Array.from({ length: 21 }, () => ({ lados: 6 as const, valor: 1 })),
    };
    const { fixture, el, preparar } = montarComLancador(FICHA, { rolarLivre: vi.fn(() => of(muitos)) });

    fixture.componentInstance.rolar({ tipo: 'livre', expressao: '21d6' });
    await aguardar(fixture);

    expect(preparar).not.toHaveBeenCalled();
    expect(el.querySelector('app-camada-dos-dados')).toBeNull();
    expect(el.textContent).toContain('21d6 = 21');
    expect(fixture.componentInstance.rolando()).toBe(false);
  });

  it('o d100 conta como 2 dados: 11 d100 passam do limite', async () => {
    const d100: RolagemLivre = {
      expressao: '11d100', bonus: 0, total: 11, dados: Array.from({ length: 11 }, () => ({ lados: 100 as const, valor: 1 })),
    };
    const { fixture, preparar } = montarComLancador(FICHA, { rolarLivre: vi.fn(() => of(d100)) });

    fixture.componentInstance.rolar({ tipo: 'livre', expressao: '11d100' });
    await aguardar(fixture);

    expect(preparar).not.toHaveBeenCalled();
  });

  it('erro do POST do ataque aparece como hoje e libera o Rolar', async () => {
    const erro: ApiError = { status: 404, titulo: 'Item não encontrado', detalhe: 'O ataque não existe.', campos: {} };
    const { fixture, el, preparar } = montarComLancador(FICHA, { rolarAtaque: vi.fn(() => throwError(() => erro)) });

    fixture.componentInstance.rolar({ tipo: 'ataque', ataqueId: 99 });
    await aguardar(fixture);

    expect(preparar).not.toHaveBeenCalled();
    expect(el.textContent).toContain('Item não encontrado: O ataque não existe.');
    expect(fixture.componentInstance.rolando()).toBe(false);
  });

  it('erro do POST da rolagem livre aparece na tela', async () => {
    const erro: ApiError = { status: 400, titulo: 'Dados inválidos', detalhe: 'Corrija.', campos: { expressao: 'Não existe d7.' } };
    const { fixture, el } = montar({ buscarFicha: vi.fn(() => of(FICHA)), rolarLivre: vi.fn(() => throwError(() => erro)) });

    fixture.componentInstance.rolar({ tipo: 'livre', expressao: '1d7' });
    await aguardar(fixture);

    expect(el.textContent).toContain('Dados inválidos: Corrija.');
  });

  it('enquanto rola, um segundo pedido (de qualquer tipo) e ignorado', () => {
    const resposta = new Subject<RolagemLivre>();
    const rolarLivre = vi.fn(() => resposta);
    const rolarAtaque = vi.fn(() => of(ATAQUE));
    const { fixture } = montar({ buscarFicha: vi.fn(() => of(FICHA)), rolarLivre, rolarAtaque });

    fixture.componentInstance.rolar({ tipo: 'livre', expressao: '1d6' });
    fixture.componentInstance.rolar({ tipo: 'ataque', ataqueId: 3 });

    expect(rolarLivre).toHaveBeenCalledTimes(1);
    expect(rolarAtaque).not.toHaveBeenCalled();
  });

  it('trocar de ficha durante a animacao do ataque descarta o cartao', async () => {
    const buscarFicha = vi.fn((id: number) => of(id === 7 ? FICHA : { ...FICHA, id: 8, personagem: 'Outro' }));
    const lancador: LancadorDeDados = { lancar: vi.fn(() => new Promise<void>(() => undefined)), encerrar: vi.fn() };
    const { fixture, el } = montar({ buscarFicha, rolarAtaque: vi.fn(() => of(ATAQUE)) }, [
      { provide: DadoAnimado, useValue: { preparar: () => Promise.resolve(lancador) } },
    ]);
    fixture.componentInstance.rolar({ tipo: 'ataque', ataqueId: 3 });
    await aguardar(fixture);

    fixture.componentRef.setInput('id', '8');
    fixture.detectChanges();
    await aguardar(fixture);

    expect(lancador.encerrar).toHaveBeenCalled();
    expect(el.querySelector('app-resultado-ataque')).toBeNull();
  });
});

describe('FichaDetalhe · abas Ataques e Dados', () => {
  const selecionada = (el: HTMLElement) => el.querySelector('[role=tab][aria-selected=true]')?.textContent?.trim();
  const ATAQUE_ORDEM = { id: 3, nome: 'Faca', pericia: 'LUTA' as const, bonus: 0, dano: '1d4', tipoDano: null, alcance: null, margem: 19, multiplicador: 3 };
  const ATAQUE_DND = { id: 4, nome: 'Espada', atributo: 'FOR' as const, proficiente: true, bonus: 0, dano: '1d8+3', tipoDano: 'cortante', alcance: null };
  const RESULTADO_ATAQUE: AtaqueResultado = {
    ataqueId: 3, nome: 'Faca', ataque: { dados: [10], mantido: 10, bonus: 5, total: 15 },
    dano: { expressao: '1d4', dados: [{ lados: 4, valor: 2 }], bonus: 0, total: 2 }, critico: false,
  };

  async function abrir(ficha: FichaOrdem | FichaDnd, aba: string, api: Partial<Record<keyof FichaApi, unknown>> = {}) {
    const montado = montar({ buscarFicha: vi.fn(() => of(ficha)), ...api });
    montado.fixture.componentRef.setInput('aba', aba);
    montado.fixture.detectChanges();
    await montado.fixture.whenStable();
    montado.fixture.detectChanges();
    return montado;
  }

  it.each([
    ['Ordem', FICHA, 'app-ataques-ordem-aba'],
    ['D&D', FICHA_DND, 'app-ataques-dnd-aba'],
  ])('?aba=ataques abre a aba Ataques de %s', async (_nome, ficha, seletor) => {
    const { el } = await abrir(ficha, 'ataques');
    expect(selecionada(el)).toBe('Ataques');
    expect(el.querySelector(seletor)).not.toBeNull();
  });

  it.each([
    ['Ordem', FICHA],
    ['D&D', FICHA_DND],
  ])('?aba=dados abre a aba Dados de %s', async (_nome, ficha) => {
    const { el } = await abrir(ficha, 'dados');
    expect(selecionada(el)).toBe('Dados');
    expect(el.querySelector('app-dados-aba')).not.toBeNull();
  });

  it('Rolar da aba Dados chama rolarLivre e o cartao livre aparece no topo, fora das abas', async () => {
    const rolarLivre = vi.fn(() =>
      of({ expressao: '1d6', bonus: 0, total: 4, dados: [{ lados: 6 as const, valor: 4 }] } satisfies RolagemLivre),
    );
    const { fixture, el } = await abrir(FICHA, 'dados', { rolarLivre });
    const campo = el.querySelector<HTMLInputElement>('app-dados-aba input')!;
    campo.value = '1d6';
    campo.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    const rolar = Array.from(el.querySelectorAll<HTMLButtonElement>('app-dados-aba button')).find((b) => b.textContent?.trim() === 'Rolar')!;
    rolar.click();
    await aguardar(fixture);

    expect(rolarLivre).toHaveBeenCalledWith(7, '1d6');
    expect(el.querySelector('mat-tab-group app-resultado-livre')).toBeNull();
    expect(el.querySelector('app-resultado-livre')).not.toBeNull();
    expect(el.textContent).toContain('1d6 = 4');
  });

  it('Rolar de um ataque de Ordem chama rolarAtaque e mostra o cartao do ataque', async () => {
    const rolarAtaque = vi.fn(() => of(RESULTADO_ATAQUE));
    const { fixture, el } = await abrir({ ...FICHA, ataques: [ATAQUE_ORDEM] }, 'ataques', { rolarAtaque });

    el.querySelector<HTMLButtonElement>('button[aria-label="Rolar ataque Faca"]')!.click();
    await aguardar(fixture);

    expect(rolarAtaque).toHaveBeenCalledWith(7, 3, undefined);
    expect(el.querySelector('app-resultado-ataque')).not.toBeNull();
    expect(el.textContent).toContain('Faca: 10 + 5 = 15');
  });

  it('D&D: Vantagem no seletor da aba Ataques vai ao servidor, e o seletor volta a Normal', async () => {
    const rolarAtaque = vi.fn(() => of({ ...RESULTADO_ATAQUE, nome: 'Espada', ataqueId: 4 }));
    const { fixture, el } = await abrir({ ...FICHA_DND, ataques: [ATAQUE_DND] }, 'ataques', { rolarAtaque });
    const modo = (nome: string) =>
      Array.from(el.querySelectorAll<HTMLButtonElement>('.modo button')).find((b) => b.textContent?.trim() === nome)!;

    modo('Vantagem').click();
    fixture.detectChanges();
    el.querySelector<HTMLButtonElement>('button[aria-label="Rolar ataque Espada"]')!.click();
    await aguardar(fixture);

    expect(rolarAtaque).toHaveBeenCalledWith(9, 4, 'VANTAGEM');
    expect(modo('Normal').getAttribute('aria-pressed')).toBe('true');
  });

  it('enquanto rola, o Rolar fica desabilitado nas abas Ataques e Dados', async () => {
    const resposta = new Subject<AtaqueResultado>();
    const { fixture, el } = await abrir({ ...FICHA, ataques: [ATAQUE_ORDEM] }, 'ataques', { rolarAtaque: vi.fn(() => resposta) });
    const rolar = el.querySelector<HTMLButtonElement>('button[aria-label="Rolar ataque Faca"]')!;

    rolar.click();
    fixture.detectChanges();
    expect(rolar.disabled).toBe(true);

    fixture.componentRef.setInput('aba', 'dados');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const campo = el.querySelector<HTMLInputElement>('app-dados-aba input')!;
    campo.value = '1d6';
    campo.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    const rolarDados = Array.from(el.querySelectorAll<HTMLButtonElement>('app-dados-aba button')).find((b) => b.textContent?.trim() === 'Rolar')!;
    expect(rolarDados.disabled).toBe(true);
  });

  it('um ataque novo gravado pela aba entra na ficha sem recarregar', async () => {
    const buscarFicha = vi.fn(() => of(FICHA));
    const { fixture, el } = montar({ buscarFicha });
    fixture.componentRef.setInput('aba', 'ataques');
    fixture.detectChanges();
    await fixture.whenStable();

    const aba = fixture.debugElement.query((d) => d.name === 'app-ataques-ordem-aba').componentInstance as {
      atualizada: { emit(lista: unknown[]): void };
    };
    aba.atualizada.emit([ATAQUE_ORDEM]);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(el.textContent).toContain('Faca');
    expect(buscarFicha).toHaveBeenCalledTimes(1);
  });

  it('um ataque novo de D&D tambem entra na ficha sem recarregar', async () => {
    const buscarFicha = vi.fn(() => of(FICHA_DND));
    const { fixture, el } = montar({ buscarFicha });
    fixture.componentRef.setInput('aba', 'ataques');
    fixture.detectChanges();
    await fixture.whenStable();

    const aba = fixture.debugElement.query((d) => d.name === 'app-ataques-dnd-aba').componentInstance as {
      atualizada: { emit(lista: unknown[]): void };
    };
    aba.atualizada.emit([ATAQUE_DND]);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(el.textContent).toContain('Espada');
    expect(buscarFicha).toHaveBeenCalledTimes(1);
  });
});
