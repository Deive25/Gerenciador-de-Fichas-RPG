import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { EstadoDoServidor } from './core/servidor/estado-do-servidor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // withComponentInputBinding: parâmetros da rota (ex.: :id) chegam como input() do componente.
    provideRouter(routes, withComponentInputBinding()),
    // O interceptor põe o token nas chamadas à API e renova a sessão num 401.
    provideHttpClient(withInterceptors([authInterceptor])),
    // Ao abrir o app, confere se a API (gratuita, que dorme) já acordou; não bloqueia a inicialização.
    provideAppInitializer(() => inject(EstadoDoServidor).iniciar()),
  ],
};
