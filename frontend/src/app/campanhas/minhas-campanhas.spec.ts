import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { NEVER, of, throwError } from 'rxjs';
import { ApiError } from '../core/api-error';
import { CampanhaApi } from './campanha-api';
import { CampanhaResumo } from './campanha.model';
import { MinhasCampanhas } from './minhas-campanhas';

const MESTRE: CampanhaResumo = {
  id: 3, nome: 'Mesa de sexta', papel: 'MESTRE', mestre: { id: 1, nome: 'Ana' }, fichas: 4,
};
const JOGADOR: CampanhaResumo = {
  id: 5, nome: 'Calamidade', papel: 'JOGADOR', mestre: { id: 2, nome: 'Bia' }, fichas: 1,
};
const INDISPONIVEL: ApiError = {
  status: 0, titulo: 'Servidor indisponível', detalhe: 'Ele pode estar acordando.', campos: {},
};

function montar(api: Partial<Record<keyof CampanhaApi, unknown>>) {
  TestBed.configureTestingModule({
    imports: [MinhasCampanhas],
    providers: [provideRouter([]), { provide: CampanhaApi, useValue: api }],
  });
  const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  const fixture = TestBed.createComponent(MinhasCampanhas);
  fixture.detectChanges();
  return { fixture, el: fixture.nativeElement as HTMLElement, navegar };
}

function botao(el: HTMLElement, texto: string): HTMLButtonElement {
  return Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes(texto)) as HTMLButtonElement;
}

describe('MinhasCampanhas', () => {
  it('carregando: avisa os leitores de tela com role=status', () => {
    const { el } = montar({ listar: () => NEVER });
    expect(el.querySelector('[role="status"]')?.textContent).toContain('Carregando campanhas');
  });

  it('lista uma campanha por cartão, com papel, mestre e fichas, ligada a /campanhas/:id', () => {
    const { el } = montar({ listar: () => of([MESTRE, JOGADOR]) });
    const cartoes = Array.from(el.querySelectorAll<HTMLAnchorElement>('a.cartao-campanha'));
    expect(cartoes.map((a) => a.getAttribute('href'))).toEqual(['/campanhas/3', '/campanhas/5']);
    expect(cartoes[0].textContent).toContain('Mesa de sexta');
    expect(cartoes[0].textContent).toContain('Mestre');
    expect(cartoes[0].textContent).toContain('4 fichas');
    expect(cartoes[1].textContent).toContain('Jogador');
    expect(cartoes[1].textContent).toContain('Mestre: Bia');
    expect(cartoes[1].textContent).toContain('1 ficha');
    expect(cartoes[1].textContent).not.toContain('1 fichas');
  });

  it('vazio: explica e oferece criar a primeira campanha', () => {
    const { el } = montar({ listar: () => of([]) });
    expect(el.textContent).toContain('Você ainda não participa de nenhuma campanha.');
    expect(botao(el, 'Criar a primeira campanha')).toBeTruthy();
  });

  it('erro: mostra o motivo e tenta de novo', () => {
    const listar = vi
      .fn()
      .mockReturnValueOnce(throwError(() => INDISPONIVEL))
      .mockReturnValueOnce(of([MESTRE]));
    const { fixture, el } = montar({ listar });
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Servidor indisponível');

    botao(el, 'Tentar de novo').click();
    fixture.detectChanges();

    expect(listar).toHaveBeenCalledTimes(2);
    expect(el.querySelectorAll('a.cartao-campanha').length).toBe(1);
  });

  it('+ Nova campanha abre o formulário; criar envia nome e descrição e abre a campanha', () => {
    const criar = vi.fn(() => of(9));
    const { fixture, el, navegar } = montar({ listar: () => of([]), criar });
    botao(el, '+ Nova campanha').click();
    fixture.detectChanges();

    const nome = el.querySelector<HTMLInputElement>('[formControlName="nome"]')!;
    nome.value = 'Mesa nova';
    nome.dispatchEvent(new Event('input'));
    el.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();

    expect(criar).toHaveBeenCalledWith({ nome: 'Mesa nova', descricao: null });
    expect(navegar).toHaveBeenCalledWith(['/campanhas', 9]);
  });

  it('erro ao criar fica no formulário e o botão volta a funcionar', () => {
    const criar = vi.fn(() => throwError(() => INDISPONIVEL));
    const { fixture, el } = montar({ listar: () => of([]), criar });
    botao(el, '+ Nova campanha').click();
    fixture.detectChanges();
    const nome = el.querySelector<HTMLInputElement>('[formControlName="nome"]')!;
    nome.value = 'Mesa';
    nome.dispatchEvent(new Event('input'));
    el.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();

    expect(el.querySelector('form [role="alert"]')?.textContent).toContain('Ele pode estar acordando.');
    expect(el.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(false);
  });

  it('Cancelar fecha o formulário', () => {
    const { fixture, el } = montar({ listar: () => of([MESTRE]) });
    botao(el, '+ Nova campanha').click();
    fixture.detectChanges();
    expect(el.querySelector('form')).not.toBeNull();

    botao(el, 'Cancelar').click();
    fixture.detectChanges();
    expect(el.querySelector('form')).toBeNull();
  });
});
