import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/api-error';
import { FichaApi } from '../../ficha-api';
import { EdicaoDnd, FichaDnd, FichaDndEntrada } from '../../ficha.model';
import { FormularioDnd, paraEntradaDnd, valorVazioDnd } from './formulario-dnd';
import { espacosZerados } from '../../testes/espacos-zerados';

const FICHA: FichaDnd = {
  id: 9, sistema: 'DND', jogador: 'Arthur', personagem: 'Brann', genero: 'M', idade: 41, retratoVersao: 1772366400250,
  edicao: 'D2014', classe: 'Guerreiro', especie: 'Anão', antecedente: 'Soldado', nivel: 5,
  atributos: { FOR: 16, DES: 12, CON: 14, INT: 8, SAB: 10, CAR: 9 },
  recursos: { PV: { atual: 30, maximo: 44 }, PV_TEMP: { atual: 5, maximo: null } },
  ca: 18, inspiracao: true, salvaguardas: ['FOR', 'CON'], pericias: { ATLETISMO: 'PROFICIENTE', PERCEPCAO: 'ESPECIALISTA' },
  descricao: { aparencia: null, historia: null, anotacoes: null },
  habilidades: [],
  itens: [],
  moedas: { pc: 0, pp: 0, pe: 0, po: 0, pl: 0 },
  atributoConjuracao: null,
  magias: [],
  espacos: espacosZerados(),
  ataques: [],
};

