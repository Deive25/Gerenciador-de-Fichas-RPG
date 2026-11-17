import { defineConfig, devices } from '@playwright/test';

// O Node já fornece o process; o projeto não instala @types/node, então só declaramos o que usamos.
declare const process: { env: Record<string, string | undefined> };

const emCI = !!process.env['CI'];

// Teste de ponta a ponta: sobe a API (o jar já empacotado) e o ng serve, e abre o Chromium.
// Local: reaproveita a API e o ng serve que já estiverem no ar. No CI: sempre sobe os dois.
export default defineConfig({
  testDir: 'e2e',
  // O fluxo cadastra duas contas e passa por várias telas: o padrão de 30 s ficaria apertado no CI.
  timeout: 90_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  forbidOnly: emCI,
  retries: 0,
  reporter: emCI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:4200',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      // O JWT_SEGREDO e as variáveis de banco (DB_URL, DB_USERNAME, DB_PASSWORD) vêm do ambiente do job.
      command: 'java -jar "../Gerenciador de fichas RPG/target/fichas-rpg-0.0.1-SNAPSHOT.jar"',
      url: 'http://localhost:8080/campanhas',
      // Sem token a API responde 401: basta para saber que ela subiu.
      ignoreHTTPSErrors: true,
      reuseExistingServer: !emCI,
      timeout: 120_000,
      gracefulShutdown: { signal: 'SIGTERM', timeout: 10_000 },
    },
    {
      command: 'npx ng serve --port 4200',
      url: 'http://localhost:4200',
      reuseExistingServer: !emCI,
      timeout: 180_000,
      env: { NG_CLI_ANALYTICS: 'false' },
    },
  ],
});
