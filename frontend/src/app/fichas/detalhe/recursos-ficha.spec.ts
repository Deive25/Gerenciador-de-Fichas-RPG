import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { FichaApi } from '../ficha-api';
import { Ficha } from '../ficha.model';
import { RecursosFicha, aplicarAjuste, limitarDigitado } from './recursos-ficha';

const FICHA: Ficha = {
  id: 7, jogador: 'Arthur', personagem: 'Lia', genero: 'F', idade: 34,
  vidaAtual: 9, vidaMaxima: 12, sanidadeAtual: 48, sanidadeMaxima: 60,
  forca: null, destreza: null, constituicao: null, inteligencia: null, carisma: null,
  sabedoria: null, tamanho: null, sorte: null, poder: null,
  pericias: { INVESTIGACAO: 70 }, retratoVersao: 123,
};

describe('aplicarAjuste', () => {
  it('soma e subtrai 1', () => {
    expect(aplicarAjuste(9, +1, 12)).toBe(10);
    expect(aplicarAjuste(9, -1, 12)).toBe(8);
  });

  it('o + nao passa do maximo, e acima dele fica parado', () => {
    expect(aplicarAjuste(12, +1, 12)).toBe(12);
    expect(aplicarAjuste(15, +1, 12)).toBe(15);
  });

  it('o - nao passa de 0 e, acima do maximo, desce de 1 em 1', () => {
    expect(aplicarAjuste(0, -1, 12)).toBe(0);
    expect(aplicarAjuste(15, -1, 12)).toBe(14);
  });

  it('valor ausente conta como 0; sem maximo, o + nao tem teto', () => {
    expect(aplicarAjuste(null, +1, null)).toBe(1);
    expect(aplicarAjuste(99, +1, null)).toBe(100);
  });
});

describe('limitarDigitado', () => {
  it('fica entre 0 e o maximo', () => {
    expect(limitarDigitado(20, 12)).toBe(12);
    expect(limitarDigitado(-3, 12)).toBe(0);
    expect(limitarDigitado(50, null)).toBe(50);
  });
});

