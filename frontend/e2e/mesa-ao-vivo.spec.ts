import { Page, expect, test } from '@playwright/test';

// Senha de 15+ caracteres que passa a política do 2.2 (sem sequência, repetição, senha comum, nome ou e-mail).
const SENHA = 'cavalo bateria grampo lunar';

/** Cada execução usa e-mails novos, no formato que se apaga do banco local depois (e2e-...@teste.local). */
const execucao = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;

/** Preenche a tela "Criar conta" já aberta. O cadastro entra na conta e leva para a rota de volta. */
async function cadastrar(pagina: Page, nome: string, email: string): Promise<void> {
  await pagina.getByLabel('Nome', { exact: true }).fill(nome);
  await pagina.getByLabel('E-mail').fill(email);
  await pagina.getByLabel('Senha', { exact: true }).fill(SENHA);
  await pagina.getByRole('button', { name: 'Criar conta' }).click();
}

test('a rolagem da jogadora aparece ao vivo no painel do mestre', async ({ browser }) => {
  // Dois contextos = duas sessões isoladas (cada uma com o próprio localStorage), como duas pessoas.
  const mestre = await (await browser.newContext()).newPage();
  const jogadora = await (await browser.newContext()).newPage();
  const personagem = `Lira ${execucao}`;

  // A (mestre): cadastra a conta, cria a campanha e pega o link do convite.
  await mestre.goto('/cadastrar');
  await cadastrar(mestre, 'Mestre E2E', `e2e-mestre-${execucao}@teste.local`);
  await mestre.getByRole('link', { name: 'Campanhas', exact: true }).click();
  await mestre.getByRole('button', { name: '+ Nova campanha' }).click();
  await mestre.getByLabel('Nome da campanha').fill(`Mesa ${execucao}`);
  await mestre.getByRole('button', { name: 'Criar campanha' }).click();
  // Criar leva direto para a tela da campanha nova.
  await expect(mestre.getByRole('heading', { name: `Mesa ${execucao}` })).toBeVisible();
  const link = await mestre.getByLabel('Link do convite').inputValue();
  expect(link).toContain('/convite/');

  // A abre o painel e o deixa aberto: a rolagem de B tem de chegar sem recarregar.
  await mestre.getByRole('button', { name: 'Rolagens' }).click();
  const painel = mestre.getByRole('dialog', { name: 'Rolagens' });
  await expect(painel).toBeVisible();
  await expect(painel.getByText('ao vivo')).toBeVisible();

  // B (jogadora): abre o link, cadastra a conta pelo caminho do convite e entra na campanha.
  await jogadora.goto(link);
  await jogadora.getByRole('link', { name: 'Criar conta' }).click();
  await cadastrar(jogadora, 'Jogadora E2E', `e2e-jogadora-${execucao}@teste.local`);
  await jogadora.getByRole('button', { name: 'Entrar na campanha' }).click();
  // Aceitar o convite leva direto para a campanha.
  await expect(jogadora.getByRole('heading', { name: `Mesa ${execucao}` })).toBeVisible();

  // B cria uma ficha de D&D dentro da campanha (só os campos obrigatórios).
  await jogadora.getByRole('link', { name: '+ Nova ficha aqui' }).click();
  await jogadora.getByRole('link', { name: /D&D 2024/ }).click();
  await jogadora.getByLabel('Jogador(a)').fill('Jogadora E2E');
  await jogadora.getByLabel('Personagem').fill(personagem);
  await jogadora.getByLabel('Gênero').fill('Feminino');
  await jogadora.getByRole('button', { name: 'Salvar ficha' }).click();
  await expect(jogadora.getByRole('heading', { name: personagem })).toBeVisible();

  // B rola uma perícia e pula o dado 3D com Esc (sem WebGL, a camada cai direto no resultado).
  await jogadora.getByRole('button', { name: /^Rolar Furtividade/ }).click();
  await jogadora.keyboard.press('Escape');
  await expect(jogadora.getByRole('status').filter({ hasText: 'Furtividade' }).first()).toBeVisible();

  // A vê a rolagem de B no painel: personagem e rótulo.
  await expect(painel.getByText(personagem)).toBeVisible();
  await expect(painel.getByText('Furtividade')).toBeVisible();
});
