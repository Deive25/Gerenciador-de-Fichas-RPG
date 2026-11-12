import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/api-error';
import { FichaApi } from '../../ficha-api';
import { FichaOrdem, FichaOrdemEntrada } from '../../ficha.model';
import { FormularioOrdem, paraEntrada, valorVazio } from './formulario-ordem';

const FICHA: FichaOrdem = {
  id: 7, sistema: 'ORDEM_PARANORMAL', jogador: 'Arthur', personagem: 'Detetive Lia', genero: 'F', idade: 34,
  retratoVersao: 1772366400250, classe: 'OCULTISTA', origem: 'Acadêmica', nex: 35,
  atributos: { AGI: 1, FOR: 1, INT: 3, PRE: 2, VIG: 1 },
  recursos: { PV: { atual: 9, maximo: 12 }, SAN: { atual: 8, maximo: 10 }, PE: { atual: 4, maximo: 5 } },
  pericias: { INVESTIGACAO: 'VETERANO', PERCEPCAO: 'TREINADO' },
  descricao: { aparencia: null, historia: null, anotacoes: null },
  habilidades: [],
  itens: [],
  rituais: [],
  ataques: [],
};

/** Ficha vinda da conversão do d100: sem classe, PE vazio, sem perícias, NEX 5 e atributos 1. */
const CONVERTIDA: FichaOrdem = {
  id: 8, sistema: 'ORDEM_PARANORMAL', jogador: 'Arthur', personagem: 'Antigo', genero: 'M', idade: null,
  retratoVersao: null, classe: null, origem: null, nex: 5,
  atributos: { AGI: 1, FOR: 1, INT: 1, PRE: 1, VIG: 1 },
  recursos: { PV: { atual: 9, maximo: 12 }, SAN: { atual: 8, maximo: 10 }, PE: { atual: null, maximo: null } },
  pericias: {},
  descricao: { aparencia: null, historia: null, anotacoes: null },
  habilidades: [],
  itens: [],
  rituais: [],
  ataques: [],
};

