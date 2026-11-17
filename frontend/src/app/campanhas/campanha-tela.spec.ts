import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { NEVER, of, throwError } from 'rxjs';
import { ApiError } from '../core/api-error';
import { Sessao } from '../core/auth/sessao';
import { respostaDe } from '../core/auth/teste-auth';
import { FichaResumo } from '../fichas/ficha.model';
import { CampanhaApi } from './campanha-api';
import { CampanhaTela } from './campanha-tela';
import { Campanha, Membro } from './campanha.model';
import { MesaAoVivo } from './mesa-ao-vivo';
import { RolagemDaMesa } from './rolagem-da-mesa.model';
import { RolagensApi } from './rolagens-api';
import { historicoVazio, mesaFalsa } from './teste-mesa';

// A sessão de teste é a da Ana (id 7).
const ANA = { id: 7, nome: 'Ana' };
const BIA = { id: 8, nome: 'Bia' };

const CAMPANHA_MESTRE: Campanha = {
  id: 3, nome: 'Mesa de sexta', descricao: 'Toda sexta, às 20h.', meuPapel: 'MESTRE', mestre: ANA,
  convite: { codigo: 'AbCdEf123456', ativo: true },
};
const CAMPANHA_JOGADOR: Campanha = {
  id: 3, nome: 'Mesa de sexta', descricao: null, meuPapel: 'JOGADOR', mestre: BIA, convite: null,
};

const MEMBROS_MESTRE: Membro[] = [
  { usuarioId: 7, nome: 'Ana', papel: 'MESTRE', entrouEm: '2026-11-16T10:00:00Z' },
  { usuarioId: 8, nome: 'Bia', papel: 'JOGADOR', entrouEm: '2026-11-16T11:00:00Z' },
];
const MEMBROS_JOGADOR: Membro[] = [
  { usuarioId: 8, nome: 'Bia', papel: 'MESTRE', entrouEm: '2026-11-16T10:00:00Z' },
  { usuarioId: 7, nome: 'Ana', papel: 'JOGADOR', entrouEm: '2026-11-16T11:00:00Z' },
  { usuarioId: 9, nome: 'Caio', papel: 'JOGADOR', entrouEm: '2026-11-16T12:00:00Z' },
];

function resumo(id: number, personagem: string, dono: { id: number; nome: string }): FichaResumo {
  return {
    id, sistema: 'ORDEM_PARANORMAL', dono, campanha: { id: 3, nome: 'Mesa de sexta' }, jogador: dono.nome, personagem,
    idade: null, retratoVersao: null, recursos: [{ codigo: 'PV', atual: 9, maximo: 12 }],
  };
}

const INDISPONIVEL: ApiError = {
  status: 0, titulo: 'Servidor indisponível', detalhe: 'Ele pode estar acordando.', campos: {},
};

let mesa: ReturnType<typeof mesaFalsa>;

function montar(api: Partial<Record<keyof CampanhaApi, unknown>>) {
  localStorage.clear();
  mesa = mesaFalsa();
  TestBed.configureTestingModule({
    imports: [CampanhaTela],
    providers: [
      provideRouter([]),
      { provide: CampanhaApi, useValue: api },
      { provide: MesaAoVivo, useValue: mesa },
      { provide: RolagensApi, useValue: historicoVazio },
    ],
  });
  TestBed.inject(Sessao).iniciar(respostaDe());
  const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  const fixture = TestBed.createComponent(CampanhaTela);
  fixture.componentRef.setInput('id', '3');
  fixture.detectChanges();
  return { fixture, el: fixture.nativeElement as HTMLElement, navegar };
}

/** A visão do mestre (Ana): campanha, membros e as três fichas da mesa. */
function apiMestre(extra: Partial<Record<keyof CampanhaApi, unknown>> = {}) {
  return {
    buscar: vi.fn(() => of(CAMPANHA_MESTRE)),
    membros: vi.fn(() => of(MEMBROS_MESTRE)),
    fichas: vi.fn(() => of([resumo(11, 'Lia', ANA), resumo(12, 'Hale', BIA)])),
    ...extra,
  };
}

