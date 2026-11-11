import { TestBed } from '@angular/core/testing';
import { DadoAnimado } from './dado-animado';
import { CARREGADOR_DO_LANCADOR, LancadorDeDados } from './lancador-de-dados';

function lancadorFalso(): LancadorDeDados {
  return { lancar: vi.fn(() => Promise.resolve()), encerrar: vi.fn() };
}

/** Monta o serviço com o carregador falso; `movimentoReduzido` e `webgl` simulam o navegador. */
function montar(opcoes: { movimentoReduzido?: boolean; webgl?: 'webgl2' | 'webgl' | null; carregador?: () => Promise<unknown> } = {}) {
  const { movimentoReduzido = false, webgl = 'webgl2' } = opcoes;
  vi.stubGlobal('matchMedia', (consulta: string) => ({
    matches: movimentoReduzido && consulta.includes('prefers-reduced-motion'),
  }));
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockImplementation(((tipo: string) =>
    tipo === webgl ? {} : null) as never);
  const criar = vi.fn(() => lancadorFalso());
  const carregador = vi.fn(opcoes.carregador ?? (() => Promise.resolve({ criarLancador3d: criar })));
  TestBed.configureTestingModule({ providers: [{ provide: CARREGADOR_DO_LANCADOR, useValue: carregador }] });
  return { servico: TestBed.inject(DadoAnimado), carregador, criar };
}

describe('DadoAnimado', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('devolve o lancador quando esta tudo bem', async () => {
    const { servico, criar } = montar();

    const lancador = await servico.preparar();

    expect(lancador).not.toBeNull();
    expect(criar).toHaveBeenCalledOnce();
  });

  it('devolve null com movimento reduzido, sem nem carregar o pedaco 3D', async () => {
    const { servico, carregador } = montar({ movimentoReduzido: true });

    expect(await servico.preparar()).toBeNull();
    expect(carregador).not.toHaveBeenCalled();
  });

  it('devolve null sem WebGL (nem webgl2 nem webgl)', async () => {
    const { servico, carregador } = montar({ webgl: null });

    expect(await servico.preparar()).toBeNull();
    expect(carregador).not.toHaveBeenCalled();
  });

  it('aceita so o webgl 1', async () => {
    const { servico } = montar({ webgl: 'webgl' });

    expect(await servico.preparar()).not.toBeNull();
  });

  it('devolve null e registra no console quando o import rejeita', async () => {
    const erro = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const { servico } = montar({ carregador: () => Promise.reject(new Error('rede')) });

    expect(await servico.preparar()).toBeNull();
    expect(erro).toHaveBeenCalled();
  });

  it('o import acontece uma vez so em duas rolagens, com um lancador novo em cada', async () => {
    const { servico, carregador, criar } = montar();

    const a = await servico.preparar();
    const b = await servico.preparar();

    expect(carregador).toHaveBeenCalledOnce();
    expect(criar).toHaveBeenCalledTimes(2);
    expect(a).not.toBe(b);
  });
});
