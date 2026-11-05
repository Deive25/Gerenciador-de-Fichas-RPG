import { OverlayContainer } from '@angular/cdk/overlay';
import { Provider, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { RetratoCache } from '../retrato-cache';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { FichaApi } from '../ficha-api';
import { FichaOrdem, TesteResultado } from '../ficha.model';
import { FichaDetalhe } from './ficha-detalhe';

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
};

const RESULTADO: TesteResultado = {
  alvo: 'PERICIA', codigo: 'INVESTIGACAO', nome: 'Investigação', dados: [4, 19, 9], mantido: 19, bonus: 5, total: 24,
};

function montar(api: Partial<Record<keyof FichaApi, unknown>>, extras: Provider[] = []) {
  TestBed.configureTestingModule({
    imports: [FichaDetalhe],
    providers: [provideRouter([]), { provide: FichaApi, useValue: { versaoFichas: signal(0), ...api } }, ...extras],
  });
  const router = TestBed.inject(Router);
  const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);
  const fixture = TestBed.createComponent(FichaDetalhe);
  fixture.componentRef.setInput('id', '7');
  fixture.detectChanges();
  return { fixture, el: fixture.nativeElement as HTMLElement, navegar };
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

  it('mostra classe, NEX, origem e Defesa (10 + AGI)', () => {
    const { el } = montar({ buscarFicha: vi.fn(() => of(FICHA)) });
    const bloco = el.querySelector('.identidade') as HTMLElement;
    expect(bloco.textContent).toContain('Ocultista');
    expect(bloco.textContent).toContain('10%');
    expect(bloco.textContent).toContain('Acadêmica');
    expect(bloco.textContent).toContain('Defesa');
    expect(bloco.textContent).toContain('13');
  });

  it('ficha convertida: classe nula vira A definir e origem vazia nao quebra', () => {
    const convertida = { ...FICHA, classe: null, origem: null };
    const { el } = montar({ buscarFicha: vi.fn(() => of(convertida)) });
    expect((el.querySelector('.identidade') as HTMLElement).textContent).toContain('A definir');
  });

  it('desenha os tres aneis de recursos', () => {
    const { el } = montar({ buscarFicha: vi.fn(() => of(FICHA)) });
    expect(el.querySelectorAll('app-anel-status').length).toBe(3);
    expect(el.textContent).toContain('9/12');
    expect(el.textContent).toContain('5/10');
  });

  it('os 5 atributos tem valor e botao de rolar com nome acessivel', () => {
    const { el } = montar({ buscarFicha: vi.fn(() => of(FICHA)) });
    for (const nome of [
      'Rolar Agilidade, 3', 'Rolar Força, 1', 'Rolar Intelecto, 4', 'Rolar Presença, 2', 'Rolar Vigor, 0',
    ]) {
      expect(el.querySelector(`button[aria-label="${nome}"]`)).not.toBeNull();
    }
  });

  it('mostra as 28 pericias com selo do grau, bonus e botao de rolar', () => {
    const { el } = montar({ buscarFicha: vi.fn(() => of(FICHA)) });
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
    const { el } = montar({ buscarFicha: vi.fn(() => of(FICHA)) });
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
    const { el } = montar({ buscarFicha: vi.fn(() => of(FICHA)) });
    const item = el.querySelector('button[aria-label="Rolar Acrobacia, +0"]')?.closest('.pericia') as HTMLElement;
    expect(getComputedStyle(item.querySelector('.nome') as HTMLElement).opacity).not.toBe('0.5');
    expect(getComputedStyle(item.querySelector('.valor') as HTMLElement).opacity).not.toBe('0.5');
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

  it('as dicas ficam em botoes pequenos, nunca no nome do item', () => {
    const { el } = montar({ buscarFicha: vi.fn(() => of(FICHA)) });
    const gatilhos = Array.from(el.querySelectorAll('.mat-mdc-tooltip-trigger'));
    // 5 atributos + 28 perícias.
    expect(gatilhos.length).toBe(33);
    for (const gatilho of gatilhos) {
      expect(gatilho.tagName).toBe('BUTTON');
      expect(gatilho.getAttribute('aria-label')).toMatch(/^Sobre /);
    }
  });

  it('rola a pericia e mostra o resultado com cada dado', () => {
    const testar = vi.fn(() => of(RESULTADO));
    const { fixture, el } = montar({ buscarFicha: vi.fn(() => of(FICHA)), testar });

    (el.querySelector('button[aria-label="Rolar Investigação, +5"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(testar).toHaveBeenCalledWith(7, { alvo: 'PERICIA', codigo: 'INVESTIGACAO' });
    expect(el.textContent).toContain('Investigação: 19 + 5 = 24');
    expect(el.querySelectorAll('.dado').length).toBe(3);
    expect(el.querySelectorAll('.mantido').length).toBe(1);
  });

  it('rola o atributo', () => {
    const resultado: TesteResultado = {
      alvo: 'ATRIBUTO', codigo: 'AGI', nome: 'Agilidade', dados: [8, 15, 2], mantido: 15, bonus: 0, total: 15,
    };
    const testar = vi.fn(() => of(resultado));
    const { fixture, el } = montar({ buscarFicha: vi.fn(() => of(FICHA)), testar });

    (el.querySelector('button[aria-label="Rolar Agilidade, 3"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(testar).toHaveBeenCalledWith(7, { alvo: 'ATRIBUTO', codigo: 'AGI' });
    expect(el.textContent).toContain('Agilidade: 15 = 15');
  });

  it('nao envia duas rolagens com toque duplo', () => {
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
    fixture.detectChanges();
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

  it('rolagem bem-sucedida limpa o erro da rolagem anterior', () => {
    const indisponivel: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'Pode estar acordando.', campos: {} };
    const testar = vi
      .fn()
      .mockReturnValueOnce(throwError(() => indisponivel))
      .mockReturnValueOnce(of(RESULTADO));
    const { fixture, el } = montar({ buscarFicha: vi.fn(() => of(FICHA)), testar });
    const rolar = () => {
      (el.querySelector('button[aria-label="Rolar Investigação, +5"]') as HTMLButtonElement).click();
      fixture.detectChanges();
    };

    rolar();
    expect(el.textContent).toContain('Servidor indisponível');
    rolar();

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
});
