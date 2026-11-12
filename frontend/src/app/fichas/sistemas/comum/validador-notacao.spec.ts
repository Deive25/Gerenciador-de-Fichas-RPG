import { FormControl } from '@angular/forms';
import { mensagemDoControle, notacao } from './formulario-comum';

describe('notacao (validador)', () => {
  const controle = (valor: string) => new FormControl(valor, { validators: [notacao()] });

  it('aceita notacao valida', () => {
    expect(controle('1d8+3').errors).toBeNull();
    expect(controle('2d6+1d4+2').errors).toBeNull();
  });

  it('vazio fica para o required', () => {
    expect(controle('').errors).toBeNull();
    expect(controle('   ').errors).toBeNull();
  });

  it('recusa notacao invalida com a mensagem', () => {
    expect(controle('1d7').errors).toEqual({ notacao: 'Não existe d7. Use d4, d6, d8, d10, d12, d20 ou d100.' });
    expect(controle('abc').errors?.['notacao']).toContain('Não entendi');
  });

  it('a mensagem do controle mostra o erro de notacao depois de tocado', () => {
    const c = controle('1d7');
    expect(mensagemDoControle(c)).toBeNull();
    c.markAsTouched();
    expect(mensagemDoControle(c)).toBe('Não existe d7. Use d4, d6, d8, d10, d12, d20 ou d100.');
  });
});
