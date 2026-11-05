import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { FichaApi } from '../ficha-api';
import { Ficha, FichaDnd, FichaOrdem } from '../ficha.model';
import { FormularioDnd } from '../sistemas/dnd/formulario-dnd';
import { FormularioOrdem } from '../sistemas/ordem/formulario-ordem';
import { EdicaoFicha } from './edicao-ficha';

const ORDEM: FichaOrdem = {
  id: 7, sistema: 'ORDEM_PARANORMAL', jogador: 'Arthur', personagem: 'Detetive Lia', genero: 'F', idade: 34,
  retratoVersao: null, classe: 'OCULTISTA', origem: 'Acadêmica', nex: 35,
  atributos: { AGI: 1, FOR: 1, INT: 3, PRE: 2, VIG: 1 },
  recursos: { PV: { atual: 9, maximo: 12 }, SAN: { atual: 8, maximo: 10 }, PE: { atual: 4, maximo: 5 } },
  pericias: { INVESTIGACAO: 'VETERANO' },
};

const DND: FichaDnd = {
  id: 9, sistema: 'DND', jogador: 'Arthur', personagem: 'Brann', genero: 'M', idade: null, retratoVersao: null,
  edicao: 'D2024', classe: 'Guerreiro', especie: 'Anão', antecedente: null, nivel: 5,
  atributos: { FOR: 16, DES: 12, CON: 14, INT: 8, SAB: 10, CAR: 9 },
  recursos: { PV: { atual: 30, maximo: 44 }, PV_TEMP: { atual: 0, maximo: null } },
  ca: 18, inspiracao: false, salvaguardas: ['FOR', 'CON'], pericias: { ATLETISMO: 'PROFICIENTE' },
};

function montar(buscarFicha: (id: number) => unknown, id = '7') {
  TestBed.configureTestingModule({
    imports: [EdicaoFicha],
    providers: [provideRouter([]), { provide: FichaApi, useValue: { versaoFichas: signal(0), buscarFicha } }],
  });
  const fixture = TestBed.createComponent(EdicaoFicha);
  fixture.componentRef.setInput('id', id);
  fixture.detectChanges();
  return { fixture, el: fixture.nativeElement as HTMLElement };
}

describe('EdicaoFicha', () => {
  it('ficha de Ordem abre o formulario de Ordem, com a ficha', () => {
    const { fixture, el } = montar(vi.fn(() => of(ORDEM as Ficha)));
    const ordem = fixture.debugElement.query((d) => d.componentInstance instanceof FormularioOrdem)
      .componentInstance as FormularioOrdem;
    expect(ordem.form.controls.personagem.value).toBe('Detetive Lia');
    expect(el.querySelector('app-formulario-dnd')).toBeNull();
  });

  it('ficha de D&D abre o formulario de D&D, sem cair no de Ordem', () => {
    const { fixture, el } = montar(vi.fn(() => of(DND as Ficha)), '9');
    const dnd = fixture.debugElement.query((d) => d.componentInstance instanceof FormularioDnd)
      .componentInstance as FormularioDnd;
    expect(dnd.form.controls.personagem.value).toBe('Brann');
    expect(el.querySelector('app-formulario-ordem')).toBeNull();
  });

  it('nao mostra nenhum formulario enquanto a ficha carrega', () => {
    const { el } = montar(vi.fn(() => new Subject<Ficha>()));
    expect(el.textContent).toContain('Carregando ficha…');
    expect(el.querySelector('form')).toBeNull();
    expect(el.querySelector('app-formulario-ordem')).toBeNull();
    expect(el.querySelector('app-formulario-dnd')).toBeNull();
    expect(el.querySelector('button[type="submit"]')).toBeNull();
  });

  it('404 mostra "Ficha nao encontrada" e nao deixa salvar', () => {
    const erro: ApiError = { status: 404, titulo: 'Ficha não encontrada', detalhe: 'Não existe ficha com o id 7.', campos: {} };
    const { el } = montar(vi.fn(() => throwError(() => erro)));
    expect(el.textContent).toContain('Ficha não encontrada');
    expect(el.querySelector('a[href="/fichas"]')?.textContent).toContain('Voltar para as fichas');
    expect(el.querySelector('form')).toBeNull();
    expect(el.querySelector('button[type="submit"]')).toBeNull();
  });

  it('servidor indisponivel permite tentar de novo', () => {
    const erro: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'Pode estar acordando.', campos: {} };
    const buscarFicha = vi
      .fn()
      .mockReturnValueOnce(throwError(() => erro))
      .mockReturnValueOnce(of(ORDEM as Ficha));
    const { fixture, el } = montar(buscarFicha);
    expect(el.querySelector('form')).toBeNull();
    expect(el.textContent).toContain('Servidor indisponível');

    (Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes('Tentar de novo')) as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(buscarFicha).toHaveBeenCalledTimes(2);
    expect(el.querySelector('form')).not.toBeNull();
    expect(el.querySelector('app-formulario-ordem')).not.toBeNull();
  });
});
