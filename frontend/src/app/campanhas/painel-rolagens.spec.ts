import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, Subject, of, throwError } from 'rxjs';
import { ApiError } from '../core/api-error';
import { CampanhaApi } from './campanha-api';
import { Campanha } from './campanha.model';
import { EstadoDaMesa, MesaAoVivo } from './mesa-ao-vivo';
import { PainelRolagens } from './painel-rolagens';
import { RolagemDaMesa } from './rolagem-da-mesa.model';
import { RolagensApi } from './rolagens-api';

const LIA = { id: 8, nome: 'Lía' };
const ANA = { id: 7, nome: 'Ana' };

function teste(id: number, extra: Partial<RolagemDaMesa> = {}): RolagemDaMesa {
  return {
    id, campanhaId: 3, criadaEm: new Date().toISOString(), autor: LIA, personagem: 'Kai', doMestre: false,
    tipo: 'TESTE', rotulo: 'Furtividade', total: 25,
    detalhe: { alvo: 'PERICIA', codigo: 'FURTIVIDADE', nome: 'Furtividade', dados: [17, 6], mantido: 17, bonus: 8, total: 25 },
    ...extra,
  } as RolagemDaMesa;
}

const CAMPANHA = { id: 3, nome: 'Mesa', mestre: ANA } as Campanha;

/** O painel dentro de um hospedeiro, para trocar o `campanhaId` e ouvir o `fechar` como a tela faz. */
@Component({
  imports: [PainelRolagens],
  template: `<app-painel-rolagens [campanhaId]="campanhaId()" (fechar)="fechou = fechou + 1" />`,
})
class Hospedeiro {
  campanhaId = signal(3);
  fechou = 0;
}

