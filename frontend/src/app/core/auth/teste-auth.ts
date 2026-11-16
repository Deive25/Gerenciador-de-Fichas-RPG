import { RespostaAuth } from './auth.model';

/** Resposta de login de mentira, para os testes do módulo de autenticação. */
export function respostaDe(sufixo = '1'): RespostaAuth {
  return {
    accessToken: `acesso-${sufixo}`,
    refreshToken: `refresh-${sufixo}`,
    usuario: { id: 7, nome: 'Ana', email: 'ana@x.com' },
  };
}
