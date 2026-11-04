import { HttpErrorResponse } from '@angular/common/http';
import { paraApiError } from './api-error';

describe('paraApiError', () => {
  it('traduz ProblemDetail de validacao', () => {
    const erro = paraApiError(
      new HttpErrorResponse({
        status: 400,
        error: {
          title: 'Dados inválidos',
          detail: 'Há campos inválidos na requisição. Veja a propriedade errors.',
          status: 400,
          errors: { jogador: 'é obrigatório' },
        },
      }),
    );
    expect(erro.status).toBe(400);
    expect(erro.titulo).toBe('Dados inválidos');
    expect(erro.detalhe).toBe('Há campos inválidos na requisição. Veja a propriedade errors.');
    expect(erro.campos['jogador']).toBe('é obrigatório');
  });

  it('trata servidor inacessivel', () => {
    const erro = paraApiError(new HttpErrorResponse({ status: 0 }));
    expect(erro.status).toBe(0);
    expect(erro.titulo).toBe('Servidor indisponível');
    expect(erro.detalhe).toBe(
      'Não foi possível falar com o servidor. Ele pode estar acordando; tente de novo em alguns segundos.',
    );
    expect(erro.campos).toEqual({});
  });

  it('trata erro sem corpo', () => {
    const erro = paraApiError(new HttpErrorResponse({ status: 502 }));
    expect(erro.status).toBe(502);
    expect(erro.titulo).toBe('Erro inesperado');
    expect(erro.detalhe).toBe('Algo deu errado. Tente de novo.');
  });
});
