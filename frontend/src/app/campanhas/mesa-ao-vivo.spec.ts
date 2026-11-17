import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RxStompState } from '@stomp/rx-stomp';
import { BehaviorSubject, Subject } from 'rxjs';
import { Renovacao } from '../core/auth/renovacao';
import { ResultadoRenovacao } from '../core/auth/auth.model';
import { Sessao } from '../core/auth/sessao';
import { respostaDe } from '../core/auth/teste-auth';
import { FABRICA_RX_STOMP, MesaAoVivo } from './mesa-ao-vivo';
import { RolagemDaMesa } from './rolagem-da-mesa.model';

/** O RxStomp de mentira: guarda a configuração e deixa o teste dirigir o estado e as mensagens. */
class RxStompFalso {
  config: Record<string, any> = {};
  ativo = false;
  stompClient = { connectHeaders: {} as Record<string, string> };
  connectionState$ = new BehaviorSubject<RxStompState>(RxStompState.CLOSED);
  mensagens = new Subject<{ body: string }>();
  destinos: string[] = [];
  configure(config: Record<string, any>) {
    this.config = config;
  }
  activate() {
    this.ativo = true;
  }
  deactivate = vi.fn(async () => {
    this.ativo = false;
  });
  watch(destino: string) {
    this.destinos.push(destino);
    return this.mensagens.asObservable();
  }
}

const ROLAGEM: RolagemDaMesa = {
  id: 41, campanhaId: 3, criadaEm: '2026-11-16T22:10:03Z', autor: { id: 8, nome: 'Lía' }, personagem: 'Kai',
  doMestre: false, tipo: 'TESTE', rotulo: 'Furtividade', total: 25,
  detalhe: { alvo: 'PERICIA', codigo: 'FURTIVIDADE', nome: 'Furtividade', dados: [17, 6], mantido: 17, bonus: 8, total: 25 },
};

describe('MesaAoVivo', () => {
  let falso: RxStompFalso;
  let renovar: ReturnType<typeof vi.fn<() => Promise<ResultadoRenovacao>>>;
  let navegar: ReturnType<typeof vi.spyOn>;
  let sessao: Sessao;
  let mesa: MesaAoVivo;

  beforeEach(() => {
    localStorage.clear();
    falso = new RxStompFalso();
    renovar = vi.fn<() => Promise<ResultadoRenovacao>>();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: FABRICA_RX_STOMP, useValue: () => falso },
        { provide: Renovacao, useValue: { renovar } },
      ],
    });
    sessao = TestBed.inject(Sessao);
    sessao.iniciar(respostaDe());
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    mesa = TestBed.inject(MesaAoVivo);
  });

  it('não conecta enquanto ninguém se inscreve', () => {
    expect(falso.ativo).toBe(false);
    expect(mesa.estado()).toBe('desconectado');
  });

  it('configura a URL do WebSocket e o heartbeat de 10 s', () => {
    mesa.rolagens$(3).subscribe();
    expect(falso.config['brokerURL']).toBe('ws://localhost:8080/ws');
    expect(falso.config['heartbeatIncoming']).toBe(10000);
    expect(falso.config['heartbeatOutgoing']).toBe(10000);
  });

  it('conecta na primeira inscrição e desativa na última', () => {
    const a = mesa.rolagens$(3).subscribe();
    const b = mesa.rolagens$(3).subscribe();
    expect(falso.ativo).toBe(true);
    a.unsubscribe();
    expect(falso.deactivate).not.toHaveBeenCalled();
    b.unsubscribe();
    expect(falso.deactivate).toHaveBeenCalledTimes(1);
    expect(mesa.estado()).toBe('desconectado');
  });

  it('assina o tópico da campanha e entrega a rolagem já convertida do JSON', () => {
    const recebidas: RolagemDaMesa[] = [];
    mesa.rolagens$(3).subscribe((r) => recebidas.push(r));
    expect(falso.destinos).toEqual(['/topic/campanhas/3/rolagens']);
    falso.mensagens.next({ body: JSON.stringify(ROLAGEM) });
    expect(recebidas).toEqual([ROLAGEM]);
  });

  it('ignora uma mensagem que não é JSON', () => {
    const recebidas: RolagemDaMesa[] = [];
    mesa.rolagens$(3).subscribe((r) => recebidas.push(r));
    falso.mensagens.next({ body: 'isto não é json' });
    expect(recebidas).toEqual([]);
  });

  it('põe o token da sessão no CONNECT, sem renovar na primeira tentativa', async () => {
    mesa.rolagens$(3).subscribe();
    await falso.config['beforeConnect'](falso);
    expect(falso.stompClient.connectHeaders['Authorization']).toBe('Bearer acesso-1');
    expect(renovar).not.toHaveBeenCalled();
  });

  it('na reconexão renova antes, porque o token de 15 min pode ter vencido', async () => {
    renovar.mockImplementation(async () => {
      sessao.iniciar(respostaDe('2'));
      return 'renovada';
    });
    mesa.rolagens$(3).subscribe();
    await falso.config['beforeConnect'](falso);
    await falso.config['beforeConnect'](falso);
    expect(renovar).toHaveBeenCalledTimes(1);
    expect(falso.stompClient.connectHeaders['Authorization']).toBe('Bearer acesso-2');
  });

  it('sem token, renova pelo fluxo do 2.2 e usa o token novo', async () => {
    sessao.limpar();
    renovar.mockImplementation(async () => {
      sessao.iniciar(respostaDe('3'));
      return 'renovada';
    });
    mesa.rolagens$(3).subscribe();
    await falso.config['beforeConnect'](falso);
    expect(falso.stompClient.connectHeaders['Authorization']).toBe('Bearer acesso-3');
  });

  it('renovação recusada: para de reconectar e segue o fluxo de sessão vencida', async () => {
    sessao.limpar();
    renovar.mockResolvedValue('sem-sessao');
    mesa.rolagens$(3).subscribe();
    await falso.config['beforeConnect'](falso);
    expect(falso.deactivate).toHaveBeenCalledTimes(1);
    expect(falso.stompClient.connectHeaders['Authorization']).toBeUndefined();
    expect(navegar).toHaveBeenCalledWith(['/entrar'], expect.anything());
    expect(mesa.estado()).toBe('desconectado');
  });

  it('renovação indisponível e sem token: para de reconectar, sem mandar para o login', async () => {
    sessao.limpar();
    renovar.mockResolvedValue('indisponivel');
    mesa.rolagens$(3).subscribe();
    await falso.config['beforeConnect'](falso);
    expect(falso.deactivate).toHaveBeenCalledTimes(1);
    expect(navegar).not.toHaveBeenCalled();
  });

  it('o estado acompanha a conexão: conectado, reconectando e desconectado', () => {
    const s = mesa.rolagens$(3).subscribe();
    falso.connectionState$.next(RxStompState.OPEN);
    expect(mesa.estado()).toBe('conectado');
    falso.connectionState$.next(RxStompState.CLOSED);
    expect(mesa.estado()).toBe('reconectando');
    falso.connectionState$.next(RxStompState.CONNECTING);
    expect(mesa.estado()).toBe('reconectando');
    s.unsubscribe();
    expect(mesa.estado()).toBe('desconectado');
  });

  it('depois de desconectar, uma inscrição nova conecta de novo', () => {
    mesa.rolagens$(3).subscribe().unsubscribe();
    falso.ativo = false;
    mesa.rolagens$(4).subscribe();
    expect(falso.ativo).toBe(true);
  });
});
