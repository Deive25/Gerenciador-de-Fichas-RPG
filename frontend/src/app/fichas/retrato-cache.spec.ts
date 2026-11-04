import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { FichaApi } from './ficha-api';
import { RetratoCache } from './retrato-cache';

describe('RetratoCache', () => {
  let baixarRetrato: ReturnType<typeof vi.fn>;
  let criar: ReturnType<typeof vi.fn>;
  let revogar: ReturnType<typeof vi.fn>;
  const originais = { criar: URL.createObjectURL, revogar: URL.revokeObjectURL };

  beforeEach(() => {
    let n = 0;
    // O jsdom não implementa createObjectURL/revokeObjectURL.
    criar = vi.fn(() => `blob:${++n}`);
    revogar = vi.fn();
    Object.defineProperty(URL, 'createObjectURL', { value: criar, configurable: true, writable: true });
    Object.defineProperty(URL, 'revokeObjectURL', { value: revogar, configurable: true, writable: true });
    baixarRetrato = vi.fn(() => of(new Blob(['jpeg'])));
    TestBed.configureTestingModule({ providers: [{ provide: FichaApi, useValue: { baixarRetrato } }] });
  });

  afterEach(() => {
    Object.defineProperty(URL, 'createObjectURL', { value: originais.criar, configurable: true, writable: true });
    Object.defineProperty(URL, 'revokeObjectURL', { value: originais.revogar, configurable: true, writable: true });
  });

  function urlDe(cache: RetratoCache, fichaId: number, versao: number): string | undefined {
    let url: string | undefined;
    cache.url(fichaId, versao).subscribe((u) => (url = u));
    return url;
  }

  it('a mesma versao baixa uma vez so e devolve a mesma URL', () => {
    const cache = TestBed.inject(RetratoCache);
    expect(urlDe(cache, 7, 100)).toBe('blob:1');
    expect(urlDe(cache, 7, 100)).toBe('blob:1');
    expect(baixarRetrato).toHaveBeenCalledTimes(1);
    expect(baixarRetrato).toHaveBeenCalledWith(7, 100);
  });

  it('uma versao nova baixa de novo e revoga a URL antiga (sem acumular memoria)', () => {
    const cache = TestBed.inject(RetratoCache);
    urlDe(cache, 7, 100);
    expect(urlDe(cache, 7, 200)).toBe('blob:2');
    expect(baixarRetrato).toHaveBeenCalledTimes(2);
    expect(revogar).toHaveBeenCalledWith('blob:1');
  });

  it('esquecer revoga a URL da ficha', () => {
    const cache = TestBed.inject(RetratoCache);
    urlDe(cache, 7, 100);
    cache.esquecer(7);
    expect(revogar).toHaveBeenCalledWith('blob:1');
    urlDe(cache, 7, 100);
    expect(baixarRetrato).toHaveBeenCalledTimes(2);
  });
});
