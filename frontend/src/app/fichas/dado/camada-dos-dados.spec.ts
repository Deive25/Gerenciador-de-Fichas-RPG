import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CamadaDosDados } from './camada-dos-dados';
import { LancadorDeDados } from './lancador-de-dados';

const DADOS = [3, 17, 9];

function montar(lancar: LancadorDeDados['lancar']) {
  const lancador: LancadorDeDados = { lancar: vi.fn(lancar), encerrar: vi.fn() };
  const fixture: ComponentFixture<CamadaDosDados> = TestBed.createComponent(CamadaDosDados);
  fixture.componentRef.setInput('lancador', lancador);
  fixture.componentRef.setInput('dados', DADOS);
  fixture.componentRef.setInput('indiceMantido', 1);
  const terminou = vi.fn();
  fixture.componentInstance.terminou.subscribe(terminou);
  fixture.detectChanges();
  return { fixture, lancador, terminou, el: fixture.nativeElement as HTMLElement };
}

/** Um lancar que só resolve quando o teste mandar (os dados "param"). */
function pendente() {
  let parar!: () => void;
  const promessa = new Promise<void>((resolve) => (parar = resolve));
  return { lancar: () => promessa, parar };
}

describe('CamadaDosDados', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('lanca exatamente os dados no palco, com o indice do mantido', () => {
    const { lancador, el } = montar(() => new Promise(() => undefined));

    expect(lancador.lancar).toHaveBeenCalledOnce();
    expect(lancador.lancar).toHaveBeenCalledWith(el.querySelector('.palco'), DADOS, 1);
  });

  it('so emite terminou depois que lancar resolve e passam 0,6 s', async () => {
    const { lancar, parar } = pendente();
    const { terminou } = montar(lancar);

    await vi.advanceTimersByTimeAsync(1000);
    expect(terminou).not.toHaveBeenCalled();

    parar();
    await vi.advanceTimersByTimeAsync(599);
    expect(terminou).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(1);
    expect(terminou).toHaveBeenCalledOnce();
  });

  it('ao terminar, libera a cena', async () => {
    const { lancar, parar } = pendente();
    const { lancador } = montar(lancar);

    parar();
    await vi.advanceTimersByTimeAsync(600);

    expect(lancador.encerrar).toHaveBeenCalled();
  });

  it('o toque na camada encerra e emite na hora', () => {
    const { lancador, terminou, el } = montar(() => new Promise(() => undefined));

    el.click();

    expect(lancador.encerrar).toHaveBeenCalledOnce();
    expect(terminou).toHaveBeenCalledOnce();
  });

  it('o Esc, no document, encerra e emite na hora', () => {
    const { lancador, terminou } = montar(() => new Promise(() => undefined));

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(lancador.encerrar).toHaveBeenCalledOnce();
    expect(terminou).toHaveBeenCalledOnce();
  });

  it('outra tecla nao faz nada', () => {
    const { terminou } = montar(() => new Promise(() => undefined));

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

    expect(terminou).not.toHaveBeenCalled();
  });

  it('emite uma vez so, mesmo com toque, Esc e fim dos dados juntos', async () => {
    const { lancar, parar } = pendente();
    const { terminou, el } = montar(lancar);

    parar();
    el.click();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    await vi.advanceTimersByTimeAsync(10_000);

    expect(terminou).toHaveBeenCalledOnce();
  });

  it('com limite de 5 s, emite mesmo sem lancar resolver', async () => {
    const { terminou, lancador } = montar(() => new Promise(() => undefined));

    await vi.advanceTimersByTimeAsync(4999);
    expect(terminou).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(1);
    expect(terminou).toHaveBeenCalledOnce();
    expect(lancador.encerrar).toHaveBeenCalled();
  });

  it('se lancar rejeitar, emite e registra no console', async () => {
    const erro = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const { terminou } = montar(() => Promise.reject(new Error('fisica quebrou')));

    await vi.advanceTimersByTimeAsync(0);

    expect(terminou).toHaveBeenCalledOnce();
    expect(erro).toHaveBeenCalled();
    erro.mockRestore();
  });

  it('se lancar lancar uma excecao na hora, tambem emite e registra', async () => {
    const erro = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const { terminou } = montar(() => {
      throw new Error('sem contexto');
    });

    await vi.advanceTimersByTimeAsync(0);

    expect(terminou).toHaveBeenCalledOnce();
    expect(erro).toHaveBeenCalled();
    erro.mockRestore();
  });

  it('destruir a camada encerra a cena sem emitir e cancela os tempos', async () => {
    const { fixture, lancador, terminou } = montar(() => new Promise(() => undefined));

    fixture.destroy();
    await vi.advanceTimersByTimeAsync(10_000);

    expect(lancador.encerrar).toHaveBeenCalledOnce();
    expect(terminou).not.toHaveBeenCalled();
  });

  it('e invisivel para leitor de tela e avisa como pular', () => {
    const { el } = montar(() => new Promise(() => undefined));

    expect(el.getAttribute('aria-hidden')).toBe('true');
    expect(el.textContent).toContain('Toque para pular');
  });

  it('cobre a tela inteira (fixed), acima do cabecalho (z-index 10), com o veu leve', () => {
    const { el } = montar(() => new Promise(() => undefined));
    const estilo = getComputedStyle(el);

    expect(estilo.position).toBe('fixed');
    expect(Number(estilo.zIndex)).toBeGreaterThan(10);
    expect(estilo.backgroundColor).toBe('rgba(11, 10, 15, 0.35)');
  });
});
