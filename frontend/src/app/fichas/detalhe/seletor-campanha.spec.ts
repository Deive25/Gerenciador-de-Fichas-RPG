import { TestBed } from '@angular/core/testing';
import { NEVER, of, throwError } from 'rxjs';
import { CampanhaApi } from '../../campanhas/campanha-api';
import { CampanhaResumo } from '../../campanhas/campanha.model';
import { ApiError } from '../../core/api-error';
import { FichaApi } from '../ficha-api';
import { Ficha } from '../ficha.model';
import { FICHA_DE_TESTE } from '../testes/ficha-de-teste';
import { SeletorCampanha } from './seletor-campanha';

const MESA: CampanhaResumo = { id: 3, nome: 'Mesa de sexta', papel: 'JOGADOR', mestre: { id: 2, nome: 'Bia' }, fichas: 2 };
const OUTRA: CampanhaResumo = { id: 5, nome: 'Calamidade', papel: 'MESTRE', mestre: { id: 1, nome: 'Arthur' }, fichas: 0 };

function montar(ficha: Ficha, apis: { listar?: unknown; trocarCampanha?: unknown }) {
  TestBed.configureTestingModule({
    imports: [SeletorCampanha],
    providers: [
      { provide: CampanhaApi, useValue: { listar: () => of([MESA, OUTRA]), ...(apis.listar ? { listar: apis.listar } : {}) } },
      { provide: FichaApi, useValue: { trocarCampanha: apis.trocarCampanha } },
    ],
  });
  const fixture = TestBed.createComponent(SeletorCampanha);
  fixture.componentRef.setInput('ficha', ficha);
  const trocadas: Ficha[] = [];
  fixture.componentInstance.atualizada.subscribe((f) => trocadas.push(f));
  fixture.detectChanges();
  const el = fixture.nativeElement as HTMLElement;
  const seletor = () => el.querySelector<HTMLSelectElement>('select')!;
  const escolher = (valor: string) => {
    seletor().value = valor;
    seletor().dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };
  return { fixture, el, seletor, escolher, trocadas };
}

describe('SeletorCampanha', () => {
  it('tem o rótulo "Campanha" e as opções: Sem campanha e as campanhas da pessoa', () => {
    const { el, seletor } = montar(FICHA_DE_TESTE, {});
    expect(el.querySelector('label')?.textContent).toContain('Campanha');
    expect(el.querySelector('label')?.getAttribute('for')).toBe(seletor().id);
    expect(Array.from(seletor().options).map((o) => o.textContent?.trim())).toEqual([
      'Sem campanha',
      'Mesa de sexta',
      'Calamidade',
    ]);
    expect(seletor().value).toBe('');
  });

  it('seleciona a campanha atual da ficha', () => {
    const ficha = { ...FICHA_DE_TESTE, campanha: { id: 5, nome: 'Calamidade' } };
    const { seletor } = montar(ficha, {});
    expect(seletor().value).toBe('5');
  });

  it('escolher uma campanha chama trocarCampanha e entrega a ficha nova', () => {
    const nova = { ...FICHA_DE_TESTE, campanha: { id: 3, nome: 'Mesa de sexta' } };
    const trocarCampanha = vi.fn(() => of(nova));
    const { el, escolher, trocadas } = montar(FICHA_DE_TESTE, { trocarCampanha });

    escolher('3');

    expect(trocarCampanha).toHaveBeenCalledWith(7, 3);
    expect(trocadas).toEqual([nova]);
    expect(el.querySelector('[role="status"]')?.textContent).toContain('Campanha alterada');
  });

  it('escolher Sem campanha envia null', () => {
    const ficha = { ...FICHA_DE_TESTE, campanha: { id: 3, nome: 'Mesa de sexta' } };
    const trocarCampanha = vi.fn(() => of({ ...ficha, campanha: null }));
    const { el, escolher } = montar(ficha, { trocarCampanha });

    escolher('');

    expect(trocarCampanha).toHaveBeenCalledWith(7, null);
    expect(el.querySelector('[role="status"]')?.textContent).toContain('sem campanha');
  });

  it('enquanto troca, o seletor fica desabilitado', () => {
    const { seletor, escolher } = montar(FICHA_DE_TESTE, { trocarCampanha: () => NEVER });
    escolher('3');
    expect(seletor().disabled).toBe(true);
  });

  it('se a API recusar, mostra o erro e volta à campanha de antes', () => {
    const erro: ApiError = { status: 404, titulo: 'Campanha não encontrada', detalhe: 'Não achei.', campos: {} };
    const { el, seletor, escolher, trocadas } = montar(FICHA_DE_TESTE, { trocarCampanha: () => throwError(() => erro) });

    escolher('3');

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Campanha não encontrada');
    expect(seletor().value).toBe('');
    expect(seletor().disabled).toBe(false);
    expect(trocadas).toEqual([]);
  });

  it('a campanha da ficha aparece mesmo que a lista não a traga', () => {
    const ficha = { ...FICHA_DE_TESTE, campanha: { id: 9, nome: 'Antiga' } };
    const { seletor } = montar(ficha, { listar: () => of([]) });
    expect(Array.from(seletor().options).map((o) => o.textContent?.trim())).toEqual(['Sem campanha', 'Antiga']);
    expect(seletor().value).toBe('9');
  });

  it('se a lista não carregar, o seletor só oferece a campanha atual e avisa', () => {
    const erro: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'Tente de novo.', campos: {} };
    const { el, seletor } = montar(FICHA_DE_TESTE, { listar: () => throwError(() => erro) });
    expect(seletor().options.length).toBe(1);
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Servidor indisponível');
  });
});
