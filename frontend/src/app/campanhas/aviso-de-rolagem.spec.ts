import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Observable, Subject } from 'rxjs';
import { Sessao } from '../core/auth/sessao';
import { respostaDe } from '../core/auth/teste-auth';
import { AvisoDeRolagem } from './aviso-de-rolagem';
import { MesaAoVivo } from './mesa-ao-vivo';
import { RolagemDaMesa } from './rolagem-da-mesa.model';

// A sessão de teste é a da Ana (id 7).
const ANA = { id: 7, nome: 'Ana' };
const LIA = { id: 8, nome: 'Lía' };

function rolagem(id: number, extra: Partial<RolagemDaMesa> = {}): RolagemDaMesa {
  return {
    id, campanhaId: 3, criadaEm: new Date().toISOString(), autor: LIA, personagem: 'Kai', doMestre: false,
    tipo: 'TESTE', rotulo: 'Furtividade', total: 25,
    detalhe: { alvo: 'PERICIA', codigo: 'FURTIVIDADE', nome: 'Furtividade', dados: [17, 6], mantido: 17, bonus: 8, total: 25 },
    ...extra,
  } as RolagemDaMesa;
}

@Component({
  imports: [AvisoDeRolagem],
  template: `<app-aviso-de-rolagem [campanhaId]="campanhaId()" [painelAberto]="painelAberto()" (abrir)="abriu = abriu + 1" />`,
})
class Hospedeiro {
  campanhaId = signal(3);
  painelAberto = signal(false);
  abriu = 0;
}

describe('AvisoDeRolagem', () => {
  let vivas: Map<number, Subject<RolagemDaMesa>>;
  let ativas: Set<number>;

  function montar() {
    localStorage.clear();
    vivas = new Map();
    ativas = new Set();
    TestBed.configureTestingModule({
      providers: [
        {
          provide: MesaAoVivo,
          useValue: {
            rolagens$: (id: number) =>
              new Observable<RolagemDaMesa>((assinante) => {
                const s = new Subject<RolagemDaMesa>();
                vivas.set(id, s);
                ativas.add(id);
                const sub = s.subscribe(assinante);
                return () => {
                  sub.unsubscribe();
                  ativas.delete(id);
                };
              }),
          },
        },
      ],
    });
    TestBed.inject(Sessao).iniciar(respostaDe());
    const fixture = TestBed.createComponent(Hospedeiro);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    return { fixture, el, host: fixture.componentInstance };
  }

  const aviso = (el: HTMLElement) => el.querySelector('.aviso-rolagem') as HTMLElement | null;

  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('a região aria-live existe desde o começo, vazia', () => {
    const { el } = montar();
    const regiao = el.querySelector('[aria-live="polite"]') as HTMLElement;
    expect(regiao).not.toBeNull();
    expect(regiao.textContent?.trim()).toBe('');
  });

  it('mostra a rolagem de outra pessoa quando o painel está fechado', () => {
    const { fixture, el } = montar();
    vivas.get(3)!.next(rolagem(41));
    fixture.detectChanges();
    expect(aviso(el)?.textContent?.trim()).toBe('Lía · Kai · Furtividade: 25');
  });

  it('a rolagem do mestre aparece como "Mestre", sem o nome do personagem', () => {
    const { fixture, el } = montar();
    vivas.get(3)!.next(rolagem(41, { autor: LIA, personagem: null, doMestre: true }));
    fixture.detectChanges();
    expect(aviso(el)?.textContent?.trim()).toBe('Mestre · Furtividade: 25');
  });

  it('não avisa da própria rolagem', () => {
    const { fixture, el } = montar();
    vivas.get(3)!.next(rolagem(41, { autor: ANA }));
    fixture.detectChanges();
    expect(aviso(el)).toBeNull();
  });

  it('não avisa com o painel aberto', () => {
    const { fixture, el, host } = montar();
    host.painelAberto.set(true);
    fixture.detectChanges();
    vivas.get(3)!.next(rolagem(41));
    fixture.detectChanges();
    expect(aviso(el)).toBeNull();
  });

  it('o aviso some em 4 s', () => {
    const { fixture, el } = montar();
    vivas.get(3)!.next(rolagem(41));
    fixture.detectChanges();
    vi.advanceTimersByTime(3999);
    fixture.detectChanges();
    expect(aviso(el)).not.toBeNull();
    vi.advanceTimersByTime(1);
    fixture.detectChanges();
    expect(aviso(el)).toBeNull();
  });

  it('uma rolagem nova troca o aviso e reinicia os 4 s', () => {
    const { fixture, el } = montar();
    vivas.get(3)!.next(rolagem(41));
    vi.advanceTimersByTime(3000);
    vivas.get(3)!.next(rolagem(42, { rotulo: 'Atletismo', total: 9 }));
    fixture.detectChanges();
    expect(aviso(el)?.textContent?.trim()).toBe('Lía · Kai · Atletismo: 9');
    vi.advanceTimersByTime(3000);
    fixture.detectChanges();
    expect(aviso(el)).not.toBeNull();
    vi.advanceTimersByTime(1000);
    fixture.detectChanges();
    expect(aviso(el)).toBeNull();
  });

  it('tocar no aviso pede para abrir o painel e o aviso some', () => {
    const { fixture, el, host } = montar();
    vivas.get(3)!.next(rolagem(41));
    fixture.detectChanges();
    (aviso(el) as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(host.abriu).toBe(1);
    expect(aviso(el)).toBeNull();
  });

  it('abrir o painel com um aviso na tela esconde o aviso', () => {
    const { fixture, el, host } = montar();
    vivas.get(3)!.next(rolagem(41));
    fixture.detectChanges();
    host.painelAberto.set(true);
    fixture.detectChanges();
    expect(aviso(el)).toBeNull();
  });

  it('trocar de campanha cancela a inscrição antiga e some com o aviso', () => {
    const { fixture, el, host } = montar();
    vivas.get(3)!.next(rolagem(41));
    fixture.detectChanges();
    host.campanhaId.set(4);
    fixture.detectChanges();
    expect(ativas).toEqual(new Set([4]));
    expect(aviso(el)).toBeNull();
  });

  it('ao destruir, cancela a inscrição', () => {
    const { fixture } = montar();
    fixture.destroy();
    expect(ativas.size).toBe(0);
  });
});