/** `ficha` presente = modo editar (EdicaoFicha passa a ficha ja carregada); ausente = nova ficha. */
function montar(api: Partial<Record<keyof FichaApi, unknown>>, ficha?: FichaOrdem) {
  TestBed.configureTestingModule({
    imports: [FormularioOrdem],
    providers: [provideRouter([]), { provide: FichaApi, useValue: { versaoFichas: signal(0), ...api } }],
  });
  const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  const fixture = TestBed.createComponent(FormularioOrdem);
  if (ficha) {
    fixture.componentRef.setInput('ficha', ficha);
  }
  fixture.detectChanges();
  const el = fixture.nativeElement as HTMLElement;
  const salvar = () => {
    // O botão de envio troca o texto para "Salvando…" enquanto espera a API.
    (el.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    fixture.detectChanges();
  };
  return { fixture, el, navegar, salvar, componente: fixture.componentInstance };
}

function preencherObrigatorios(componente: FormularioOrdem) {
  componente.form.patchValue({ jogador: 'Arthur', personagem: 'Lia', genero: 'F' });
}

function erroApi(campos: Record<string, string>): ApiError {
  return { status: 400, titulo: 'Dados inválidos', detalhe: 'Há campos inválidos.', campos };
}

describe('paraEntrada', () => {
  it('converte vazio em null, texto em numero, e as pericias sem grau saem do objeto', () => {
    const valor = valorVazio();
    valor.jogador = ' Arthur ';
    valor.personagem = 'Lia';
    valor.genero = 'F';
    valor.idade = '';
    valor.classe = '';
    valor.origem = '  ';
    valor.nex = '35';
    valor.atributos.INT = '3';
    valor.recursos.PV.atual = '9';
    valor.pericias.INVESTIGACAO = 'VETERANO';
    valor.pericias.OCULTISMO = '';

    const entrada = paraEntrada(valor);

    expect(entrada.sistema).toBe('ORDEM_PARANORMAL');
    expect(entrada.jogador).toBe('Arthur');
    expect(entrada.idade).toBeNull();
    expect(entrada.classe).toBeNull();
    expect(entrada.origem).toBeNull();
    expect(entrada.nex).toBe(35);
    expect(entrada.atributos.INT).toBe(3);
    expect(entrada.recursos.PV).toEqual({ atual: 9, maximo: null });
    expect(entrada.pericias).toEqual({ INVESTIGACAO: 'VETERANO' });
  });
});

describe('FormularioOrdem', () => {
  it('exige os campos de texto', () => {
    const criar = vi.fn();
    const { el, salvar } = montar({ criar });
    salvar();
    expect(el.textContent?.match(/Obrigatório/g)?.length).toBe(3);
    expect(criar).not.toHaveBeenCalled();
  });

  it('bloqueia numero nao inteiro', () => {
    const criar = vi.fn();
    const { el, salvar, componente } = montar({ criar });
    preencherObrigatorios(componente);
    componente.form.controls.atributos.controls.FOR.setValue('1.5');
    salvar();
    expect(componente.form.invalid).toBe(true);
    expect(el.textContent).toContain('Use um número inteiro');
    expect(criar).not.toHaveBeenCalled();
  });

  it('bloqueia NEX fora de 5 a 99 e atributo fora de 0 a 5', () => {
    const criar = vi.fn();
    const { el, salvar, componente } = montar({ criar });
    preencherObrigatorios(componente);
    componente.form.controls.nex.setValue('100');
    componente.form.controls.atributos.controls.AGI.setValue('6');
    salvar();
    expect(el.querySelector('[data-campo="nex"]')?.textContent).toContain('De 5 a 99');
    expect(el.querySelector('[data-campo="AGI"]')?.textContent).toContain('De 0 a 5');
    expect(criar).not.toHaveBeenCalled();
  });

  it('cria com sistema ORDEM_PARANORMAL e navega para a ficha', () => {
    const criar = vi.fn(() => of({ ...FICHA, id: 9 }));
    const { navegar, salvar, componente } = montar({ criar });
    preencherObrigatorios(componente);
    salvar();
    expect(criar).toHaveBeenCalledTimes(1);
    const [entrada] = criar.mock.calls[0] as unknown as [FichaOrdemEntrada];
    expect(entrada.sistema).toBe('ORDEM_PARANORMAL');
    expect(entrada.nex).toBe(5);
    expect(entrada.classe).toBeNull();
    expect(navegar).toHaveBeenCalledWith(['/fichas', 9]);
  });

  it('mostra o erro do servidor no campo', () => {
    const erro = erroApi({ personagem: 'deve ter no máximo 255 caracteres' });
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) });
    preencherObrigatorios(componente);
    salvar();
    expect(el.querySelector('[data-campo="personagem"]')?.textContent).toContain('deve ter no máximo 255 caracteres');
  });

  it('erro de atributo no formato do Bean Validation (atributos[AGI]) cai no campo certo', () => {
    const erro = erroApi({ 'atributos[AGI]': 'deve estar entre 0 e 5' });
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) });
    preencherObrigatorios(componente);
    salvar();
    expect(el.querySelector('[data-campo="AGI"]')?.textContent).toContain('deve estar entre 0 e 5');
  });

  it('erro de atributo no formato da regra (atributos.VIG) cai no campo certo', () => {
    const erro = erroApi({ 'atributos.VIG': 'Vigor é obrigatório' });
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) });
    preencherObrigatorios(componente);
    salvar();
    expect(el.querySelector('[data-campo="VIG"]')?.textContent).toContain('Vigor é obrigatório');
  });

  it('erro de recurso (recursos[PV].atual) cai no campo do recurso', () => {
    const erro = erroApi({ 'recursos[PV].atual': 'deve ser maior ou igual a 0' });
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) });
    preencherObrigatorios(componente);
    salvar();
    expect(el.querySelector('[data-campo="PV-atual"]')?.textContent).toContain('deve ser maior ou igual a 0');
  });

  it('erro de grau (pericias.INVESTIGACAO) cai na pericia, sem erro geral', () => {
    const erro = erroApi({ 'pericias.INVESTIGACAO': 'Veterano exige NEX 35% ou mais' });
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) });
    preencherObrigatorios(componente);
    salvar();
    expect(el.querySelector('[data-campo="INVESTIGACAO"]')?.textContent).toContain('Veterano exige NEX 35% ou mais');
    expect(el.querySelector('.acoes [role="alert"]')).toBeNull();
  });

  it('baixar o NEX abaixo do grau ja escolhido mostra o erro do servidor na pericia', () => {
    const erro = erroApi({ 'pericias.INVESTIGACAO': 'Veterano exige NEX 35% ou mais' });
    const atualizar = vi.fn(() => throwError(() => erro));
    const { el, salvar, componente } = montar({ atualizar }, FICHA);
    componente.form.controls.nex.setValue('30');
    salvar();
    expect(el.querySelector('[data-campo="INVESTIGACAO"]')?.textContent).toContain('Veterano exige NEX 35% ou mais');
  });

  it('corrigir o NEX limpa o erro do servidor das pericias, para o formulario poder ser enviado', () => {
    const erro = erroApi({ 'pericias.INVESTIGACAO': 'Veterano exige NEX 35% ou mais' });
    const atualizar = vi.fn().mockReturnValueOnce(throwError(() => erro)).mockReturnValueOnce(of(FICHA));
    const { fixture, salvar, componente } = montar({ atualizar }, FICHA);
    componente.form.controls.nex.setValue('30');
    salvar();
    expect(componente.form.controls.pericias.controls.INVESTIGACAO.errors?.['servidor']).toBeTruthy();

    componente.form.controls.nex.setValue('35');
    fixture.detectChanges();
    expect(componente.form.valid).toBe(true);
    salvar();
    expect(atualizar).toHaveBeenCalledTimes(2);
  });

  it('erro do servidor na pericia leva o foco ate o select dela', () => {
    const erro = erroApi({ 'pericias.INVESTIGACAO': 'Veterano exige NEX 35% ou mais' });
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) });
    preencherObrigatorios(componente);
    salvar();
    expect(document.activeElement).toBe(el.querySelector('[data-campo="INVESTIGACAO"] mat-select'));
  });

  it('erro do servidor num campo de texto leva o foco ate ele', () => {
    const erro = erroApi({ personagem: 'deve ter no máximo 255 caracteres' });
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) });
    preencherObrigatorios(componente);
    salvar();
    expect(document.activeElement).toBe(el.querySelector('[data-campo="personagem"] input'));
  });

  it('erro geral (sem campo) nao rouba o foco', () => {
    const erro: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'x', campos: {} };
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) });
    preencherObrigatorios(componente);
    (document.activeElement as HTMLElement | null)?.blur();
    salvar();
    expect(document.activeElement).not.toBe(el.querySelector('[data-campo="personagem"] input'));
    expect(el.contains(document.activeElement) && document.activeElement?.matches('input, mat-select')).toBeFalsy();
  });

  it('aceita recursos acima de 9999 (a API nao tem teto) e envia', () => {
    const criar = vi.fn(() => of({ ...FICHA, id: 9 }));
    const { salvar, componente } = montar({ criar });
    preencherObrigatorios(componente);
    componente.form.controls.recursos.controls.PV.controls.maximo.setValue('10000');
    salvar();
    expect(criar).toHaveBeenCalledTimes(1);
    const [entrada] = criar.mock.calls[0] as unknown as [FichaOrdemEntrada];
    expect(entrada.recursos.PV.maximo).toBe(10000);
  });

  it('recurso negativo continua barrado', () => {
    const { el, salvar, componente } = montar({ criar: vi.fn() });
    preencherObrigatorios(componente);
    componente.form.controls.recursos.controls.PV.controls.atual.setValue('-1');
    salvar();
    expect(el.querySelector('[data-campo="PV-atual"]')?.textContent).toContain('0 ou mais');
  });

  it('o select mostra so o nome do grau, sem a dica do NEX', async () => {
    const { fixture, el } = montar({}, FICHA);
    fixture.componentInstance.form.controls.nex.setValue('30');
    await fixture.whenStable();
    fixture.detectChanges();
    const trigger = el.querySelector('[data-campo="INVESTIGACAO"] .mat-mdc-select-value');
    expect(trigger?.textContent?.trim()).toBe('Veterano');
  });

  it('mostra erro geral quando o servidor esta indisponivel', () => {
    const erro: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'Pode estar acordando.', campos: {} };
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) });
    preencherObrigatorios(componente);
    salvar();
    expect(el.textContent).toContain('Servidor indisponível');
  });

  it('nao envia duas vezes com clique duplo', () => {
    const resposta = new Subject<FichaOrdem>();
    const criar = vi.fn(() => resposta);
    const { salvar, componente } = montar({ criar });
    preencherObrigatorios(componente);
    salvar();
    salvar();
    expect(criar).toHaveBeenCalledTimes(1);
  });

  it('foca o primeiro campo invalido ao salvar', () => {
    const { el, salvar } = montar({ criar: vi.fn() });
    salvar();
    expect(document.activeElement).toBe(el.querySelector('[data-campo="jogador"] input'));
  });

  it('foca uma lista (classe ou grau) quando ela e o primeiro campo invalido', () => {
    const { fixture, el, salvar, componente } = montar({ criar: vi.fn() });
    preencherObrigatorios(componente);
    componente.form.controls.pericias.controls.ACROBACIA.setErrors({ servidor: 'x' });
    fixture.detectChanges();
    salvar();
    expect(document.activeElement).toBe(el.querySelector('[data-campo="ACROBACIA"] mat-select'));
  });

  it('erro ao salvar aparece junto dos botoes, onde o usuario esta olhando', () => {
    const erro: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'Pode estar acordando.', campos: {} };
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) });
    preencherObrigatorios(componente);
    salvar();
    expect(el.querySelector('.acoes [role="alert"]')?.textContent).toContain('Servidor indisponível');
  });

  it('salvar continua bloqueado depois da resposta ate a navegacao terminar', () => {
    const criar = vi.fn(() => of({ ...FICHA, id: 9 }));
    const { el, navegar, salvar, componente } = montar({ criar });
    navegar.mockReturnValue(new Promise<boolean>(() => undefined));
    preencherObrigatorios(componente);

    salvar();
    salvar();

    expect(criar).toHaveBeenCalledTimes(1);
    expect((el.querySelector('button[type="submit"]') as HTMLButtonElement).disabled).toBe(true);
  });

  it('as dicas ficam em botoes pequenos, nunca no campo inteiro', () => {
    // No celular o tooltip desliga os gestos nativos do elemento onde está; num campo
    // inteiro isso impede rolar a tela com o dedo. Só os botões "ⓘ" podem ter dica.
    const { el } = montar({});
    const gatilhos = Array.from(el.querySelectorAll('.mat-mdc-tooltip-trigger'));
    expect(gatilhos.length).toBe(5 + 28);
    for (const gatilho of gatilhos) {
      expect(gatilho.tagName).toBe('BUTTON');
      expect(gatilho.getAttribute('aria-label')).toMatch(/^Sobre /);
    }
  });

  it('edita com a ficha carregada', () => {
    const atualizar = vi.fn(() => of(FICHA));
    const { salvar, componente, navegar } = montar({ atualizar }, FICHA);
    expect(componente.form.controls.personagem.value).toBe('Detetive Lia');
    expect(componente.form.controls.classe.value).toBe('OCULTISTA');
    expect(componente.form.controls.pericias.controls.INVESTIGACAO.value).toBe('VETERANO');
    salvar();
    expect(atualizar).toHaveBeenCalledTimes(1);
    const [id, entrada] = atualizar.mock.calls[0] as unknown as [number, FichaOrdemEntrada];
    expect(id).toBe(7);
    expect(entrada.sistema).toBe('ORDEM_PARANORMAL');
    expect(entrada.pericias).toEqual({ INVESTIGACAO: 'VETERANO', PERCEPCAO: 'TREINADO' });
    expect(entrada.recursos.PE).toEqual({ atual: 4, maximo: 5 });
    expect('id' in entrada).toBe(false);
    expect('retratoVersao' in entrada).toBe(false);
    expect(navegar).toHaveBeenCalledWith(['/fichas', 7]);
  });

  it('edita uma ficha convertida (sem classe, PE vazio, sem pericias) sem exigir a classe', async () => {
    const atualizar = vi.fn(() => of(CONVERTIDA));
    const { fixture, el, salvar, componente, navegar } = montar({ atualizar }, CONVERTIDA);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(el.querySelector('form')).not.toBeNull();
    expect(componente.form.controls.classe.value).toBe('');
    expect(el.querySelector('[data-campo="classe"]')?.textContent).toContain('A definir');

    salvar();

    expect(atualizar).toHaveBeenCalledTimes(1);
    const [id, entrada] = atualizar.mock.calls[0] as unknown as [number, FichaOrdemEntrada];
    expect(id).toBe(8);
    expect(entrada.classe).toBeNull();
    expect(entrada.origem).toBeNull();
    expect(entrada.idade).toBeNull();
    expect(entrada.recursos.PE).toEqual({ atual: null, maximo: null });
    expect(entrada.pericias).toEqual({});
    expect(navegar).toHaveBeenCalledWith(['/fichas', 8]);
  });
});

