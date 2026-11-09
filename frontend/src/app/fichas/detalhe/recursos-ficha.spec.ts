import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { FichaApi } from '../ficha-api';
import { FichaDnd, FichaOrdem } from '../ficha.model';
import { RecursosFicha, aplicarAjuste, limitarDigitado } from './recursos-ficha';

const FICHA: FichaOrdem = {
  id: 7, sistema: 'ORDEM_PARANORMAL', jogador: 'Arthur', personagem: 'Lia', genero: 'F', idade: 34, retratoVersao: 123,
  classe: 'OCULTISTA', origem: 'Acadêmica', nex: 45,
  atributos: { AGI: 2, FOR: 1, INT: 4, PRE: 3, VIG: 1 },
  recursos: {
    PV: { atual: 6, maximo: 12 },
    SAN: { atual: 48, maximo: 60 },
    PE: { atual: 5, maximo: 10 },
  },
  pericias: { INVESTIGACAO: 'TREINADO' },
  descricao: { aparencia: null, historia: null, anotacoes: null },
  habilidades: [],
};

const FICHA_DND: FichaDnd = {
  id: 9, sistema: 'DND', jogador: 'Arthur', personagem: 'Kael', genero: 'M', idade: 27, retratoVersao: null,
  edicao: 'D2024', classe: 'Ladino', especie: 'Halfling', antecedente: null, nivel: 5,
  atributos: { FOR: 8, DES: 14, CON: 12, INT: 10, SAB: 9, CAR: 16 },
  recursos: { PV: { atual: 30, maximo: 38 }, PV_TEMP: { atual: 4, maximo: null } },
  ca: 15, inspiracao: false, salvaguardas: [], pericias: {},
  descricao: { aparencia: null, historia: null, anotacoes: null },
  habilidades: [],
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
  let atualizarRecursos: ReturnType<typeof vi.fn>;

  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  /** Resposta do servidor depois de um PATCH: a ficha com os atuais trocados. */
  function responder(atuais: Record<string, number>): FichaOrdem {
    const recursos = { ...FICHA.recursos };
    for (const [codigo, atual] of Object.entries(atuais)) {
      const chave = codigo as keyof typeof recursos;
      recursos[chave] = { ...recursos[chave], atual };
    }
    return { ...FICHA, recursos };
  }

  function montar(respostas: ReturnType<typeof vi.fn>, ficha: FichaOrdem | FichaDnd = FICHA) {
    atualizarRecursos = respostas;
    TestBed.configureTestingModule({
      imports: [RecursosFicha],
      providers: [{ provide: FichaApi, useValue: { atualizarRecursos, versaoFichas: signal(0) } }],
    });
    const fixture = TestBed.createComponent(RecursosFicha);
    fixture.componentRef.setInput('ficha', ficha);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  function clicar(fixture: ComponentFixture<RecursosFicha>, rotulo: string, vezes = 1): void {
    for (let i = 0; i < vezes; i++) {
      (fixture.nativeElement.querySelector(`button[aria-label="${rotulo}"]`) as HTMLButtonElement).click();
      fixture.detectChanges();
    }
  }

  it('tem os tres aneis, cada um com - e + de nome acessivel', () => {
    const { el } = montar(vi.fn());
    for (const nome of ['vida', 'sanidade', 'esforço']) {
      expect(el.querySelector(`button[aria-label="Diminuir ${nome}"]`)).not.toBeNull();
      expect(el.querySelector(`button[aria-label="Aumentar ${nome}"]`)).not.toBeNull();
    }
    expect(el.querySelectorAll('app-anel-status').length).toBe(3);
  });

  it('tres toques em Aumentar vida viram um PATCH so, 800 ms depois, so com o codigo PV', () => {
    const { fixture, el } = montar(vi.fn(() => of(responder({ PV: 9 }))));

    clicar(fixture, 'Aumentar vida', 3);
    expect(el.textContent).toContain('9/12');
    vi.advanceTimersByTime(799);
    expect(atualizarRecursos).not.toHaveBeenCalled();
    vi.advanceTimersByTime(1);

    expect(atualizarRecursos).toHaveBeenCalledTimes(1);
    expect(atualizarRecursos).toHaveBeenCalledWith(7, { PV: 9 });
  });

  it('o PATCH leva todos os codigos mudados dentro da janela, e so eles', () => {
    const { fixture } = montar(vi.fn(() => of(responder({ PV: 5, PE: 6 }))));

    clicar(fixture, 'Diminuir vida');
    clicar(fixture, 'Aumentar esforço');
    vi.advanceTimersByTime(800);

    expect(atualizarRecursos).toHaveBeenCalledWith(7, { PV: 5, PE: 6 });
  });

  it('esforco sem maximo: o + continua habilitado e o PATCH leva so o PE', () => {
    const semMaximo: FichaOrdem = { ...FICHA, recursos: { ...FICHA.recursos, PE: { atual: 5, maximo: null } } };
    const { fixture } = montar(vi.fn(() => of(responder({ PE: 7 }))), semMaximo);
    const mais = () => fixture.nativeElement.querySelector('button[aria-label="Aumentar esforço"]') as HTMLButtonElement;

    const nomeEditar = () =>
      fixture.nativeElement.querySelector('button[aria-label^="Editar esforço"]')?.getAttribute('aria-label');

    expect(nomeEditar()).toContain('5');
    expect(fixture.nativeElement.textContent).not.toContain('—');
    expect(mais().disabled).toBe(false);
    clicar(fixture, 'Aumentar esforço', 2);
    expect(mais().disabled).toBe(false);
    expect(nomeEditar()).toContain('7');
    vi.advanceTimersByTime(800);

    expect(atualizarRecursos).toHaveBeenCalledWith(7, { PE: 7 });
  });

  it('esforco vazio (atual e maximo nulos) tambem pode subir', () => {
    const vazio: FichaOrdem = { ...FICHA, recursos: { ...FICHA.recursos, PE: { atual: null, maximo: null } } };
    const { fixture } = montar(vi.fn(() => of(responder({ PE: 1 }))), vazio);

    clicar(fixture, 'Aumentar esforço');
    vi.advanceTimersByTime(800);

    expect(atualizarRecursos).toHaveBeenCalledWith(7, { PE: 1 });
  });

  it('se o salvamento falhar, volta ao ultimo valor salvo e mostra o erro', () => {
    const erro: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'Tente de novo.', campos: {} };
    const { fixture, el } = montar(vi.fn(() => throwError(() => erro)));

    clicar(fixture, 'Diminuir vida', 2);
    vi.advanceTimersByTime(800);
    fixture.detectChanges();

    expect(el.textContent).toContain('6/12');
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Servidor indisponível: Tente de novo.');
  });

  it('um toque durante o salvamento salva de novo depois, sem ser apagado pela resposta', () => {
    const primeira = new Subject<FichaOrdem>();
    const { fixture, el } = montar(
      vi.fn().mockReturnValueOnce(primeira).mockReturnValueOnce(of(responder({ PV: 8 }))),
    );

    clicar(fixture, 'Aumentar vida');
    vi.advanceTimersByTime(800);
    fixture.detectChanges();
    expect(atualizarRecursos).toHaveBeenCalledTimes(1);
    expect(el.querySelector('[role="status"]')?.textContent).toContain('salvando');

    clicar(fixture, 'Aumentar vida');
    vi.advanceTimersByTime(800);
    primeira.next(responder({ PV: 7 }));
    primeira.complete();
    fixture.detectChanges();

    expect(atualizarRecursos).toHaveBeenCalledTimes(2);
    expect(atualizarRecursos).toHaveBeenNthCalledWith(2, 7, { PV: 8 });
    expect(el.textContent).toContain('8/12');
  });

  it('sair da ficha com um ajuste esperando salva na hora', () => {
    const { fixture } = montar(vi.fn(() => of(responder({ SAN: 47 }))));

    clicar(fixture, 'Diminuir sanidade');
    fixture.destroy();

    expect(atualizarRecursos).toHaveBeenCalledTimes(1);
    expect(atualizarRecursos).toHaveBeenCalledWith(7, { SAN: 47 });
  });

  it('sem ajuste nenhum, sair da ficha nao chama a API', () => {
    const { fixture } = montar(vi.fn());
    fixture.destroy();
    expect(atualizarRecursos).not.toHaveBeenCalled();
  });

  it('digitar o valor e confirmar com Enter salva o numero, limitado ao maximo', () => {
    const { fixture, el } = montar(vi.fn(() => of(responder({ SAN: 5 }))));

    (el.querySelector('button[aria-label^="Editar sanidade"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    const campo = el.querySelector('input[type="number"]') as HTMLInputElement;
    campo.value = '5';
    campo.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
    fixture.detectChanges();
    vi.advanceTimersByTime(800);

    expect(atualizarRecursos).toHaveBeenCalledWith(7, { SAN: 5 });
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
    expect(atualizarRecursos).not.toHaveBeenCalled();
  });

  it('os botoes respeitam os limites: - desabilitado em 0 e + desabilitado no maximo', () => {
    const limites: FichaOrdem = {
      ...FICHA,
      recursos: { ...FICHA.recursos, PV: { atual: 0, maximo: 12 }, SAN: { atual: 60, maximo: 60 } },
    };
    const { el } = montar(vi.fn(), limites);

    expect((el.querySelector('button[aria-label="Diminuir vida"]') as HTMLButtonElement).disabled).toBe(true);
    expect((el.querySelector('button[aria-label="Aumentar sanidade"]') as HTMLButtonElement).disabled).toBe(true);
    expect((el.querySelector('button[aria-label="Aumentar vida"]') as HTMLButtonElement).disabled).toBe(false);
  });

  describe('em D&D', () => {
    it('tem dois aneis: Vida e Temporario', () => {
      const { el } = montar(vi.fn(), FICHA_DND);
      expect(el.querySelectorAll('app-anel-status').length).toBe(2);
      for (const nome of ['vida', 'temporário']) {
        expect(el.querySelector(`button[aria-label="Diminuir ${nome}"]`)).not.toBeNull();
        expect(el.querySelector(`button[aria-label="Aumentar ${nome}"]`)).not.toBeNull();
      }
      expect(el.querySelector('button[aria-label="Aumentar sanidade"]')).toBeNull();
    });

    it('PV temporario sem maximo: o + continua habilitado e 2 toques viram um PATCH so com PV_TEMP', () => {
      const resposta: FichaDnd = { ...FICHA_DND, recursos: { ...FICHA_DND.recursos, PV_TEMP: { atual: 6, maximo: null } } };
      const { fixture } = montar(vi.fn(() => of(resposta)), FICHA_DND);
      const mais = () => fixture.nativeElement.querySelector('button[aria-label="Aumentar temporário"]') as HTMLButtonElement;

      expect(mais().disabled).toBe(false);
      clicar(fixture, 'Aumentar temporário', 2);
      expect(mais().disabled).toBe(false);
      expect(
        fixture.nativeElement.querySelector('button[aria-label^="Editar temporário"]')?.getAttribute('aria-label'),
      ).toContain('6');
      vi.advanceTimersByTime(800);

      expect(atualizarRecursos).toHaveBeenCalledTimes(1);
      expect(atualizarRecursos).toHaveBeenCalledWith(9, { PV_TEMP: 6 });
    });

    it('a vida de D&D respeita o maximo e o PATCH leva so o PV', () => {
      const { fixture } = montar(vi.fn(() => of(FICHA_DND)), FICHA_DND);
      clicar(fixture, 'Diminuir vida', 2);
      vi.advanceTimersByTime(800);
      expect(atualizarRecursos).toHaveBeenCalledWith(9, { PV: 28 });
    });

    it('a resposta de D&D tambem emite atualizada', () => {
      const resposta: FichaDnd = { ...FICHA_DND, recursos: { ...FICHA_DND.recursos, PV: { atual: 29, maximo: 38 } } };
      const { fixture } = montar(vi.fn(() => of(resposta)), FICHA_DND);
      const emitidas: unknown[] = [];
      fixture.componentInstance.atualizada.subscribe((f) => emitidas.push(f));

      clicar(fixture, 'Diminuir vida');
      vi.advanceTimersByTime(800);

      expect(emitidas).toEqual([resposta]);
    });

    it('erro no salvamento volta o PV temporario ao ultimo valor salvo', () => {
      const erro: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'Tente de novo.', campos: {} };
      const { fixture, el } = montar(vi.fn(() => throwError(() => erro)), FICHA_DND);
      clicar(fixture, 'Aumentar temporário', 3);
      vi.advanceTimersByTime(800);
      fixture.detectChanges();
      expect(el.querySelector('button[aria-label^="Editar temporário"]')?.getAttribute('aria-label')).toContain('4');
    });
  });
});
