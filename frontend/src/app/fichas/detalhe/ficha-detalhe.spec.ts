import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { FichaApi } from '../ficha-api';
import { Ficha, TestePericiaResultado } from '../ficha.model';
import { FichaDetalhe } from './ficha-detalhe';

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

describe('FichaDetalhe', () => {
  it('mostra os dados e as 23 pericias com botao de rolar', () => {
    const { el } = montar({ buscar: vi.fn(() => of(FICHA)) });
    expect(el.textContent).toContain('Detetive Lia');
    expect(el.textContent).toContain('9/12');
    expect(el.querySelectorAll('button[aria-label^="Rolar "]').length).toBe(23);
  });

  it('as dicas ficam em botoes pequenos, nunca no nome da pericia', () => {
    const { el } = montar({ buscar: vi.fn(() => of(FICHA)) });
    const gatilhos = Array.from(el.querySelectorAll('.mat-mdc-tooltip-trigger'));
    expect(gatilhos.length).toBe(23);
    for (const gatilho of gatilhos) {
      expect(gatilho.tagName).toBe('BUTTON');
      expect(gatilho.getAttribute('aria-label')).toMatch(/^Sobre /);
    }
  });

  it('rola a pericia e mostra o resultado', () => {
    const testarPericia = vi.fn(() => of(RESULTADO));
    const { fixture, el } = montar({ buscar: vi.fn(() => of(FICHA)), testarPericia });

    (el.querySelector('button[aria-label="Rolar Investigação"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(testarPericia).toHaveBeenCalledWith(7, 'INVESTIGACAO');
    expect(el.textContent).toContain('Investigação: 23 → Sucesso');
  });

  it('nao envia duas rolagens com toque duplo', () => {
    const resposta = new Subject<TestePericiaResultado>();
    const testarPericia = vi.fn(() => resposta);
    const { fixture, el } = montar({ buscar: vi.fn(() => of(FICHA)), testarPericia });
    const rolar = el.querySelector('button[aria-label="Rolar Investigação"]') as HTMLButtonElement;

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

    botao(el, 'Excluir ficha').click();
    fixture.detectChanges();
    botao(el, 'Cancelar').click();
    fixture.detectChanges();
    expect(excluir).not.toHaveBeenCalled();

    botao(el, 'Excluir ficha').click();
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
      (el.querySelector('button[aria-label="Rolar Investigação"]') as HTMLButtonElement).click();
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

    botao(el, 'Excluir ficha').click();
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