describe('FormularioOrdem: graus e NEX', () => {
  async function abrirGraus(codigo: string, nex: string) {
    const { fixture, componente } = montar({});
    componente.form.controls.nex.setValue(nex);
    fixture.detectChanges();
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const select = await loader.getHarness(MatSelectHarness.with({ selector: `[data-campo="${codigo}"] mat-select` }));
    await select.open();
    const opcoes = await select.getOptions();
    const porNome = new Map<string, { texto: string; desabilitada: boolean }>();
    for (const opcao of opcoes) {
      const texto = (await opcao.getText()).trim();
      porNome.set(texto.split(' ')[0], { texto, desabilitada: await opcao.isDisabled() });
    }
    return porNome;
  }

  it('oferece Destreinado, Treinado, Veterano e Expert', async () => {
    const graus = await abrirGraus('ACROBACIA', '99');
    expect([...graus.keys()]).toEqual(['Destreinado', 'Treinado', 'Veterano', 'Expert']);
  });

  it('com NEX 30, Veterano e Expert ficam desabilitados com a dica do NEX exigido', async () => {
    const graus = await abrirGraus('ACROBACIA', '30');
    expect(graus.get('Destreinado')?.desabilitada).toBe(false);
    expect(graus.get('Treinado')?.desabilitada).toBe(false);
    expect(graus.get('Veterano')?.desabilitada).toBe(true);
    expect(graus.get('Veterano')?.texto).toContain('exige NEX 35%');
    expect(graus.get('Expert')?.desabilitada).toBe(true);
    expect(graus.get('Expert')?.texto).toContain('exige NEX 70%');
  });

  it('com NEX 35, Veterano fica habilitado (e Expert continua desabilitado)', async () => {
    const graus = await abrirGraus('ACROBACIA', '35');
    expect(graus.get('Veterano')?.desabilitada).toBe(false);
    expect(graus.get('Veterano')?.texto).not.toContain('exige');
    expect(graus.get('Expert')?.desabilitada).toBe(true);
  });

  it('com NEX 70, Expert fica habilitado', async () => {
    const graus = await abrirGraus('ACROBACIA', '70');
    expect(graus.get('Expert')?.desabilitada).toBe(false);
  });
});
