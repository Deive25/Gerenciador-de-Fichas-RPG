// Configuração usada em desenvolvimento (ng serve / npm start): API local.
export const environment = {
  apiUrl: 'http://localhost:8080',
  // Client ID do Google (público, aparece no navegador de qualquer um; não é segredo).
  googleClientId: '181222736917-1k8vp73jptcmrbgceam718qoa55p0i39.apps.googleusercontent.com',
  // Mostra "Ver demonstração". Sem o perfil `demo` na API, ela responde 404 e a tela mostra o erro.
  demoAtiva: true,
};
