import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { Sessao } from '../core/auth/sessao';
import { respostaDe } from '../core/auth/teste-auth';
import { CampanhaApi } from './campanha-api';
import { CampanhaTela } from './campanha-tela';
import { Campanha, Membro } from './campanha.model';
import { MesaAoVivo } from './mesa-ao-vivo';
import { RolagensApi } from './rolagens-api';
import { historicoVazio, mesaFalsa } from './teste-mesa';

const TEXTO_DO_CARTAO = 'Abra este convite em outra aba ou no celular para entrar como jogador.';
const LINK = `${location.origin}/convite/AbCdEf123456`;
const ANA = { id: 7, nome: 'Ana' };

const MESTRE: Campanha = {
  id: 3, nome: 'Mesa de demonstração', descricao: null, meuPapel: 'MESTRE', mestre: ANA,
  convite: { codigo: 'AbCdEf123456', ativo: true },
};
const JOGADOR: Campanha = { ...MESTRE, meuPapel: 'JOGADOR', mestre: { id: 8, nome: 'Bia' }, convite: null };
const MEMBROS: Membro[] = [{ usuarioId: 7, nome: 'Ana', papel: 'MESTRE', entrouEm: '2026-11-16T10:00:00Z' }];

function montar(campanha: Campanha, demo: boolean) {
  localStorage.clear();
  sessionStorage.clear();
  const api = { buscar: () => of(campanha), membros: () => of(MEMBROS), fichas: () => of([]) };
  TestBed.configureTestingModule({
    imports: [CampanhaTela],
    providers: [
      provideRouter([]),
      { provide: CampanhaApi, useValue: api },
      { provide: MesaAoVivo, useValue: mesaFalsa() },
      { provide: RolagensApi, useValue: historicoVazio },
    ],
  });
  TestBed.inject(Sessao).iniciar(respostaDe(), demo);
  const fixture = TestBed.createComponent(CampanhaTela);
  fixture.componentRef.setInput('id', '3');
  fixture.detectChanges();
  const el = fixture.nativeElement as HTMLElement;
  return { fixture, el, cartao: () => el.querySelector<HTMLElement>('.cartao-demo') };
}

describe('CampanhaTela: cartão do convite demo', () => {
  it('conta demo (mestre): mostra o texto exato e o botão de copiar o link', () => {
    const { cartao } = montar(MESTRE, true);
    expect(cartao()?.textContent).toContain(TEXTO_DO_CARTAO);
    expect(cartao()?.querySelector('button')?.textContent).toContain('Copiar link do convite');
  });

  it('conta normal: não mostra o cartão', () => {
    expect(montar(MESTRE, false).cartao()).toBeNull();
  });

  it('jogador demo: não mostra (ele não tem o link do convite)', () => {
    expect(montar(JOGADOR, true).cartao()).toBeNull();
  });

  it('o botão copia o mesmo link da seção Convite e avisa', async () => {
    const writeText = vi.fn(() => Promise.resolve());
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
    const { fixture, cartao } = montar(MESTRE, true);
    cartao()!.querySelector('button')!.click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(writeText).toHaveBeenCalledWith(LINK);
    expect(cartao()?.textContent).toContain('Link copiado');
  });

  it('convite desativado: o cartão aparece sem o botão de copiar', () => {
    const { cartao } = montar({ ...MESTRE, convite: { codigo: 'AbCdEf123456', ativo: false } }, true);
    expect(cartao()?.textContent).toContain(TEXTO_DO_CARTAO);
    expect(cartao()?.querySelector('button')).toBeNull();
  });
});