/** A visão de uma jogadora (Ana): a ficha do mestre não vem; a de Caio vem só como resumo. */
function apiJogador(extra: Partial<Record<keyof CampanhaApi, unknown>> = {}) {
  return {
    buscar: vi.fn(() => of(CAMPANHA_JOGADOR)),
    membros: vi.fn(() => of(MEMBROS_JOGADOR)),
    fichas: vi.fn(() => of([resumo(11, 'Lia', ANA), resumo(13, 'Kai', { id: 9, nome: 'Caio' })])),
    ...extra,
  };
}

function botao(el: HTMLElement, texto: string): HTMLButtonElement {
  return Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes(texto)) as HTMLButtonElement;
}

function clicar(fixture: { detectChanges: () => void }, el: HTMLElement, texto: string): void {
  botao(el, texto).click();
  fixture.detectChanges();
}

describe('CampanhaTela: carregamento', () => {
  it('carregando: avisa com role=status', () => {
    const { el } = montar({ buscar: () => NEVER, membros: () => NEVER, fichas: () => NEVER });
    expect(el.querySelector('[role="status"]')?.textContent).toContain('Carregando campanha');
  });

  it('carrega a campanha, os membros e as fichas da mesa e mostra nome, descrição e papel', () => {
    const api = apiMestre();
    const { el } = montar(api);
    expect(api.buscar).toHaveBeenCalledWith(3);
    expect(api.membros).toHaveBeenCalledWith(3);
    expect(api.fichas).toHaveBeenCalledWith(3);
    expect(el.querySelector('h1')?.textContent).toContain('Mesa de sexta');
    expect(el.textContent).toContain('Toda sexta, às 20h.');
    expect(el.textContent).toContain('Você é o mestre');
  });

  it('404: campanha não encontrada, com volta para a lista', () => {
    const naoAcho: ApiError = { status: 404, titulo: 'Campanha não encontrada', detalhe: '', campos: {} };
    const { el } = montar({
      buscar: () => throwError(() => naoAcho),
      membros: () => of([]),
      fichas: () => of([]),
    });
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Campanha não encontrada');
    expect(el.querySelector('a[href="/campanhas"]')).not.toBeNull();
  });

  it('erro: mostra o motivo e tenta de novo', () => {
    const base = apiMestre();
    const buscar = vi
      .fn()
      .mockReturnValueOnce(throwError(() => INDISPONIVEL))
      .mockReturnValueOnce(of(CAMPANHA_MESTRE));
    const { fixture, el } = montar({ ...base, buscar });
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Servidor indisponível');

    clicar(fixture, el, 'Tentar de novo');

    expect(buscar).toHaveBeenCalledTimes(2);
    expect(el.querySelector('h1')?.textContent).toContain('Mesa de sexta');
  });
});

describe('CampanhaTela: as fichas da mesa', () => {
  it('o mestre abre todas as fichas e vê o dono em cada cartão', () => {
    const { el } = montar(apiMestre());
    const links = Array.from(el.querySelectorAll('a.cartao-ficha')).map((a) => a.getAttribute('href'));
    expect(links).toEqual(['/fichas/11', '/fichas/12']);
    expect(el.textContent).toContain('Dono: Bia');
  });

  it('o jogador abre só a própria ficha; a de outro jogador é um cartão sem link', () => {
    const { el } = montar(apiJogador());
    expect(Array.from(el.querySelectorAll('a.cartao-ficha')).map((a) => a.getAttribute('href'))).toEqual(['/fichas/11']);
    const semLink = el.querySelector('div.cartao-ficha');
    expect(semLink?.textContent).toContain('Kai');
    expect(semLink?.textContent).toContain('Dono: Caio');
  });

  it('os cartões não repetem o nome da campanha', () => {
    const { el } = montar(apiMestre());
    expect(el.querySelector('.cartao-ficha .campanha')).toBeNull();
  });

  it('+ Nova ficha aqui leva à criação com a campanha na URL', () => {
    const { el } = montar(apiJogador());
    const link = Array.from(el.querySelectorAll('a')).find((a) => a.textContent?.includes('+ Nova ficha aqui'));
    expect(link?.getAttribute('href')).toBe('/fichas/nova?campanha=3');
  });

  it('mesa vazia: explica', () => {
    const { el } = montar(apiMestre({ fichas: () => of([]) }));
    expect(el.textContent).toContain('Nenhuma ficha na mesa ainda.');
  });
});

