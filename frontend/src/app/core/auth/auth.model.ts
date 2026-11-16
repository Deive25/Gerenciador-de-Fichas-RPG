/** Quem está logado (resposta de /auth/* e de /me). */
export interface Usuario {
  id: number;
  nome: string;
  email: string;
}

/** Resposta de registro, login e refresh: o par de tokens mais o usuário. */
export interface RespostaAuth {
  accessToken: string;
  refreshToken: string;
  usuario: Usuario;
}

/**
 * Resultado de uma tentativa de renovar a sessão:
 * - renovada: há um token de acesso novo;
 * - sem-sessao: não há refresh, ou a API o recusou (401); a sessão foi apagada;
 * - indisponivel: a API não respondeu (rede ou 5xx); o refresh foi mantido.
 */
export type ResultadoRenovacao = 'renovada' | 'sem-sessao' | 'indisponivel';
