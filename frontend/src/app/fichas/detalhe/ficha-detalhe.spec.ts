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

  it('ficha inexistente', () => {
    const erro: ApiError = { status: 404, titulo: 'Ficha não encontrada', detalhe: 'Não existe ficha com o id 7.', campos: {} };
    const { el } = montar({ buscar: vi.fn(() => throwError(() => erro)) });
    expect(el.textContent).toContain('Ficha não encontrada');
    expect(el.querySelector('a[href="/fichas"]')?.textContent).toContain('Voltar para as fichas');
  });
});