describe('CampanhaTela: o que cada papel vê', () => {
  it('o mestre vê Editar, Excluir campanha, o convite e Remover nos jogadores (não em si)', () => {
    const { el } = montar(apiMestre());
    expect(botao(el, 'Editar')).toBeTruthy();
    expect(botao(el, 'Excluir campanha')).toBeTruthy();
    expect(el.querySelector('input[readonly]')).not.toBeNull();
    expect(botao(el, 'Sair da campanha')).toBeUndefined();
    const linhas = Array.from(el.querySelectorAll('.membro'));
    expect(linhas.length).toBe(2);
    expect(linhas[0].textContent).toContain('Ana');
    expect(linhas[0].textContent).toContain('Mestre');
    expect(linhas[0].querySelector('button')).toBeNull();
    expect(linhas[1].querySelector('button')?.textContent).toContain('Remover');
    expect(linhas[1].querySelector('button')?.getAttribute('aria-label')).toBe('Remover Bia da campanha');
  });

  it('o jogador vê só Sair da campanha: sem Editar, Excluir, convite nem Remover', () => {
    const { el } = montar(apiJogador());
    expect(botao(el, 'Sair da campanha')).toBeTruthy();
    expect(botao(el, 'Editar')).toBeUndefined();
    expect(botao(el, 'Excluir campanha')).toBeUndefined();
    expect(botao(el, 'Remover')).toBeUndefined();
    expect(el.querySelector('input[readonly]')).toBeNull();
    expect(el.textContent).not.toContain('Convite');
    expect(el.textContent).toContain('Você é jogador');
  });
});

describe('CampanhaTela: remover e sair', () => {
  it('o mestre remove um jogador depois de confirmar, e a mesa é recarregada', () => {
    const removerMembro = vi.fn(() => of(undefined));
    const api = apiMestre({ removerMembro });
    const { fixture, el } = montar(api);

    clicar(fixture, el, 'Remover');
    expect(removerMembro).not.toHaveBeenCalled();
    expect(el.textContent).toContain('Remover Bia da campanha?');
    expect(el.textContent).toContain('As fichas de Bia ficam sem campanha');

    clicar(fixture, el, 'Sim, remover');

    expect(removerMembro).toHaveBeenCalledWith(3, 8);
    expect(api.membros).toHaveBeenCalledTimes(2);
    expect(api.fichas).toHaveBeenCalledTimes(2);
    expect(el.textContent).not.toContain('Remover Bia da campanha?');
  });

  it('Cancelar na confirmação não remove ninguém', () => {
    const removerMembro = vi.fn(() => of(undefined));
    const { fixture, el } = montar(apiMestre({ removerMembro }));
    clicar(fixture, el, 'Remover');
    clicar(fixture, el, 'Cancelar');
    expect(removerMembro).not.toHaveBeenCalled();
    expect(el.textContent).not.toContain('Remover Bia da campanha?');
  });

  it('o jogador sai depois de confirmar e volta para a lista de campanhas', () => {
    const removerMembro = vi.fn(() => of(undefined));
    const { fixture, el, navegar } = montar(apiJogador({ removerMembro }));

    clicar(fixture, el, 'Sair da campanha');
    expect(removerMembro).not.toHaveBeenCalled();
    expect(el.textContent).toContain('As suas fichas ficam sem campanha');

    clicar(fixture, el, 'Sim, sair');

    expect(removerMembro).toHaveBeenCalledWith(3, 7);
    expect(navegar).toHaveBeenCalledWith(['/campanhas']);
  });

  it('erro ao remover aparece num alerta', () => {
    const removerMembro = vi.fn(() => throwError(() => INDISPONIVEL));
    const { fixture, el } = montar(apiMestre({ removerMembro }));
    clicar(fixture, el, 'Remover');
    clicar(fixture, el, 'Sim, remover');
    expect(el.querySelector('p[role="alert"]')?.textContent).toContain('Servidor indisponível');
  });
});

