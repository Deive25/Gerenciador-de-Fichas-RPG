import { TestBed } from '@angular/core/testing';
import { ApiError } from '../core/api-error';
import { CampanhaEntrada } from './campanha.model';
import { FormularioCampanha } from './formulario-campanha';

function montar(inicial?: CampanhaEntrada) {
  TestBed.configureTestingModule({ imports: [FormularioCampanha] });
  const fixture = TestBed.createComponent(FormularioCampanha);
  if (inicial) {
    fixture.componentRef.setInput('inicial', inicial);
  }
  fixture.detectChanges();
  const el = fixture.nativeElement as HTMLElement;
  const enviados: CampanhaEntrada[] = [];
  fixture.componentInstance.enviar.subscribe((e) => enviados.push(e));
  const cancelou = vi.fn();
  fixture.componentInstance.cancelar.subscribe(cancelou);
  const campo = (nome: string) => el.querySelector<HTMLInputElement>(`[formControlName="${nome}"]`)!;
  const digitar = (nome: string, valor: string) => {
    campo(nome).value = valor;
    campo(nome).dispatchEvent(new Event('input'));
  };
  const submeter = () => {
    el.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();
  };
  return { fixture, el, enviados, cancelou, digitar, submeter, campo };
}

describe('FormularioCampanha', () => {
  it('envia o nome sem espaços nas pontas e a descrição vazia como null', () => {
    const { enviados, digitar, submeter } = montar();
    digitar('nome', '  Mesa de sexta  ');
    submeter();
    expect(enviados).toEqual([{ nome: 'Mesa de sexta', descricao: null }]);
  });

  it('envia a descrição quando preenchida', () => {
    const { enviados, digitar, submeter } = montar();
    digitar('nome', 'Mesa');
    digitar('descricao', 'Toda sexta');
    submeter();
    expect(enviados).toEqual([{ nome: 'Mesa', descricao: 'Toda sexta' }]);
  });

  it('nome em branco não envia e mostra "Obrigatório"', () => {
    const { enviados, el, digitar, submeter } = montar();
    digitar('nome', '   ');
    submeter();
    expect(enviados).toEqual([]);
    expect(el.textContent).toContain('Obrigatório');
  });

  it('parte dos valores iniciais (edição)', () => {
    const { campo } = montar({ nome: 'Mesa', descricao: 'Texto' });
    expect(campo('nome').value).toBe('Mesa');
    expect(campo('descricao').value).toBe('Texto');
  });

  it('Cancelar avisa o pai', () => {
    const { el, cancelou } = montar();
    Array.from(el.querySelectorAll('button'))
      .find((b) => b.textContent?.includes('Cancelar'))!
      .click();
    expect(cancelou).toHaveBeenCalled();
  });

  it('erro de campo do servidor aparece no campo; erro geral, num alerta', () => {
    const { fixture, el } = montar();
    const campos: ApiError = {
      status: 400,
      titulo: 'Dados inválidos',
      detalhe: 'Há campos inválidos.',
      campos: { nome: 'nome repetido' },
    };
    fixture.componentRef.setInput('erro', campos);
    fixture.detectChanges();
    expect(el.textContent).toContain('nome repetido');

    const geral: ApiError = { status: 0, titulo: 'Servidor indisponível', detalhe: 'Tente de novo.', campos: {} };
    fixture.componentRef.setInput('erro', geral);
    fixture.detectChanges();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Tente de novo.');
  });

  it('enquanto envia, o botão fica desabilitado', () => {
    const { fixture, el } = montar();
    fixture.componentRef.setInput('enviando', true);
    fixture.detectChanges();
    expect(el.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(true);
  });
});
