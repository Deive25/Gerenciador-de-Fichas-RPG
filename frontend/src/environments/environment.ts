// Configuração usada no build de produção (ng build): API pública no Render (nome provisório até o deploy).
export const environment = {
  apiUrl: 'https://fichas-rpg-api.onrender.com',
  // Client ID do Google (público, aparece no navegador de qualquer um; não é segredo).
  googleClientId: '181222736917-1k8vp73jptcmrbgceam718qoa55p0i39.apps.googleusercontent.com',
  // Mostra "Ver demonstração" (a API só aceita com o perfil `demo`).
  demoAtiva: true,
};