function montar(api: Partial<Record<keyof FichaApi, unknown>>, opcoes: { edicao?: EdicaoDnd; ficha?: FichaDnd } = {}) {
  TestBed.configureTestingModule({
    imports: [FormularioDnd],
    providers: [provideRouter([]), { provide: FichaApi, useValue: { versaoFichas: signal(0), ...api } }],
  });
  const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  const fixture = TestBed.createComponent(FormularioDnd);
  if (opcoes.edicao) {
    fixture.componentRef.setInput('edicao', opcoes.edicao);
  }
  if (opcoes.ficha) {
    fixture.componentRef.setInput('ficha', opcoes.ficha);
  }
  fixture.detectChanges();
  const el = fixture.nativeElement as HTMLElement;
  const salvar = () => {
    (el.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    fixture.detectChanges();
  };
  return { fixture, el, navegar, salvar, componente: fixture.componentInstance };
}

function preencherObrigatorios(componente: FormularioDnd) {
  componente.form.patchValue({ jogador: 'Arthur', personagem: 'Brann', genero: 'M' });
}

function erroApi(campos: Record<string, string>): ApiError {
  return { status: 400, titulo: 'Dados inválidos', detalhe: 'Há campos inválidos.', campos };
}

describe('paraEntradaDnd', () => {
  it('converte vazio em null, salvaguardas em lista e tira as pericias sem proficiencia', () => {
    const valor = valorVazioDnd();
    valor.jogador = ' Arthur ';
    valor.personagem = 'Brann';
    valor.genero = 'M';
    valor.classe = '  ';
    valor.especie = 'Anão';
    valor.salvaguardas.CON = true;
    valor.salvaguardas.FOR = true;
    valor.recursos.PV.atual = '7';
    valor.recursos.PV_TEMP.atual = '2';
    valor.pericias.ATLETISMO = 'ESPECIALISTA';
    valor.pericias.ACROBACIA = '';

    const entrada = paraEntradaDnd(valor, 'D2014');

    expect(entrada.sistema).toBe('DND');
    expect(entrada.edicao).toBe('D2014');
    expect(entrada.jogador).toBe('Arthur');
    expect(entrada.classe).toBeNull();
    expect(entrada.especie).toBe('Anão');
    expect(entrada.idade).toBeNull();
    expect(entrada.ca).toBeNull();
    expect(entrada.inspiracao).toBe(false);
    expect(entrada.salvaguardas).toEqual(['FOR', 'CON']);
    expect(entrada.recursos).toEqual({ PV: { atual: 7, maximo: null }, PV_TEMP: { atual: 2, maximo: null } });
    expect(entrada.pericias).toEqual({ ATLETISMO: 'ESPECIALISTA' });
  });
});

describe('FormularioDnd', () => {
  it('criar a partir de dnd-2024 envia o padrao e navega para a ficha', () => {
    const criar = vi.fn(() => of({ ...FICHA, id: 12 }));
    const { navegar, salvar, componente } = montar({ criar }, { edicao: 'D2024' });
    preencherObrigatorios(componente);
    componente.form.controls.salvaguardas.controls.SAB.setValue(true);
    componente.form.controls.salvaguardas.controls.CAR.setValue(true);
    componente.form.controls.pericias.controls.FURTIVIDADE.setValue('PROFICIENTE');
    salvar();

    expect(criar).toHaveBeenCalledTimes(1);
    const [entrada] = criar.mock.calls[0] as unknown as [FichaDndEntrada];
    expect(entrada).toEqual({
      sistema: 'DND',
      jogador: 'Arthur',
      personagem: 'Brann',
      genero: 'M',
      idade: null,
      edicao: 'D2024',
      classe: null,
      especie: null,
      antecedente: null,
      nivel: 1,
      atributoConjuracao: null,
      atributos: { FOR: 10, DES: 10, CON: 10, INT: 10, SAB: 10, CAR: 10 },
      recursos: { PV: { atual: null, maximo: null }, PV_TEMP: { atual: null, maximo: null } },
      ca: null,
      inspiracao: false,
      salvaguardas: ['SAB', 'CAR'],
      pericias: { FURTIVIDADE: 'PROFICIENTE' },
    });
    expect(navegar).toHaveBeenCalledWith(['/fichas', 12]);
  });

  it('mostra o nome da edicao so para leitura e o rotulo Raca em 2014, Especie em 2024', () => {
    const antigo = montar({}, { edicao: 'D2014' });
    expect(antigo.el.textContent).toContain('D&D 5e (2014)');
    expect(antigo.el.querySelector('[data-campo="especie"] mat-label')?.textContent?.trim()).toBe('Raça');
    expect(antigo.el.querySelector('[data-campo="edicao"]')).toBeNull();
    TestBed.resetTestingModule();

    const novo = montar({}, { edicao: 'D2024' });
    expect(novo.el.textContent).toContain('D&D 2024');
    expect(novo.el.querySelector('[data-campo="especie"] mat-label')?.textContent?.trim()).toBe('Espécie');
  });

  it('exige os campos de texto', () => {
    const criar = vi.fn();
    const { el, salvar } = montar({ criar }, { edicao: 'D2024' });
    salvar();
    expect(el.textContent?.match(/Obrigatório/g)?.length).toBe(3);
    expect(criar).not.toHaveBeenCalled();
  });

  it('bloqueia nivel fora de 1 a 20 e atributo fora de 1 a 30', () => {
    const criar = vi.fn();
    const { el, salvar, componente } = montar({ criar }, { edicao: 'D2024' });
    preencherObrigatorios(componente);
    componente.form.controls.nivel.setValue('21');
    componente.form.controls.atributos.controls.DES.setValue('0');
    componente.form.controls.atributos.controls.FOR.setValue('31');
    salvar();
    expect(el.querySelector('[data-campo="nivel"]')?.textContent).toContain('De 1 a 20');
    expect(el.querySelector('[data-campo="DES"]')?.textContent).toContain('De 1 a 30');
    expect(el.querySelector('[data-campo="FOR"]')?.textContent).toContain('De 1 a 30');
    expect(criar).not.toHaveBeenCalled();
  });

  it('bloqueia numero nao inteiro', () => {
    const criar = vi.fn();
    const { el, salvar, componente } = montar({ criar }, { edicao: 'D2024' });
    preencherObrigatorios(componente);
    componente.form.controls.ca.setValue('1.5');
    salvar();
    expect(el.querySelector('[data-campo="ca"]')?.textContent).toContain('Use um número inteiro');
    expect(criar).not.toHaveBeenCalled();
  });

  it('erro de atributo no formato do Bean Validation (atributos[DES]) cai no campo e leva o foco', () => {
    const erro = erroApi({ 'atributos[DES]': 'deve estar entre 1 e 30' });
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) }, { edicao: 'D2024' });
    preencherObrigatorios(componente);
    salvar();
    expect(el.querySelector('[data-campo="DES"]')?.textContent).toContain('deve estar entre 1 e 30');
    expect(document.activeElement).toBe(el.querySelector('[data-campo="DES"] input'));
    expect(el.querySelector('.acoes [role="alert"]')).toBeNull();
  });

  it('erro de atributo no formato da regra (atributos.CON) cai no campo certo', () => {
    const erro = erroApi({ 'atributos.CON': 'Constituição é obrigatória' });
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) }, { edicao: 'D2024' });
    preencherObrigatorios(componente);
    salvar();
    expect(el.querySelector('[data-campo="CON"]')?.textContent).toContain('Constituição é obrigatória');
  });

  it('erro de recurso (recursos[PV].atual) cai no campo do recurso', () => {
    const erro = erroApi({ 'recursos[PV].atual': 'deve ser maior ou igual a 0' });
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) }, { edicao: 'D2024' });
    preencherObrigatorios(componente);
    salvar();
    expect(el.querySelector('[data-campo="PV-atual"]')?.textContent).toContain('deve ser maior ou igual a 0');
  });

  it('erro recursos.PV_TEMP.maximo cai no campo do PV temporario e leva o foco', () => {
    const erro = erroApi({ 'recursos.PV_TEMP.maximo': 'PV temporário não tem máximo' });
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) }, { edicao: 'D2024' });
    preencherObrigatorios(componente);
    salvar();
    expect(el.querySelector('[data-campo="PV_TEMP-atual"]')?.textContent).toContain('PV temporário não tem máximo');
    expect(document.activeElement).toBe(el.querySelector('[data-campo="PV_TEMP-atual"] input'));
    expect(el.querySelector('.acoes [role="alert"]')).toBeNull();
  });

  it('erro do servidor na pericia leva o foco ate o select dela', () => {
    const erro = erroApi({ 'pericias.ATLETISMO': 'grau inválido' });
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) }, { edicao: 'D2024' });
    preencherObrigatorios(componente);
    salvar();
    expect(el.querySelector('[data-campo="ATLETISMO"]')?.textContent).toContain('grau inválido');
    expect(document.activeElement).toBe(el.querySelector('[data-campo="ATLETISMO"] mat-select'));
  });

  it('foca o primeiro campo invalido ao salvar', () => {
    const { el, salvar } = montar({ criar: vi.fn() }, { edicao: 'D2024' });
    salvar();
    expect(document.activeElement).toBe(el.querySelector('[data-campo="jogador"] input'));
  });

  it('erro geral (sem campo) aparece junto dos botoes e nao rouba o foco', () => {
    const erro: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'Pode estar acordando.', campos: {} };
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) }, { edicao: 'D2024' });
    preencherObrigatorios(componente);
    (document.activeElement as HTMLElement | null)?.blur();
    salvar();
    expect(el.querySelector('.acoes [role="alert"]')?.textContent).toContain('Servidor indisponível');
    expect(el.contains(document.activeElement) && document.activeElement?.matches('input, mat-select')).toBeFalsy();
  });

  it('aceita recursos acima de 9999 (a API nao tem teto) e envia', () => {
    const criar = vi.fn(() => of({ ...FICHA, id: 9 }));
    const { salvar, componente } = montar({ criar }, { edicao: 'D2024' });
    preencherObrigatorios(componente);
    componente.form.controls.recursos.controls.PV.controls.maximo.setValue('10000');
    salvar();
    const [entrada] = criar.mock.calls[0] as unknown as [FichaDndEntrada];
    expect(entrada.recursos.PV.maximo).toBe(10000);
  });

  it('recurso negativo continua barrado', () => {
    const { el, salvar, componente } = montar({ criar: vi.fn() }, { edicao: 'D2024' });
    preencherObrigatorios(componente);
    componente.form.controls.recursos.controls.PV.controls.atual.setValue('-1');
    salvar();
    expect(el.querySelector('[data-campo="PV-atual"]')?.textContent).toContain('0 ou mais');
  });

  it('nao envia duas vezes com clique duplo', () => {
    const resposta = new Subject<FichaDnd>();
    const criar = vi.fn(() => resposta);
    const { salvar, componente } = montar({ criar }, { edicao: 'D2024' });
    preencherObrigatorios(componente);
    salvar();
    salvar();
    expect(criar).toHaveBeenCalledTimes(1);
  });

  it('salvar continua bloqueado depois da resposta ate a navegacao terminar', () => {
    const criar = vi.fn(() => of({ ...FICHA, id: 12 }));
    const { el, navegar, salvar, componente } = montar({ criar }, { edicao: 'D2024' });
    navegar.mockReturnValue(new Promise<boolean>(() => undefined));
    preencherObrigatorios(componente);
    salvar();
    salvar();
    expect(criar).toHaveBeenCalledTimes(1);
    expect((el.querySelector('button[type="submit"]') as HTMLButtonElement).disabled).toBe(true);
  });

  it('as caixas de salvaguarda ficam num bloco que limpa a legend flutuante', () => {
    // jsdom nao mede layout; o scss tem clear: both em .marcas, e aqui garantimos que o bloco existe.
    const { el } = montar({}, { edicao: 'D2024' });
    expect(el.querySelectorAll('fieldset .marcas').length).toBe(2);
    expect(el.querySelectorAll('.marcas mat-checkbox').length).toBe(7);
  });

  it('as dicas ficam em botoes pequenos: 6 atributos e 18 pericias', () => {
    const { el } = montar({}, { edicao: 'D2024' });
    const gatilhos = Array.from(el.querySelectorAll('.mat-mdc-tooltip-trigger'));
    expect(gatilhos.length).toBe(6 + 18);
    for (const gatilho of gatilhos) {
      expect(gatilho.tagName).toBe('BUTTON');
      expect(gatilho.getAttribute('aria-label')).toMatch(/^Sobre /);
    }
  });

  it('editar uma ficha carregada salva com o mesmo id e a mesma edicao, sem id nem retratoVersao', () => {
    const atualizar = vi.fn(() => of(FICHA));
    const { salvar, componente, navegar } = montar({ atualizar }, { ficha: FICHA });
    expect(componente.form.controls.personagem.value).toBe('Brann');
    expect(componente.form.controls.nivel.value).toBe('5');
    expect(componente.form.controls.salvaguardas.controls.FOR.value).toBe(true);
    expect(componente.form.controls.salvaguardas.controls.DES.value).toBe(false);
    expect(componente.form.controls.pericias.controls.PERCEPCAO.value).toBe('ESPECIALISTA');
    expect(componente.form.controls.inspiracao.value).toBe(true);
    salvar();

    expect(atualizar).toHaveBeenCalledTimes(1);
    const [id, entrada] = atualizar.mock.calls[0] as unknown as [number, FichaDndEntrada];
    expect(id).toBe(9);
    expect(entrada.sistema).toBe('DND');
    expect(entrada.edicao).toBe('D2014');
    expect(entrada.nivel).toBe(5);
    expect(entrada.ca).toBe(18);
    expect(entrada.inspiracao).toBe(true);
    expect(entrada.salvaguardas).toEqual(['FOR', 'CON']);
    expect(entrada.recursos).toEqual({ PV: { atual: 30, maximo: 44 }, PV_TEMP: { atual: 5, maximo: null } });
    expect(entrada.pericias).toEqual({ ATLETISMO: 'PROFICIENTE', PERCEPCAO: 'ESPECIALISTA' });
    expect('id' in entrada).toBe(false);
    expect('retratoVersao' in entrada).toBe(false);
    expect(navegar).toHaveBeenCalledWith(['/fichas', 9]);
  });

  describe('atributo de conjuracao', () => {
    it('oferece Nenhum, Inteligencia, Sabedoria e Carisma e comeca em Nenhum numa ficha nova', async () => {
      const { fixture, el } = montar({}, { edicao: 'D2024' });
      const loader = TestbedHarnessEnvironment.loader(fixture);
      const select = await loader.getHarness(MatSelectHarness.with({ selector: '[data-campo="atributoConjuracao"] mat-select' }));
      expect(el.querySelector('[data-campo="atributoConjuracao"] mat-label')?.textContent?.trim()).toBe('Atributo de conjuração');
      await select.open();
      const nomes = await Promise.all((await select.getOptions()).map(async (o) => (await o.getText()).trim()));
      expect(nomes).toEqual(['Nenhum', 'Inteligência', 'Sabedoria', 'Carisma']);
      await select.close();
      expect(await select.getValueText()).toBe('Nenhum');
    });

    it('escolher Sabedoria e salvar manda atributoConjuracao SAB', async () => {
      const criar = vi.fn(() => of({ ...FICHA, id: 12 }));
      const { fixture, salvar, componente } = montar({ criar }, { edicao: 'D2024' });
      preencherObrigatorios(componente);
      const loader = TestbedHarnessEnvironment.loader(fixture);
      const select = await loader.getHarness(MatSelectHarness.with({ selector: '[data-campo="atributoConjuracao"] mat-select' }));
      await select.clickOptions({ text: 'Sabedoria' });
      salvar();

      const [entrada] = criar.mock.calls[0] as unknown as [FichaDndEntrada];
      expect(entrada.atributoConjuracao).toBe('SAB');
    });

    it('a edicao traz o atributo da ficha e voltar para Nenhum manda null', async () => {
      const atualizar = vi.fn(() => of(FICHA));
      const { fixture, salvar } = montar({ atualizar }, { ficha: { ...FICHA, atributoConjuracao: 'INT' } });
      const loader = TestbedHarnessEnvironment.loader(fixture);
      const select = await loader.getHarness(MatSelectHarness.with({ selector: '[data-campo="atributoConjuracao"] mat-select' }));
      expect(await select.getValueText()).toBe('Inteligência');
      await select.clickOptions({ text: 'Nenhum' });
      salvar();

      const [, entrada] = atualizar.mock.calls[0] as unknown as [number, FichaDndEntrada];
      expect(entrada.atributoConjuracao).toBeNull();
    });

    it('erro do servidor em atributoConjuracao aparece no campo', () => {
      const atualizar = vi.fn(() => throwError(() => erroApi({ atributoConjuracao: 'deve ser INT, SAB ou CAR' })));
      const { el, salvar } = montar({ atualizar }, { ficha: FICHA });
      salvar();
      expect(el.querySelector('[data-campo="atributoConjuracao"]')?.textContent).toContain('deve ser INT, SAB ou CAR');
    });
  });

  it('o select da pericia oferece os tres graus e mostra so o nome', async () => {
    const { fixture, el } = montar({}, { ficha: FICHA });
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const select = await loader.getHarness(MatSelectHarness.with({ selector: '[data-campo="ATLETISMO"] mat-select' }));
    await select.open();
    const nomes = await Promise.all((await select.getOptions()).map(async (o) => (await o.getText()).trim()));
    expect(nomes).toEqual(['Sem proficiência', 'Proficiente', 'Especialista']);
    await select.close();
    expect(el.querySelector('[data-campo="ATLETISMO"] .mat-mdc-select-value')?.textContent?.trim()).toBe('Proficiente');
    expect(el.querySelector('[data-campo="ACROBACIA"] .mat-mdc-select-value')?.textContent?.trim()).toBe('Sem proficiência');
  });
});
