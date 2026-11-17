import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { EstadoDoServidor } from './estado-do-servidor';

const LIVENESS = `${environment.apiUrl}/actuator/health/liveness`;

describe('EstadoDoServidor', () => {
  let servidor: EstadoDoServidor;
  let http: HttpTestingController;

  /** Pedidos de liveness ainda vivos (os cancelados por uma nova tentativa não contam). */
  const abertos = (): TestRequest[] => http.match(LIVENESS).filter((r) => !r.cancelled);

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servidor = TestBed.inject(EstadoDoServidor);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => vi.useRealTimers());

  it('antes de iniciar não faz pedido e não bloqueia nada', () => {
    expect(servidor.estado()).toBe('verificando');
    expect(servidor.aguardando()).toBe(false);
    http.expectNone(LIVENESS);
  });

  it('ao iniciar chama o liveness sem token', () => {
    servidor.iniciar();
    const req = http.expectOne(LIVENESS);
    expect(req.request.method).toBe('GET');
    expect(req.request.headers.has('Authorization')).toBe(false);
  });

  it('responde antes de 3 s: nunca mostra a faixa', () => {
    servidor.iniciar();
    vi.advanceTimersByTime(1000);
    http.expectOne(LIVENESS).flush({ status: 'UP' });
    expect(servidor.estado()).toBe('pronto');
    vi.advanceTimersByTime(10_000);
    expect(servidor.estado()).toBe('pronto');
    expect(abertos()).toHaveLength(0);
  });

  it('sem resposta em 3 s, a faixa aparece e os botões ficam aguardando', () => {
    servidor.iniciar();
    vi.advanceTimersByTime(2999);
    expect(servidor.estado()).toBe('verificando');
    expect(servidor.aguardando()).toBe(false);
    vi.advanceTimersByTime(1);
    expect(servidor.estado()).toBe('acordando');
    expect(servidor.aguardando()).toBe(true);
    expect(servidor.segundos()).toBe(3);
  });

  it('o contador de segundos avança de 1 em 1', () => {
    servidor.iniciar();
    vi.advanceTimersByTime(10_000);
    expect(servidor.segundos()).toBe(10);
  });

  it('tenta de novo a cada 5 s, sem acumular pedidos pendentes', () => {
    servidor.iniciar();
    vi.advanceTimersByTime(5000);
    expect(abertos()).toHaveLength(1);
    vi.advanceTimersByTime(5000);
    expect(abertos()).toHaveLength(1);
  });

  it('a faixa some quando a API responde e para de tentar', () => {
    servidor.iniciar();
    vi.advanceTimersByTime(7000);
    expect(servidor.estado()).toBe('acordando');
    const [req] = http.match(LIVENESS).filter((r) => !r.cancelled);
    req.flush({ status: 'UP' });
    expect(servidor.estado()).toBe('pronto');
    expect(servidor.aguardando()).toBe(false);
    vi.advanceTimersByTime(30_000);
    expect(abertos()).toHaveLength(0);
  });

  it('erro (503 ou rede) não encerra: continua acordando e tenta de novo', () => {
    servidor.iniciar();
    vi.advanceTimersByTime(4000);
    http.expectOne(LIVENESS).flush(null, { status: 503, statusText: 'Service Unavailable' });
    expect(servidor.estado()).toBe('acordando');
    vi.advanceTimersByTime(1000);
    expect(abertos()).toHaveLength(1);
  });

  it('desiste em 3 min: mostra que o servidor não respondeu e para de tentar', () => {
    servidor.iniciar();
    vi.advanceTimersByTime(179_000);
    expect(servidor.estado()).toBe('acordando');
    vi.advanceTimersByTime(1000);
    expect(servidor.estado()).toBe('falhou');
    expect(servidor.aguardando()).toBe(true);
    http.match(LIVENESS);
    vi.advanceTimersByTime(30_000);
    expect(abertos()).toHaveLength(0);
  });

  it('quandoLiberado resolve na hora se o servidor não está aguardando', async () => {
    await expect(servidor.quandoLiberado()).resolves.toBeUndefined();
  });

  it('quandoLiberado espera a API responder', async () => {
    servidor.iniciar();
    vi.advanceTimersByTime(4000);
    let liberou = false;
    void servidor.quandoLiberado().then(() => (liberou = true));
    await Promise.resolve();
    expect(liberou).toBe(false);
    http.expectOne(LIVENESS).flush({ status: 'UP' });
    await Promise.resolve();
    expect(liberou).toBe(true);
  });

  it('iniciar duas vezes não duplica o pedido', () => {
    servidor.iniciar();
    servidor.iniciar();
    expect(http.match(LIVENESS)).toHaveLength(1);
  });
});