describe('RecursosFicha', () => {
  let atualizar: ReturnType<typeof vi.fn>;

  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  function montar(respostas: ReturnType<typeof vi.fn>) {
    atualizar = respostas;
    TestBed.configureTestingModule({
      imports: [RecursosFicha],
      providers: [{ provide: FichaApi, useValue: { atualizar, versaoFichas: signal(0) } }],
    });
    const fixture = TestBed.createComponent(RecursosFicha);
    fixture.componentRef.setInput('ficha', FICHA);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  function clicar(fixture: ComponentFixture<RecursosFicha>, rotulo: string, vezes = 1): void {
    for (let i = 0; i < vezes; i++) {
      (fixture.nativeElement.querySelector(`button[aria-label="${rotulo}"]`) as HTMLButtonElement).click();
      fixture.detectChanges();
    }
  }

  it('tem os quatro botoes com nomes acessiveis', () => {
    const { el } = montar(vi.fn());
    for (const nome of ['Diminuir vida', 'Aumentar vida', 'Diminuir sanidade', 'Aumentar sanidade']) {
      expect(el.querySelector(`button[aria-label="${nome}"]`)).not.toBeNull();
    }
  });

  it('varios toques viram um PUT so, 800 ms depois do ultimo, sem id nem retratoVersao no corpo', () => {
    const { fixture, el } = montar(vi.fn((_id: number, entrada: object) => of({ ...FICHA, ...entrada })));

    clicar(fixture, 'Aumentar vida', 3);
    expect(el.textContent).toContain('12/12');
    vi.advanceTimersByTime(799);
    expect(atualizar).not.toHaveBeenCalled();
    vi.advanceTimersByTime(1);

    expect(atualizar).toHaveBeenCalledTimes(1);
    const [id, corpo] = atualizar.mock.calls[0];
    expect(id).toBe(7);
    expect(corpo.vidaAtual).toBe(12);
    expect(corpo.sanidadeAtual).toBe(48);
    expect('id' in corpo).toBe(false);
    expect('retratoVersao' in corpo).toBe(false);
  });

  it('se o salvamento falhar, volta ao ultimo valor salvo e mostra o erro', () => {
    const erro: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'Tente de novo.', campos: {} };
    const { fixture, el } = montar(vi.fn(() => throwError(() => erro)));

    clicar(fixture, 'Diminuir vida', 2);
    vi.advanceTimersByTime(800);
    fixture.detectChanges();

    expect(el.textContent).toContain('9/12');
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Servidor indisponível: Tente de novo.');
  });

  it('um toque durante o salvamento salva de novo depois, sem ser apagado pela resposta', () => {
    const primeira = new Subject<Ficha>();
    const { fixture, el } = montar(
      vi.fn().mockReturnValueOnce(primeira).mockReturnValueOnce(of({ ...FICHA, vidaAtual: 11 })),
    );

    clicar(fixture, 'Aumentar vida');
    vi.advanceTimersByTime(800);
    fixture.detectChanges();
    expect(atualizar).toHaveBeenCalledTimes(1);
    expect(el.querySelector('[role="status"]')?.textContent).toContain('salvando');

    clicar(fixture, 'Aumentar vida');
    vi.advanceTimersByTime(800);
    primeira.next({ ...FICHA, vidaAtual: 10 });
    primeira.complete();
    fixture.detectChanges();

    expect(atualizar).toHaveBeenCalledTimes(2);
    expect(atualizar.mock.calls[1][1].vidaAtual).toBe(11);
    expect(el.textContent).toContain('11/12');
  });

  it('sair da ficha com um ajuste esperando salva na hora', () => {
    const { fixture } = montar(vi.fn((_id: number, entrada: object) => of({ ...FICHA, ...entrada })));

    clicar(fixture, 'Diminuir sanidade');
    fixture.destroy();

    expect(atualizar).toHaveBeenCalledTimes(1);
    expect(atualizar.mock.calls[0][1].sanidadeAtual).toBe(47);
  });

  it('digitar o valor e confirmar com Enter salva o numero, limitado ao maximo', () => {
    const { fixture, el } = montar(vi.fn((_id: number, entrada: object) => of({ ...FICHA, ...entrada })));

    (el.querySelector('button[aria-label^="Editar sanidade"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    const campo = el.querySelector('input[type="number"]') as HTMLInputElement;
    campo.value = '5';
    campo.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
    fixture.detectChanges();
    vi.advanceTimersByTime(800);

    expect(atualizar.mock.calls[0][1].sanidadeAtual).toBe(5);
  });

  it('Esc cancela a digitacao sem salvar', () => {
    const { fixture, el } = montar(vi.fn());

    (el.querySelector('button[aria-label^="Editar vida"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    const campo = el.querySelector('input[type="number"]') as HTMLInputElement;
    campo.value = '1';
    campo.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    vi.advanceTimersByTime(800);

    expect(el.querySelector('input[type="number"]')).toBeNull();
    expect(atualizar).not.toHaveBeenCalled();
  });

  it('os botoes respeitam os limites: - desabilitado em 0 e + desabilitado no maximo', () => {
    TestBed.configureTestingModule({
      imports: [RecursosFicha],
      providers: [{ provide: FichaApi, useValue: { atualizar: vi.fn(), versaoFichas: signal(0) } }],
    });
    const fixture = TestBed.createComponent(RecursosFicha);
    fixture.componentRef.setInput('ficha', { ...FICHA, vidaAtual: 0, sanidadeAtual: 60 });
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    expect((el.querySelector('button[aria-label="Diminuir vida"]') as HTMLButtonElement).disabled).toBe(true);
    expect((el.querySelector('button[aria-label="Aumentar sanidade"]') as HTMLButtonElement).disabled).toBe(true);
    expect((el.querySelector('button[aria-label="Aumentar vida"]') as HTMLButtonElement).disabled).toBe(false);
  });
});
