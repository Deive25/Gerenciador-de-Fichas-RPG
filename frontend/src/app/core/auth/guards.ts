import { inject } from '@angular/core';
import { CanActivateChildFn, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { Renovacao } from './renovacao';
import { Sessao } from './sessao';

/** Aceita só caminho interno ("/x", nunca "//host"): evita redirecionamento aberto pelo ?volta=. */
export function voltaSegura(volta: string | null | undefined): string {
  return typeof volta === 'string' && volta.startsWith('/') && !volta.startsWith('//') ? volta : '/fichas';
}

/**
 * Protege as rotas de fichas. Sem token de acesso na memória (recarregou a página), tenta renovar
 * com o refresh guardado antes de abrir: é isso que mantém a pessoa logada e na mesma tela.
 * Com a API indisponível deixa passar; a tela mostra "Servidor indisponível" no lugar de ir ao login.
 */
export const exigeSessao: CanActivateChildFn = async (_rota, estado: RouterStateSnapshot) => {
  if (inject(Sessao).estaLogado()) {
    return true;
  }
  const router = inject(Router);
  const resultado = await inject(Renovacao).renovar();
  return resultado === 'sem-sessao' ? router.createUrlTree(['/entrar'], { queryParams: { volta: estado.url } }) : true;
};

/** Abre /entrar e /cadastrar só para quem não está logado; quem está vai para /fichas. */
export const soConvidado: CanActivateFn = async () => {
  const router = inject(Router);
  if (inject(Sessao).estaLogado()) {
    return router.createUrlTree(['/fichas']);
  }
  const resultado = await inject(Renovacao).renovar();
  return resultado === 'renovada' ? router.createUrlTree(['/fichas']) : true;
};
