import { TestBed } from '@angular/core/testing';
import { GoogleIdentity } from './google-identity';

const scripts = () => document.head.querySelectorAll('script[src="https://accounts.google.com/gsi/client"]');

describe('GoogleIdentity', () => {
  let servico: GoogleIdentity;

  beforeEach(() => {
    scripts().forEach((s) => s.remove());
    servico = TestBed.inject(GoogleIdentity);
  });
  afterEach(() => scripts().forEach((s) => s.remove()));

  it('injeta o script uma vez só', () => {
    void servico.carregar().catch(() => undefined);
    void servico.carregar().catch(() => undefined);
    expect(scripts().length).toBe(1);
  });

  it('resolve com google.accounts.id quando o script carrega', async () => {
    const id = { initialize: vi.fn(), renderButton: vi.fn() };
    (window as unknown as { google: unknown }).google = { accounts: { id } };
    const promessa = servico.carregar();
    scripts()[0].dispatchEvent(new Event('load'));
    await expect(promessa).resolves.toBe(id);
    delete (window as unknown as { google?: unknown }).google;
  });

  it('rejeita quando o script falha', async () => {
    const promessa = servico.carregar();
    scripts()[0].dispatchEvent(new Event('error'));
    await expect(promessa).rejects.toThrow();
  });
});