describe('CampanhaTela: editar e excluir', () => {
  it('Editar abre o formulário com os dados e salva com PUT, atualizando o título', () => {
    const atualizar = vi.fn(() => of({ ...CAMPANHA_MESTRE, nome: 'Mesa nova', descricao: null }));
    const { fixture, el } = montar(apiMestre({ atualizar }));

    clicar(fixture, el, 'Editar');
    const nome = el.querySelector<HTMLInputElement>('[formControlName="nome"]')!;
    expect(nome.value).toBe('Mesa de sexta');
    nome.value = 'Mesa nova';
    nome.dispatchEvent(new Event('input'));
    const descricao = el.querySelector<HTMLTextAreaElement>('[formControlName="descricao"]')!;
    descricao.value = '';
    descricao.dispatchEvent(new Event('input'));
    el.querySelector<HTMLButtonElement>('form button[type="submit"]')!.click();
    fixture.detectChanges();

    expect(atualizar).toHaveBeenCalledWith(3, { nome: 'Mesa nova', descricao: null });
    expect(el.querySelector('h1')?.textContent).toContain('Mesa nova');
    expect(el.querySelector('form')).toBeNull();
  });

  it('Excluir campanha confirma com o aviso, apaga e volta para a lista', () => {
    const excluir = vi.fn(() => of(undefined));
    const { fixture, el, navegar } = montar(apiMestre({ excluir }));

    clicar(fixture, el, 'Excluir campanha');
    expect(excluir).not.toHaveBeenCalled();
    expect(el.textContent).toContain('As fichas voltam para os donos, sem campanha.');

    clicar(fixture, el, 'Sim, excluir');

    expect(excluir).toHaveBeenCalledWith(3);
    expect(navegar).toHaveBeenCalledWith(['/campanhas']);
  });
});

describe('CampanhaTela: convite', () => {
  const LINK = `${location.origin}/convite/AbCdEf123456`;

  function comAreaDeTransferencia(writeText: () => Promise<void>) {
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
  }

  it('mostra o link completo, selecionável', () => {
    const { el } = montar(apiMestre());
    expect(el.querySelector<HTMLInputElement>('input[readonly]')?.value).toBe(LINK);
  });

  it('Copiar manda o link para a área de transferência e avisa com role=status', async () => {
    const writeText = vi.fn(() => Promise.resolve());
    comAreaDeTransferencia(writeText);
    const { fixture, el } = montar(apiMestre());

    clicar(fixture, el, 'Copiar');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(writeText).toHaveBeenCalledWith(LINK);
    expect(el.querySelector('.convite [role="status"]')?.textContent).toContain('Link copiado');
  });

  it('se não conseguir copiar, pede para selecionar o link', async () => {
    comAreaDeTransferencia(() => Promise.reject(new Error('negado')));
    const { fixture, el } = montar(apiMestre());

    clicar(fixture, el, 'Copiar');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(el.querySelector('.convite [role="status"]')?.textContent).toContain('Selecione o link');
  });

  it('Renovar avisa que o link antigo para de funcionar e só gera depois de confirmar', () => {
    const renovarConvite = vi.fn(() => of({ codigo: 'NovoCodigo123', ativo: true }));
    const { fixture, el } = montar(apiMestre({ renovarConvite }));

    clicar(fixture, el, 'Renovar');
    expect(renovarConvite).not.toHaveBeenCalled();
    expect(el.textContent).toContain('O link antigo para de funcionar.');

    clicar(fixture, el, 'Sim, renovar');

    expect(renovarConvite).toHaveBeenCalledWith(3);
    expect(el.querySelector<HTMLInputElement>('input[readonly]')?.value).toBe(`${location.origin}/convite/NovoCodigo123`);
  });

  it('Desativar tira o link e oferece Gerar novo link', () => {
    const desativarConvite = vi.fn(() => of(undefined));
    const renovarConvite = vi.fn(() => of({ codigo: 'Outro1234567', ativo: true }));
    const { fixture, el } = montar(apiMestre({ desativarConvite, renovarConvite }));

    clicar(fixture, el, 'Desativar');

    expect(desativarConvite).toHaveBeenCalledWith(3);
    expect(el.querySelector('input[readonly]')).toBeNull();
    expect(el.textContent).toContain('O convite está desativado.');

    clicar(fixture, el, 'Gerar novo link');

    expect(renovarConvite).toHaveBeenCalledWith(3);
    expect(el.querySelector<HTMLInputElement>('input[readonly]')?.value).toBe(`${location.origin}/convite/Outro1234567`);
  });

  it('convite que já vem desativado mostra Gerar novo link', () => {
    const desativada = { ...CAMPANHA_MESTRE, convite: { codigo: 'AbCdEf123456', ativo: false } };
    const { el } = montar(apiMestre({ buscar: () => of(desativada) }));
    expect(el.querySelector('input[readonly]')).toBeNull();
    expect(botao(el, 'Gerar novo link')).toBeTruthy();
  });
});

