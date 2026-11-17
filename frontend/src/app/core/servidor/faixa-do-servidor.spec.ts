import { WritableSignal, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { EstadoDoServidor, SituacaoDoServidor } from './estado-do-servidor';
import { FaixaDoServidor } from './faixa-do-servidor';

describe('FaixaDoServidor', () => {
  let estado: WritableSignal<SituacaoDoServidor>;
  let segundos: WritableSignal<number>;

  function montar(inicial: SituacaoDoServidor = 'verificando') {
    estado = signal(inicial);
    segundos = signal(0);
    TestBed.configureTestingModule({ providers: [{ provide: EstadoDoServidor, useValue: { estado, segundos } }] });
    const fixture = TestBed.createComponent(FaixaDoServidor);
    fixture.detectChanges();
    const faixa = () => (fixture.nativeElement as HTMLElement).querySelector('[role="status"]');
    return { fixture, faixa };
  }

  it.each<SituacaoDoServidor>(['verificando', 'pronto'])('não aparece com o servidor %s', (situacao) => {
    expect(montar(situacao).faixa()).toBeNull();
  });

  it('acordando: mostra a mensagem com o contador de segundos e role=status', () => {
    const { fixture, faixa } = montar('acordando');
    segundos.set(7);
    fixture.detectChanges();
    expect(faixa()?.textContent?.trim()).toBe('Acordando o servidor gratuito… pode levar até 2 minutos (7s).');
    segundos.set(8);
    fixture.detectChanges();
    expect(faixa()?.textContent).toContain('(8s)');
  });

  it('some quando o servidor responde', () => {
    const { fixture, faixa } = montar('acordando');
    estado.set('pronto');
    fixture.detectChanges();
    expect(faixa()).toBeNull();
  });

  it('depois de 3 min mostra que o servidor não respondeu', () => {
    const { faixa } = montar('falhou');
    expect(faixa()?.textContent?.trim()).toBe('O servidor não respondeu. Tente recarregar a página.');
  });
});