describe('PainelRolagens', () => {
  let vivas: Map<number, Subject<RolagemDaMesa>>;
  let assinaturasAtivas: Set<number>;
  let historico: ReturnType<typeof vi.fn>;
  let estado: ReturnType<typeof signal<EstadoDaMesa>>;

  function montar(paginas: Record<string, RolagemDaMesa[] | Observable<RolagemDaMesa[]>> = { '3:': [] }) {
    vivas = new Map();
    assinaturasAtivas = new Set();
    estado = signal<EstadoDaMesa>('conectado');
    historico = vi.fn((campanhaId: number, antes?: number) => {
      const pagina = paginas[`${campanhaId}:${antes ?? ''}`] ?? [];
      return Array.isArray(pagina) ? of(pagina) : pagina;
    });
    const mesa = {
      estado,
      rolagens$: (campanhaId: number) =>
        new Observable<RolagemDaMesa>((assinante) => {
          const s = new Subject<RolagemDaMesa>();
          vivas.set(campanhaId, s);
          assinaturasAtivas.add(campanhaId);
          const sub = s.subscribe(assinante);
          return () => {
            sub.unsubscribe();
            assinaturasAtivas.delete(campanhaId);
          };
        }),
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: MesaAoVivo, useValue: mesa },
        { provide: RolagensApi, useValue: { historico } },
        { provide: CampanhaApi, useValue: { buscar: () => of(CAMPANHA) } },
      ],
    });
    const fixture: ComponentFixture<Hospedeiro> = TestBed.createComponent(Hospedeiro);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    return { fixture, el, host: fixture.componentInstance };
  }

  const linhas = (el: HTMLElement) => Array.from(el.querySelectorAll('li.rolagem')) as HTMLElement[];
  const botao = (el: HTMLElement, texto: string) =>
    Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.trim() === texto) as HTMLButtonElement | undefined;

  it('é um diálogo com o título "Rolagens" e o foco nele', async () => {
    const { fixture, el } = montar();
    await fixture.whenStable();
    const dialogo = el.querySelector('[role="dialog"]') as HTMLElement;
    const titulo = el.querySelector('#titulo-rolagens') as HTMLElement;
    expect(dialogo.getAttribute('aria-labelledby')).toBe('titulo-rolagens');
    expect(titulo.textContent?.trim()).toBe('Rolagens');
    expect(document.activeElement).toBe(titulo);
  });

  it('mostra o histórico: quem rolou, o rótulo e o total', () => {
    const { el } = montar({ '3:': [teste(41), teste(40, { autor: ANA, personagem: 'Dara', rotulo: 'Luta', total: 11 })] });
    const l = linhas(el);
    expect(l.length).toBe(2);
    expect(l[0].textContent).toContain('Lía · Kai');
    expect(l[0].textContent).toContain('Furtividade');
    expect(l[0].querySelector('.total')?.textContent?.trim()).toBe('25');
    expect(l[1].textContent).toContain('Dara');
  });

  it('pede as 50 últimas da campanha ao abrir', () => {
    montar();
    expect(historico).toHaveBeenCalledWith(3);
  });

  it('a rolagem nova entra no topo', () => {
    const { fixture, el } = montar({ '3:': [teste(41)] });
    vivas.get(3)!.next(teste(42, { rotulo: 'Atletismo', total: 12 }));
    fixture.detectChanges();
    const l = linhas(el);
    expect(l.length).toBe(2);
    expect(l[0].textContent).toContain('Atletismo');
    expect(l[1].textContent).toContain('Furtividade');
  });

  it('o mesmo id pelo GET e pelo tópico aparece uma vez só (a própria rolagem, ou a reconexão)', () => {
    const { fixture, el } = montar({ '3:': [teste(41)] });
    vivas.get(3)!.next(teste(41));
    vivas.get(3)!.next(teste(41));
    fixture.detectChanges();
    expect(linhas(el).length).toBe(1);
  });

  it('a rolagem que chega antes do histórico não duplica quando o histórico volta', () => {
    const resposta = new Subject<RolagemDaMesa[]>();
    const { fixture, el } = montar({ '3:': resposta });
    vivas.get(3)!.next(teste(42));
    resposta.next([teste(42), teste(41)]);
    fixture.detectChanges();
    expect(linhas(el).length).toBe(2);
  });

  it('ignora uma rolagem de outra campanha que chegue pelo tópico', () => {
    const { fixture, el } = montar();
    vivas.get(3)!.next(teste(50, { campanhaId: 9 }));
    fixture.detectChanges();
    expect(linhas(el).length).toBe(0);
  });

  it('vazio: "Nenhuma rolagem nesta campanha ainda."', () => {
    const { el } = montar();
    expect(el.textContent).toContain('Nenhuma rolagem nesta campanha ainda.');
    expect(botao(el, 'Carregar mais')).toBeUndefined();
  });

  it('o texto de vazio some quando chega uma rolagem', () => {
    const { fixture, el } = montar();
    vivas.get(3)!.next(teste(1));
    fixture.detectChanges();
    expect(el.textContent).not.toContain('Nenhuma rolagem nesta campanha ainda.');
  });

  describe('Carregar mais', () => {
    const cheia = Array.from({ length: 50 }, (_, i) => teste(100 - i)); // ids de 100 a 51

    it('aparece com a página cheia, manda antes = o menor id e junta as páginas', () => {
      const { fixture, el } = montar({ '3:': cheia, '3:51': [teste(50), teste(49)] });
      expect(linhas(el).length).toBe(50);
      botao(el, 'Carregar mais')!.click();
      fixture.detectChanges();
      expect(historico).toHaveBeenLastCalledWith(3, 51);
      expect(linhas(el).length).toBe(52);
    });

    it('some quando a página seguinte vem incompleta', () => {
      const { fixture, el } = montar({ '3:': cheia, '3:51': [teste(50)] });
      botao(el, 'Carregar mais')!.click();
      fixture.detectChanges();
      expect(botao(el, 'Carregar mais')).toBeUndefined();
    });

    it('não aparece com a primeira página incompleta', () => {
      const { el } = montar({ '3:': [teste(41)] });
      expect(botao(el, 'Carregar mais')).toBeUndefined();
    });

    it('continua aparecendo com outra página cheia', () => {
      const outra = Array.from({ length: 50 }, (_, i) => teste(50 - i)); // 50 a 1
      const { fixture, el } = montar({ '3:': cheia, '3:51': outra });
      botao(el, 'Carregar mais')!.click();
      fixture.detectChanges();
      expect(botao(el, 'Carregar mais')).toBeDefined();
    });
  });

  describe('quem rolou', () => {
    it('"Mestre" quando a ficha é do mestre, sem o nome do personagem', () => {
      const { el } = montar({ '3:': [teste(41, { autor: ANA, personagem: null, doMestre: true })] });
      expect(linhas(el)[0].querySelector('.quem')?.textContent?.trim()).toBe('Mestre');
    });

    it('"(rolado pelo mestre)" quando o mestre rolou com a ficha de um jogador', () => {
      const { el } = montar({ '3:': [teste(41, { autor: ANA, personagem: 'Kai' })] });
      expect(linhas(el)[0].querySelector('.quem')?.textContent?.trim()).toBe('Kai (rolado pelo mestre)');
    });

    it('o jogador rolando com a própria ficha: "autor · personagem"', () => {
      const { el } = montar({ '3:': [teste(41)] });
      expect(linhas(el)[0].querySelector('.quem')?.textContent?.trim()).toBe('Lía · Kai');
    });
  });

  describe('os dados', () => {
    it('teste: o dado mantido em destaque e "20 natural"', () => {
      const r = teste(41, {
        detalhe: { alvo: 'PERICIA', codigo: 'F', nome: 'F', dados: [20, 6], mantido: 20, bonus: 5, total: 25 },
      } as Partial<RolagemDaMesa>);
      const { el } = montar({ '3:': [r] });
      const dados = linhas(el)[0].querySelector('.dados') as HTMLElement;
      expect(dados.querySelector('strong')?.textContent).toBe('20');
      expect(dados.textContent).toContain('6');
      expect(dados.textContent).toContain('20 natural');
    });

    it('teste: "1 natural"', () => {
      const r = teste(41, {
        detalhe: { alvo: 'PERICIA', codigo: 'F', nome: 'F', dados: [1], mantido: 1, bonus: 0, total: 1 },
      } as Partial<RolagemDaMesa>);
      const { el } = montar({ '3:': [r] });
      expect(linhas(el)[0].querySelector('.dados')?.textContent).toContain('1 natural');
    });

    it('ataque: "Crítico!" e o dano', () => {
      const r: RolagemDaMesa = {
        id: 41, campanhaId: 3, criadaEm: new Date().toISOString(), autor: LIA, personagem: 'Kai', doMestre: false,
        tipo: 'ATAQUE', rotulo: 'Espada', total: 19,
        detalhe: {
          ataqueId: 1, nome: 'Espada', critico: true,
          ataque: { dados: [19], mantido: 19, bonus: 0, total: 19 },
          dano: { expressao: '2d8+3', dados: [{ lados: 8, valor: 5 }, { lados: 8, valor: 6 }], bonus: 3, total: 14 },
        },
      };
      const { el } = montar({ '3:': [r] });
      const dados = linhas(el)[0].querySelector('.dados') as HTMLElement;
      expect(dados.textContent).toContain('Crítico!');
      expect(dados.textContent).toContain('dano 14');
      expect(dados.textContent).toContain('2d8+3');
    });

    it('ataque sem crítico não mostra "Crítico!"', () => {
      const r: RolagemDaMesa = {
        id: 41, campanhaId: 3, criadaEm: new Date().toISOString(), autor: LIA, personagem: 'Kai', doMestre: false,
        tipo: 'ATAQUE', rotulo: 'Espada', total: 12,
        detalhe: {
          ataqueId: 1, nome: 'Espada', critico: false,
          ataque: { dados: [12], mantido: 12, bonus: 0, total: 12 },
          dano: { expressao: '1d8', dados: [{ lados: 8, valor: 4 }], bonus: 0, total: 4 },
        },
      };
      const { el } = montar({ '3:': [r] });
      expect(linhas(el)[0].textContent).not.toContain('Crítico!');
    });

    it('rolagem livre: os dados e o bônus', () => {
      const r: RolagemDaMesa = {
        id: 41, campanhaId: 3, criadaEm: new Date().toISOString(), autor: LIA, personagem: 'Kai', doMestre: false,
        tipo: 'LIVRE', rotulo: '2d6+3', total: 12,
        detalhe: { expressao: '2d6+3', dados: [{ lados: 6, valor: 4 }, { lados: 6, valor: 5 }], bonus: 3, total: 12 },
      };
      const { el } = montar({ '3:': [r] });
      const dados = linhas(el)[0].querySelector('.dados') as HTMLElement;
      expect(dados.textContent).toContain('4');
      expect(dados.textContent).toContain('5');
      expect(dados.textContent).toContain('+3');
    });
  });

  describe('a hora', () => {
    const atras = (ms: number) => new Date(Date.now() - ms).toISOString();
    const hora = (el: HTMLElement) => linhas(el)[0].querySelector('.hora')?.textContent?.trim();

    it('"agora" no primeiro minuto', () => {
      expect(hora(montar({ '3:': [teste(1, { criadaEm: atras(20_000) })] }).el)).toBe('agora');
    });
    it('"há N min"', () => {
      expect(hora(montar({ '3:': [teste(1, { criadaEm: atras(3 * 60_000 + 500) })] }).el)).toBe('há 3 min');
    });
    it('"há N h"', () => {
      expect(hora(montar({ '3:': [teste(1, { criadaEm: atras(2 * 3_600_000 + 500) })] }).el)).toBe('há 2 h');
    });
    it('depois de 24 h, a data', () => {
      expect(hora(montar({ '3:': [teste(1, { criadaEm: atras(30 * 3_600_000) })] }).el)).toMatch(/\d{2}\/\d{2}\/\d{4}/);
    });
  });

  describe('a conexão', () => {
    it('"ao vivo" quando conectado', () => {
      const { el } = montar();
      expect(el.querySelector('.estado')?.textContent?.trim()).toBe('ao vivo');
    });

    it('"reconectando…" quando não está conectado', () => {
      const { fixture, el } = montar();
      estado.set('reconectando');
      fixture.detectChanges();
      expect(el.querySelector('.estado')?.textContent?.trim()).toBe('reconectando…');
    });
  });

  describe('fechar', () => {
    it('o botão "Fechar" emite fechar', () => {
      const { el, host } = montar();
      botao(el, 'Fechar')!.click();
      expect(host.fechou).toBe(1);
    });

    it('Esc emite fechar', () => {
      const { host } = montar();
      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
      expect(host.fechou).toBe(1);
    });
  });

  describe('trocar de campanha (da A para a B)', () => {
    it('cancela a inscrição antiga, esvazia a lista e busca o histórico da nova', () => {
      const { fixture, el, host } = montar({ '3:': [teste(41)], '4:': [teste(7, { campanhaId: 4, rotulo: 'Luta' })] });
      expect(assinaturasAtivas).toEqual(new Set([3]));
      host.campanhaId.set(4);
      fixture.detectChanges();
      expect(assinaturasAtivas).toEqual(new Set([4]));
      expect(historico).toHaveBeenLastCalledWith(4);
      const l = linhas(el);
      expect(l.length).toBe(1);
      expect(l[0].textContent).toContain('Luta');
      expect(el.textContent).not.toContain('Furtividade');
    });

    it('o histórico lento da campanha antiga não aparece na nova', () => {
      const lento = new Subject<RolagemDaMesa[]>();
      const { fixture, el, host } = montar({ '3:': lento, '4:': [] });
      host.campanhaId.set(4);
      fixture.detectChanges();
      lento.next([teste(41)]);
      fixture.detectChanges();
      expect(linhas(el).length).toBe(0);
    });

    it('ao destruir o painel, cancela a inscrição', () => {
      const { fixture } = montar();
      fixture.destroy();
      expect(assinaturasAtivas.size).toBe(0);
    });
  });

  it('erro ao carregar o histórico: mostra o aviso e as rolagens ao vivo continuam chegando', () => {
    const erro: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'Tente de novo.', campos: {} };
    const { fixture, el } = montar({ '3:': throwError(() => erro) });
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Servidor indisponível');
    vivas.get(3)!.next(teste(42));
    fixture.detectChanges();
    expect(linhas(el).length).toBe(1);
  });
});
