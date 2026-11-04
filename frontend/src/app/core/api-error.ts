import { HttpErrorResponse } from '@angular/common/http';

/** Erro da API já traduzido para a interface (a API responde em ProblemDetail, RFC 9457). */
export interface ApiError {
  status: number;
  titulo: string;
  detalhe: string;
  /** Erros de validação por campo (propriedade `errors` do ProblemDetail). */
  campos: Record<string, string>;
}

interface ProblemDetail {
  title?: string;
  detail?: string;
  errors?: Record<string, string>;
}

const INDISPONIVEL: Omit<ApiError, 'status'> = {
  titulo: 'Servidor indisponível',
  detalhe: 'Não foi possível falar com o servidor. Ele pode estar acordando; tente de novo em alguns segundos.',
  campos: {},
};

const INESPERADO: Omit<ApiError, 'status'> = {
  titulo: 'Erro inesperado',
  detalhe: 'Algo deu errado. Tente de novo.',
  campos: {},
};

export function paraApiError(erro: HttpErrorResponse): ApiError {
  // status 0: o navegador nem chegou a receber resposta (servidor fora do ar, dormindo ou rede caída).
  if (erro.status === 0) {
    return { status: 0, ...INDISPONIVEL };
  }
  const corpo = erro.error as ProblemDetail | null;
  if (!corpo || typeof corpo !== 'object' || (!corpo.title && !corpo.detail)) {
    return { status: erro.status, ...INESPERADO };
  }
  return {
    status: erro.status,
    titulo: corpo.title ?? INESPERADO.titulo,
    detalhe: corpo.detail ?? '',
    campos: corpo.errors ?? {},
  };
}
