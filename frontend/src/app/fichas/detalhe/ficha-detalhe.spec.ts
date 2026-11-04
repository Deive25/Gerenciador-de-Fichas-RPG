import { OverlayContainer } from '@angular/cdk/overlay';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { FichaApi } from '../ficha-api';
import { Ficha, TestePericiaResultado } from '../ficha.model';
import { FichaDetalhe, larguraBarra, marcaExtremo } from './ficha-detalhe';

const FICHA: Ficha = {
  id: 7, jogador: 'Arthur', personagem: 'Detetive Lia', genero: 'F', idade: 34,
  vidaAtual: 9, vidaMaxima: 12, sanidadeAtual: 48, sanidadeMaxima: 60,
  forca: 45, destreza: 60, constituicao: null, inteligencia: 75, carisma: null,
  sabedoria: null, tamanho: null, sorte: null, poder: null,
  pericias: { INVESTIGACAO: 70 },
};

const RESULTADO: TestePericiaResultado = {
  pericia: 'INVESTIGACAO', nome: 'Investigação', valor: 70, rolagem: 23, resultado: 'SUCESSO',
};

function montar(api: Partial<Record<keyof FichaApi, unknown>>) {
  TestBed.configureTestingModule({
    imports: [FichaDetalhe],
    providers: [provideRouter([]), { provide: FichaApi, useValue: { versaoFichas: signal(0), ...api } }],
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

describe('marcaExtremo e larguraBarra', () => {
  it('a marca do extremo fica em valor/5, sem passar de 100', () => {
    expect(marcaExtremo(70)).toBe(14);
    expect(marcaExtremo(0)).toBe(0);
    expect(marcaExtremo(600)).toBe(100);
  });

  it('a barra vai ate o valor, entre 0 e 100', () => {
    expect(larguraBarra(70)).toBe(70);
    expect(larguraBarra(130)).toBe(100);
    expect(larguraBarra(-5)).toBe(0);
  });
});

describe('FichaDetalhe', () => {
  it('mostra jogador, idade e genero', () => {
    const { el } = montar({ buscar: vi.fn(() => of(FICHA)) });
    expect(el.textContent).toContain('Arthur · 34 anos · F');
  });

  it('pericia sem valor fica esmaecida e continua rolavel', () => {
    const { el } = montar({ buscar: vi.fn(() => of(FICHA)) });
    const rolar = el.querySelector('button[aria-label="Rolar Acrobacia, 0"]') as HTMLButtonElement;
    expect(rolar.disabled).toBe(false);
    expect(rolar.closest('.pericia')?.classList).toContain('sem-valor');
    expect(el.querySelector('button[aria-label="Rolar Investigação, 70"]')?.closest('.pericia')?.classList).not.toContain(
      'sem-valor',
    );
  });

  it('a barra da pericia vai ate o valor e marca o extremo', () => {
    const { el } = montar({ buscar: vi.fn(() => of(FICHA)) });
    const item = el.querySelector('button[aria-label="Rolar Investigação, 70"]')?.closest('.pericia') as HTMLElement;
    expect((item.querySelector('.barra i') as HTMLElement).style.width).toBe('70%');
    expect((item.querySelector('.barra em') as HTMLElement).style.left).toBe('14%');
  });

  it('o menu de acoes tem Editar (link) e Excluir', () => {
    const { fixture } = montar({ buscar: vi.fn(() => of(FICHA)) });
    abrirMenu(fixture);
    const [editar, excluir] = itensDoMenu();
    expect(editar.textContent).toContain('Editar');
    expect(editar.getAttribute('href')).toBe('/fichas/7/editar');
    expect(excluir.textContent).toContain('Excluir');
  });

  it('mostra os dados e as 23 pericias com botao de rolar', () => {
    const { el } = montar({ buscar: vi.fn(() => of(FICHA)) });
    expect(el.textContent).toContain('Detetive Lia');
    expect(el.textContent).toContain('9/12');
    expect(el.querySelectorAll('button[aria-label^="Rolar "]').length).toBe(23);
  });

  it('as dicas ficam em botoes pequenos, nunca no nome da pericia', () => {
    const { el } = montar({ buscar: vi.fn(() => of(FICHA)) });
    const gatilhos = Array.from(el.querySelectorAll('.mat-mdc-tooltip-trigger'));
    // 9 atributos + 23 perícias: os atributos não ficam mais numa aba escondida (plano 2.1b).
    expect(gatilhos.length).toBe(32);
    for (const gatilho of gatilhos) {
      expect(gatilho.tagName).toBe('BUTTON');
      expect(gatilho.getAttribute('aria-label')).toMatch(/^Sobre /);
    }
  });

  it('rola a pericia e mostra o resultado', () => {
    const testarPericia = vi.fn(() => of(RESULTADO));
    const { fixture, el } = montar({ buscar: vi.fn(() => of(FICHA)), testarPericia });

    (el.querySelector('button[aria-label="Rolar Investigação, 70"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(testarPericia).toHaveBeenCalledWith(7, 'INVESTIGACAO');
    expect(el.textContent).toContain('Investigação: 23 → Sucesso');
  });

  it('nao envia duas rolagens com toque duplo', () => {
    const resposta = new Subject<TestePericiaResultado>();
    const testarPericia = vi.fn(() => resposta);
    const { fixture, el } = montar({ buscar: vi.fn(() => of(FICHA)), testarPericia });
    const rolar = el.querySelector('button[aria-label="Rolar Investigação, 70"]') as HTMLButtonElement;

    rolar.click();
    fixture.detectChanges();
    rolar.click();
    fixture.detectChanges();

    expect(testarPericia).toHaveBeenCalledTimes(1);
    expect(rolar.disabled).toBe(true);

    resposta.next(RESULTADO);
    resposta.complete();
    fixture.detectChanges();
    expect(rolar.disabled).toBe(false);
  });

  it('exclui apos confirmar', () => {
    const excluir = vi.fn(() => of(undefined));
    const { fixture, el, navegar } = montar({ buscar: vi.fn(() => of(FICHA)), excluir });

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
    const { fixture, el } = montar({ buscar });
    expect(el.textContent).toContain('Servidor indisponível');

    botao(el, 'Tentar de novo').click();
    fixture.detectChanges();

    expect(el.textContent).toContain('Detetive Lia');
    expect(el.textContent).not.toContain('Servidor indisponível');
  });

  it('rolagem bem-sucedida limpa o erro da rolagem anterior', () => {
    const indisponivel: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'Pode estar acordando.', campos: {} };
    const testarPericia = vi
      .fn()
      .mockReturnValueOnce(throwError(() => indisponivel))
      .mockReturnValueOnce(of(RESULTADO));
    const { fixture, el } = montar({ buscar: vi.fn(() => of(FICHA)), testarPericia });
    const rolar = () => {
      (el.querySelector('button[aria-label="Rolar Investigação, 70"]') as HTMLButtonElement).click();
      fixture.detectChanges();
    };

    rolar();
    expect(el.textContent).toContain('Servidor indisponível');
    rolar();

    expect(el.textContent).toContain('Investigação: 23 → Sucesso');
    expect(el.textContent).not.toContain('Servidor indisponível');
  });

  it('sim excluir continua bloqueado ate a navegacao terminar', () => {
    const excluir = vi.fn(() => of(undefined));
    const { fixture, el, navegar } = montar({ buscar: vi.fn(() => of(FICHA)), excluir });
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
    const { el } = montar({ buscar: vi.fn(() => throwError(() => erro)) });
    expect(el.textContent).toContain('Ficha não encontrada');
    expect(el.querySelector('a[href="/fichas"]')?.textContent).toContain('Voltar para as fichas');
  });
});