describe('CampanhaTela: rolagens ao vivo (2.4)', () => {
  const DA_BIA: RolagemDaMesa = {
    id: 41, campanhaId: 3, criadaEm: new Date().toISOString(), autor: BIA, personagem: 'Kai', doMestre: false,
    tipo: 'TESTE', rotulo: 'Furtividade', total: 25,
    detalhe: { alvo: 'PERICIA', codigo: 'FURTIVIDADE', nome: 'Furtividade', dados: [17, 6], mantido: 17, bonus: 8, total: 25 },
  };

  it('o botão Rolagens aparece para o mestre, com o painel fechado', () => {
    const { el } = montar(apiMestre());
    expect(botao(el, 'Rolagens')).toBeTruthy();
    expect(el.querySelector('app-painel-rolagens')).toBeNull();
  });

  it('o botão Rolagens aparece para o jogador', () => {
    const { el } = montar(apiJogador());
    expect(botao(el, 'Rolagens')).toBeTruthy();
  });

  it('o botão abre o painel desta campanha e "Fechar" o fecha', () => {
    const { fixture, el } = montar(apiMestre());
    clicar(fixture, el, 'Rolagens');
    expect(el.querySelector('app-painel-rolagens [role="dialog"]')).not.toBeNull();
    clicar(fixture, el, 'Fechar');
    expect(el.querySelector('app-painel-rolagens')).toBeNull();
  });

  it('a conexão ao vivo fica ativa com a tela aberta, mesmo com o painel fechado', () => {
    montar(apiJogador());
    expect(mesa.inscritos(3)).toBe(1);
  });

  it('a rolagem de outra pessoa com o painel fechado vira um aviso; tocar nele abre o painel', () => {
    const { fixture, el } = montar(apiMestre());
    mesa.emitir(3, DA_BIA);
    fixture.detectChanges();
    const aviso = el.querySelector('.aviso-rolagem') as HTMLButtonElement;
    expect(aviso.textContent?.trim()).toBe('Bia · Kai · Furtividade: 25');
    aviso.click();
    fixture.detectChanges();
    expect(el.querySelector('app-painel-rolagens')).not.toBeNull();
    expect(el.querySelector('.aviso-rolagem')).toBeNull();
  });

  it('a rolagem da própria pessoa não gera aviso', () => {
    const { fixture, el } = montar(apiMestre());
    mesa.emitir(3, { ...DA_BIA, autor: ANA });
    fixture.detectChanges();
    expect(el.querySelector('.aviso-rolagem')).toBeNull();
  });

  it('com o painel aberto não há aviso, e a rolagem entra no painel', () => {
    const { fixture, el } = montar(apiMestre());
    clicar(fixture, el, 'Rolagens');
    mesa.emitir(3, DA_BIA);
    fixture.detectChanges();
    expect(el.querySelector('.aviso-rolagem')).toBeNull();
    expect(el.querySelector('app-painel-rolagens li.rolagem')?.textContent).toContain('Furtividade');
  });
});
