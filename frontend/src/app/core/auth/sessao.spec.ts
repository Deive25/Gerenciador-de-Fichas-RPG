import { TestBed } from '@angular/core/testing';
import { Sessao } from './sessao';
import { respostaDe } from './teste-auth';

describe('Sessao', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.restoreAllMocks());

  it('começa deslogada', () => {
    const s = TestBed.inject(Sessao);
    expect(s.estaLogado()).toBe(false);
    expect(s.usuario()).toBeNull();
    expect(s.tokenDeAcesso()).toBeNull();
  });

  it('iniciar guarda o usuário e o acesso na memória e só o refresh no localStorage', () => {
    const s = TestBed.inject(Sessao);
    s.iniciar(respostaDe());
    expect(s.estaLogado()).toBe(true);
    expect(s.usuario()?.nome).toBe('Ana');
    expect(s.tokenDeAcesso()).toBe('acesso-1');
    expect(s.refreshGuardado()).toBe('refresh-1');
    expect(localStorage.getItem('fichas-rpg.refresh')).toBe('refresh-1');
    expect(JSON.stringify({ ...localStorage })).not.toContain('acesso-1');
  });

  it('limpar apaga tudo', () => {
    const s = TestBed.inject(Sessao);
    s.iniciar(respostaDe());
    s.limpar();
    expect(s.estaLogado()).toBe(false);
    expect(s.tokenDeAcesso()).toBeNull();
    expect(localStorage.getItem('fichas-rpg.refresh')).toBeNull();
  });

  it('com o localStorage indisponível nada quebra', () => {
    const s = TestBed.inject(Sessao);
    const falha = () => {
      throw new DOMException('bloqueado', 'SecurityError');
    };
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(falha);
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(falha);
    vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(falha);
    expect(() => s.iniciar(respostaDe())).not.toThrow();
    expect(s.estaLogado()).toBe(true);
    expect(s.refreshGuardado()).toBeNull();
    expect(() => s.limpar()).not.toThrow();
    expect(s.estaLogado()).toBe(false);
  });
});
