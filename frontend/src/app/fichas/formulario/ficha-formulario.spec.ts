import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { FichaApi } from '../ficha-api';
import { Ficha } from '../ficha.model';
import { FichaFormulario, paraEntrada, valorVazio } from './ficha-formulario';

const FICHA: Ficha = {
  id: 7, jogador: 'Arthur', personagem: 'Detetive Lia', genero: 'F', idade: 34,
  vidaAtual: 9, vidaMaxima: 12, sanidadeAtual: 48, sanidadeMaxima: 60,
  forca: 45, destreza: null, constituicao: null, inteligencia: null, carisma: null,
  sabedoria: null, tamanho: null, sorte: null, poder: null,
  pericias: { INVESTIGACAO: 70, PERCEPCAO: 60 },
};

function montar(api: Partial<Record<keyof FichaApi, unknown>>, id?: string) {
  TestBed.configureTestingModule({
    imports: [FichaFormulario],
    providers: [provideRouter([]), { provide: FichaApi, useValue: { versaoFichas: signal(0), ...api } }],
  });
  const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  const fixture = TestBed.createComponent(FichaFormulario);
  if (id) {
    fixture.componentRef.setInput('id', id);
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

function preencherObrigatorios(componente: FichaFormulario) {
  componente.form.patchValue({ jogador: 'Arthur', personagem: 'Lia', genero: 'F' });
}

describe('paraEntrada', () => {
  it('converte vazio em null e texto em numero', () => {
    const valor = valorVazio();
    valor.jogador = ' Arthur ';
    valor.personagem = 'Lia';
    valor.genero = 'F';
    valor.idade = '';
    valor.forca = '45';
    valor.vidaAtual = '-2';
    valor.pericias.CURA = '70';
    valor.pericias.OCULTISMO = '';

    const entrada = paraEntrada(valor);

    expect(entrada.jogador).toBe('Arthur');
    expect(entrada.idade).toBeNull();
    expect(entrada.forca).toBe(45);
    expect(entrada.vidaAtual).toBe(-2);
    expect(entrada.pericias).toEqual({ CURA: 70 });
  });
});

describe('FichaFormulario', () => {
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
    componente.form.controls.forca.setValue('1.5');
    salvar();
    expect(componente.form.invalid).toBe(true);
    expect(el.textContent).toContain('Use um número inteiro');
    expect(criar).not.toHaveBeenCalled();
  });

  it('cria e navega para a ficha', () => {
    const criar = vi.fn(() => of({ ...FICHA, id: 9 }));
    const { navegar, salvar, componente } = montar({ criar });
    preencherObrigatorios(componente);
    salvar();
    expect(criar).toHaveBeenCalledTimes(1);
    expect(navegar).toHaveBeenCalledWith(['/fichas', 9]);
  });

  it('mostra o erro do servidor no campo', () => {
    const erro: ApiError = {
      status: 400, titulo: 'Dados inválidos', detalhe: 'Há campos inválidos.',
      campos: { personagem: 'deve ter no máximo 255 caracteres' },
    };
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) });
    preencherObrigatorios(componente);
    salvar();
    expect(el.querySelector('[data-campo="personagem"]')?.textContent).toContain('deve ter no máximo 255 caracteres');
  });

  it('mostra erro geral quando o servidor esta indisponivel', () => {
    const erro: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'Pode estar acordando.', campos: {} };
    const { el, salvar, componente } = montar({ criar: vi.fn(() => throwError(() => erro)) });
    preencherObrigatorios(componente);
    salvar();
    expect(el.textContent).toContain('Servidor indisponível');
  });

  it('nao envia duas vezes com clique duplo', () => {
    const resposta = new Subject<Ficha>();
    const criar = vi.fn(() => resposta);
    const { salvar, componente } = montar({ criar });
    preencherObrigatorios(componente);
    salvar();
    salvar();
    expect(criar).toHaveBeenCalledTimes(1);
  });

  it('edita com a ficha carregada', () => {
    const atualizar = vi.fn(() => of(FICHA));
    const { salvar, componente, navegar } = montar({ buscar: vi.fn(() => of(FICHA)), atualizar }, '7');
    expect(componente.form.controls.personagem.value).toBe('Detetive Lia');
    expect(componente.form.controls.pericias.controls.INVESTIGACAO.value).toBe('70');
    salvar();
    expect(atualizar).toHaveBeenCalledTimes(1);
    const [id, entrada] = atualizar.mock.calls[0] as unknown as [number, Ficha];
    expect(id).toBe(7);
    expect(entrada.pericias).toEqual({ INVESTIGACAO: 70, PERCEPCAO: 60 });
    expect('id' in entrada).toBe(false);
    expect(navegar).toHaveBeenCalledWith(['/fichas', 7]);
  });
});
