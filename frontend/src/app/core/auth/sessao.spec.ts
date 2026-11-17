import { TestBed } from '@angular/core/testing';
import { Sessao } from './sessao';
import { respostaDe } from './teste-auth';

describe('Sessao', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
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

  describe('conta demo (por aba)', () => {
    const CHAVE = 'fichas-rpg.refresh';

    it('iniciar como demo guarda o refresh no sessionStorage e não no localStorage', () => {
      const s = TestBed.inject(Sessao);
      s.iniciar(respostaDe('d'), true);
      expect(sessionStorage.getItem(CHAVE)).toBe('refresh-d');
      expect(localStorage.getItem(CHAVE)).toBeNull();
      expect(s.refreshGuardado()).toBe('refresh-d');
      expect(s.usuario()?.demo).toBe(true);
    });

    it('conta normal continua no localStorage e não é demo', () => {
      const s = TestBed.inject(Sessao);
      s.iniciar(respostaDe());
      expect(localStorage.getItem(CHAVE)).toBe('refresh-1');
      expect(sessionStorage.getItem(CHAVE)).toBeNull();
      expect(s.usuario()?.demo).toBeFalsy();
    });

    it('entrar numa conta normal apaga o refresh demo da aba', () => {
      const s = TestBed.inject(Sessao);
      s.iniciar(respostaDe('d'), true);
      s.iniciar(respostaDe('n'), false);
      expect(sessionStorage.getItem(CHAVE)).toBeNull();
      expect(localStorage.getItem(CHAVE)).toBe('refresh-n');
      expect(s.refreshGuardado()).toBe('refresh-n');
    });

    it('entrar numa conta demo não derruba a conta normal das outras abas', () => {
      const s = TestBed.inject(Sessao);
      s.iniciar(respostaDe('n'));
      s.iniciar(respostaDe('d'), true);
      expect(localStorage.getItem(CHAVE)).toBe('refresh-n');
      expect(sessionStorage.getItem(CHAVE)).toBe('refresh-d');
      expect(s.refreshGuardado()).toBe('refresh-d');
    });

    it('sair da demo apaga só o refresh da aba', () => {
      const s = TestBed.inject(Sessao);
      s.iniciar(respostaDe('n'));
      s.iniciar(respostaDe('d'), true);
      s.limpar();
      expect(sessionStorage.getItem(CHAVE)).toBeNull();
      expect(localStorage.getItem(CHAVE)).toBe('refresh-n');
    });

    it('sair da demo depois de recarregar (só o refresh da aba) também preserva a conta normal', () => {
      localStorage.setItem(CHAVE, 'normal');
      sessionStorage.setItem(CHAVE, 'demo');
      TestBed.inject(Sessao).limpar();
      expect(sessionStorage.getItem(CHAVE)).toBeNull();
      expect(localStorage.getItem(CHAVE)).toBe('normal');
    });

    it('sair da conta normal apaga os dois', () => {
      const s = TestBed.inject(Sessao);
      s.iniciar(respostaDe('n'));
      sessionStorage.setItem(CHAVE, 'resto');
      s.limpar();
      expect(localStorage.getItem(CHAVE)).toBeNull();
      expect(sessionStorage.getItem(CHAVE)).toBeNull();
    });

    it('o refresh demo da aba tem prioridade ao ler', () => {
      localStorage.setItem(CHAVE, 'normal');
      sessionStorage.setItem(CHAVE, 'demo');
      expect(TestBed.inject(Sessao).refreshGuardado()).toBe('demo');
    });

    it('ehDemo: vale pela sessão em memória e, depois de recarregar, pelo refresh da aba', () => {
      const s = TestBed.inject(Sessao);
      expect(s.ehDemo()).toBe(false);
      sessionStorage.setItem(CHAVE, 'demo');
      expect(s.ehDemo()).toBe(true);
      s.iniciar(respostaDe('n'), false);
      expect(s.ehDemo()).toBe(false);
      s.iniciar(respostaDe('d'), true);
      expect(s.ehDemo()).toBe(true);
    });

    it('com o sessionStorage indisponível nada quebra', () => {
      const falha = () => {
        throw new DOMException('bloqueado', 'SecurityError');
      };
      vi.spyOn(Storage.prototype, 'getItem').mockImplementation(falha);
      vi.spyOn(Storage.prototype, 'setItem').mockImplementation(falha);
      vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(falha);
      const s = TestBed.inject(Sessao);
      expect(() => s.iniciar(respostaDe('d'), true)).not.toThrow();
      expect(s.usuario()?.demo).toBe(true);
      expect(() => s.limpar()).not.toThrow();
    });
  });
});
