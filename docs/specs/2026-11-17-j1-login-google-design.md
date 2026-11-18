# J1 2.5: login com Google (design)

- **Status:** decisões aprovadas em conversa pelo Arthur em 2026-11-17 (6 perguntas). Aguarda a revisão desta spec escrita.
- **Base:** `main` em `8f5e396` (o 2.4, PR #20, está mesclado). ADRs 0001 a 0026, banco na V12.
- **Relação com outras specs:** detalha o 2.5 da spec da fase 2 (`2026-11-04-j1-fase-2-design.md`, §3, §5 "Google", §8 e §9) e mantém o que ela aprovou. Reaproveita os tokens, a `familia` do refresh e a sessão no navegador do 2.2 (`2026-11-12-j1-login-design.md`, ADRs 0022 e 0023), sem mudança. **Acrescenta uma regra:** ao ligar uma conta por senha, a senha é apagada (§3).
- **Projeto OAuth:** criado pelo Arthur no Google Cloud em 2026-11-17. O cliente é do tipo Aplicativo da Web, com as origens `http://localhost:4200` e `http://localhost` e sem URI de redirecionamento, e o público está em Teste (só os usuários de teste dele). O Client ID é `181222736917-1k8vp73jptcmrbgceam718qoa55p0i39.apps.googleusercontent.com`. **Ele é público, e não um segredo:** o navegador o mostra a qualquer um. O client secret não é usado.

## 1. Objetivo

Hoje, para criar uma conta, a pessoa precisa inventar uma senha de 15 caracteres. Depois do 2.5, **quem tem conta no Google entra com dois cliques**, numa conta do próprio J1. A sessão continua a de sempre: o JWT de 15 min e o refresh rotacionado.

**Sucesso:**
- O botão "Continuar com o Google" funciona no ambiente local, em Entrar e em Cadastrar.
- Uma conta nova é criada, uma conta já ligada entra, e uma conta por senha com o mesmo e-mail verificado é ligada.
- Um token inválido é recusado: com a assinatura errada, de outro cliente (`aud`), de outro emissor (`iss`) ou vencido.
- Um e-mail não verificado é recusado.
- Quem vem de um convite volta para o convite.
- Tudo isso funciona a 375 px e a 1280 px.

**Escolhas do Arthur (2026-11-17):**
1. **A conta só do Google não define uma senha depois.** Ela entra só pelo Google. "Definir senha" e uma tela de perfil ficam fora do escopo.
2. **Um e-mail não verificado no Google é recusado** com 401 e a mensagem "Confirme o seu e-mail no Google antes de entrar.". Nenhuma conta é criada e nada é ligado.
3. **Ao ligar uma conta que tinha senha, a senha é apagada e todas as sessões dela são encerradas** (§3, *pre-account takeover*).
4. **O botão fica em Entrar e em Cadastrar.** O Convite continua como está: os links dele já levam a essas telas com `?volta=/convite/<código>`.
5. **Os testes do backend usam um JWKS falso em memória**, com uma chave RSA gerada no teste, sem rede e sem dependência nova.
6. **O Playwright não ganha um caso.** O botão é testado no Vitest com um `google` falso, e o login real é conferido no navegador local, uma vez, com o Arthur.

**Fora do escopo:**
- definir ou trocar a senha, a tela de perfil e desligar o Google;
- o One Tap, o pop-up automático do Google, que depende do FedCM e de regras do navegador que mudam;
- outros provedores (GitHub, Microsoft);
- a origem do Cloudflare Pages no cliente OAuth, o status Em produção e a CSP com os domínios do Google: tudo isso fica para o **2.6**.

## 2. Abordagens consideradas

| Abordagem | Por que sim ou por que não |
|---|---|
| **Botão do Google Identity Services → *ID token* → `POST /auth/google`, validado com o Nimbus (escolhida)** | É o desenho da spec da fase 2. A API continua stateless e emite os próprios tokens. O Nimbus já vem com o starter `oauth2-resource-server`, então não entra nenhuma dependência nova no backend. O GIS é um `<script>` do Google, e não um pacote npm, então também não entra nada no front e não é preciso parar o `ng serve`. |
| `oauth2-client` do Spring (fluxo *authorization code* no servidor) | Exige client secret, URI de redirecionamento e sessão com cookie. Isso choca com a API stateless e com o front e a API em domínios diferentes no 2.6 (ADR 0023). |
| `GoogleIdTokenVerifier` (biblioteca `google-api-client`) | Faz o mesmo que o Nimbus, que já está no projeto, e traz uma dependência a mais. |

## 3. Regras da conta (`POST /auth/google`)

A API valida o token (§4) e lê `sub`, `email`, `email_verified` e `name`. Depois decide, nesta ordem:

1. **`email_verified` não é `true`** → 401 "Confirme o seu e-mail no Google antes de entrar.". Nada muda no banco.
2. **Já existe um usuário com esse `google_sub`** → entra nessa conta, mesmo que o e-mail no Google tenha mudado. O `sub` é a identidade, e o e-mail não é. O e-mail gravado não é atualizado.
3. **Existe um usuário com esse e-mail** (normalizado como no 2.2: sem espaços e em minúsculas):
   - **Se ele já tem outro `google_sub`** → 401 "Este e-mail já está ligado a outra conta do Google.". É raro: acontece quando o Google entrega o mesmo e-mail a outra conta. *(Decisão minha.)*
   - **Senão, liga a conta, numa transação só:**
     - grava o `google_sub`;
     - **apaga a senha** (`senha_hash` passa a nulo);
     - **revoga todas as famílias de refresh abertas** do usuário;
     - o nome não muda.
     - **Por quê:** o cadastro por senha não confirma o e-mail, então qualquer pessoa pode criar uma conta com o e-mail de outra. Quem provou ao Google que é dona do e-mail vence, e quem criou a conta antes perde a senha e as sessões. É o ataque conhecido como *pre-account takeover*.
     - **Efeito na conta do Arthur:** se ela foi criada com o Gmail dele, ela passa a entrar só pelo Google depois do primeiro login com o Google. Isso foi aceito na escolha 3.
4. **Não existe nenhum dos dois** → cria a conta:
   - `email` = o do token, normalizado;
   - `nome` = `name`, sem espaços nas pontas e cortado em 100 caracteres. Sem `name`, usa a parte do e-mail antes do @. *(Decisão minha.)*
   - `senha_hash` nulo e `google_sub` = `sub`.
   - Se duas criações simultâneas baterem na restrição única, a API busca de novo pelo `sub` e entra nessa conta. *(Decisão minha.)*
5. **A resposta é sempre 200**, com o mesmo corpo do login (`{accessToken, refreshToken, usuario}`) e uma família nova. Uma rota só, que entra e também cria, não distingue 200 de 201. *(Decisão minha.)*

**Efeitos nas rotas do 2.2:**
- o `POST /auth/login` de uma conta só do Google dá o mesmo 401 "E-mail ou senha incorretos". O código já trata o `senha_hash` nulo assim (como no `legado`);
- o `POST /auth/registro` com o e-mail de uma conta do Google dá o 409 "E-mail já cadastrado" de sempre.

**Logs:** só o id do usuário, como no `ContaService`: "Login Google id=", "Conta criada pelo Google id=" e "Conta ligada ao Google id=". Nunca o token, o e-mail ou o nome.

## 4. API e validação do token

| Rota | Corpo | Sucesso | Erros |
|---|---|---|---|
| `POST /auth/google` | `{idToken}` (obrigatório, até 4096 caracteres; o JSON é estrito, como no resto da API) | 200 + `{accessToken, refreshToken, usuario}` | 400 sem o campo; 401 "Não foi possível entrar com o Google. Tente de novo." para qualquer token inválido; os dois 401 da §3 |

A rota fica aberta, como as outras de `/auth/**` (`SecurityConfig` sem mudança).

**`VerificadorDoGoogle`** (pacote `auth`):
- recebe a string do token e devolve as claims, ou lança a exceção do 401;
- tem dentro dele um `NimbusJwtDecoder` próprio:
  - o algoritmo é **RS256**;
  - as chaves vêm de um bean `JWKSource` (`chavesDoGoogle`). Na produção, ele vem da URL `https://www.googleapis.com/oauth2/v3/certs`, com cache e com a troca automática de chave pelo `kid` do Nimbus (`JWKSourceBuilder`);
  - **os validadores** são:
    - o `JwtTimestampValidator` com o `Clock` da aplicação, como no `JwtConfig`;
    - o `iss` deve ser `https://accounts.google.com` ou `accounts.google.com`, as duas formas que o Google usa;
    - o `aud` deve conter o client ID.
- **Esse decoder não vira um bean `JwtDecoder`.** Se virasse, o *resource server* passaria a ter dois e poderia usar o errado para os nossos tokens HS256. Ele fica privado dentro do verificador.
- Um token HS256, ou um token sem assinatura (`alg: none`), é recusado pelo próprio seletor de chaves RS256.

**Configuração:**
- `app.google.client-id=${GOOGLE_CLIENT_ID:181222736917-…apps.googleusercontent.com}` no `application.properties`. Ele é público e pode ficar no repositório; o 2.6 só o sobrescreve se mudar;
- `app.google.jwks-url`, com a URL acima como padrão.

**Swagger:** a rota nova entra na tag "Autenticação", com `@SecurityRequirements` vazio, como as outras.

## 5. Dados: migração V13 (`V13__google_sub.sql`)

```sql
alter table usuario add column google_sub varchar(255);
alter table usuario add constraint uk_usuario_google_sub unique (google_sub);
```

- O `sub` do Google tem até 255 caracteres ASCII, segundo a documentação do Google.
- Nada de CHECK que exija senha ou `google_sub`: o `legado` não tem nenhum dos dois, de propósito.
- `Usuario` ganha `googleSub`, mais um método `ligarAoGoogle(sub)` que grava o `sub` e anula a senha.
- `RefreshTokenRepository` ganha `revogarTodasDoUsuario(usuarioId, quando)`, um `UPDATE` só.
- **Antes de aplicar a V13 no banco local:** o `pg_dump` vai para `antes-da-v13.sql`, numa pasta temporária, fora do repositório. As 5 fichas do Arthur (2, 7, 8, 9 e 13) não mudam.

## 6. Frontend

**Configuração:** `environment.ts` e `environment.development.ts` ganham `googleClientId`, o mesmo Client ID público.

**`core/auth/google-identity.ts`** (serviço):
- carrega `https://accounts.google.com/gsi/client` **sob demanda**: injeta o `<script>` uma vez e guarda a promessa. Assim, as páginas que não são de login nem baixam o script do Google;
- declara à mão os tipos mínimos de `google.accounts.id` (`initialize` e `renderButton`), sem `@types/google.accounts`, para não instalar nada com o `ng serve` no ar.

**`auth/botao-google.ts`** (componente, input `volta`):
- chama `initialize({ client_id, callback })` e depois `renderButton(elemento, { theme: 'filled_black', text: 'continue_with', shape: 'pill', locale: 'pt-BR', width })`. A largura é a do cartão, entre 200 e 400 px, o limite do Google;
- usa o **botão oficial do Google**, e não um botão nosso. As regras de marca do Google exigem o visual dele. O tema escuro é o que combina com o grimório. *(Decisão minha.)*
- no callback, pega o `credential` e chama `AuthApi.entrarComGoogle(idToken)`. Depois faz `Sessao.iniciar(resposta)` e navega para `voltaSegura(volta)`, o mesmo caminho do Entrar;
- **erros** aparecem numa mensagem com `role="alert"` debaixo do botão: a mensagem da API (401), ou "Não foi possível falar com o servidor." numa falha de rede;
- se o script do Google não carregar (rede bloqueada, extensão), aparece um aviso discreto: "O login com o Google não está disponível agora. Use o e-mail e a senha." O formulário continua funcionando.

**Telas:**
- **Entrar e Cadastrar:** o `<app-botao-google [volta]="volta()">` fica no topo do cartão, seguido de um separador "ou" e do formulário de sempre;
- **Convite:** nenhuma mudança.

## 7. Testes

**Backend** (Testcontainers, como hoje):
- **Os testes atuais não mudam de asserção.** A única exceção prevista é o `EsquemaBancoTest`, que passa a esperar a coluna nova; ela é listada no PR.
- **`ChavesDoGoogleDeTeste`** (`@TestConfiguration`):
  - gera um par RSA com `kid`;
  - expõe um `JWKSource` em memória (`ImmutableJWKSet`) que substitui o `chavesDoGoogle`;
  - tem um método que assina tokens com as claims pedidas.
- **`GoogleLoginTest`:**
  - **conta nova:** 200; o e-mail em minúsculas; o nome vindo do `name`, com o corte de 100 caracteres e a falta do `name`; o `senha_hash` nulo; o `google_sub` gravado; o refresh funciona;
  - **entra de novo pelo `sub`:** o mesmo id, mesmo com outro e-mail no token;
  - **liga uma conta por senha:**
    - o `google_sub` é gravado e o `senha_hash` fica nulo;
    - o refresh antigo dessa conta dá 401;
    - o login por senha dá 401;
    - a família de outro usuário continua viva;
  - **e-mail ligado a outro `sub`:** 401, sem nenhuma mudança;
  - **`email_verified` falso ou ausente:** 401 com a mensagem, e nenhuma conta é criada;
  - **token inválido** (401 e nada muda):
    - assinado por outra chave RSA;
    - `kid` desconhecido;
    - `aud` de outro cliente;
    - `iss` de outro emissor;
    - vencido, com o `Clock` avançado;
    - um token HS256 nosso;
    - um texto qualquer;
  - **400** sem `idToken` e com uma chave desconhecida no JSON;
  - **registro** com o e-mail de uma conta do Google: 409.

**Front** (Vitest):
- **`google-identity.spec`:**
  - o script é injetado uma vez só;
  - uma falha no carregamento rejeita a promessa.
- **`botao-google.spec`**, com um `google` falso e o `HttpTestingController`:
  - o `renderButton` recebe o elemento e o client ID;
  - o `credential` vira `POST /auth/google` com `{idToken}`;
  - o sucesso inicia a sessão e navega para o `volta` (e para `/fichas` com um `volta` externo);
  - um 401 aparece em `role="alert"`;
  - uma falha do script mostra o aviso.
- **`entrar.spec` e `cadastrar.spec`:** o botão aparece com o `volta` repassado.

**Playwright:** sem mudança (escolha 6).

**Conferência no navegador** (com o Arthur):
- o Arthur faz o login real com a conta de teste dele. **Quem digita a senha do Google é o próprio Arthur**;
- conta nova ou ligada, recarregar a página e continuar logado, sair, voltar pelo convite e 375 px;
- se o navegador de teste bloquear o pop-up do Google, a conferência vai para o Chrome.

## 8. Documentação

- **ADR 0027:** login com Google pelo *ID token* e ligação de contas por e-mail verificado, com a senha apagada na ligação (*pre-account takeover*). As alternativas são as da §2, mais "ligar mantendo a senha" e "não ligar sozinho".
- **README:**
  - a linha do 2.4 passa de "Em revisão (PR)" para **"Concluído (PR #20)"** (pendência do 2.4);
  - entra a linha do 2.5;
  - entra a linha do `POST /auth/google` na tabela de autenticação, mais as regras da §3 em duas frases;
  - o "como rodar" ganha uma nota: o Client ID já vem configurado, o público do Google está em Teste, e só os usuários de teste entram;
  - entram as linhas dos testes novos.
- **Roteiro de entrevista:** uma pergunta, "Como você validou o login com Google e por que apagou a senha na ligação?".
- **`estado.md`:** atualizado ao fim. Fica anotado, para o 2.6, que a CSP precisa liberar `accounts.google.com` (script, frame e connect).

## 9. Processo

É o processo enxuto:
1. **Preparação:** a branch `feat/login-google`, a partir da `main` (`8f5e396`), e o backup do banco antes da V13.
2. **Desenvolvimento:** **backend e front em paralelo**. O contrato da §4 é fixo.
3. **Verificação:** **uma revisão independente no fim**.
4. **Conferência no navegador** (§7).
5. **PR** com o CI verde. O merge só acontece quando o Arthur pedir.

**Regras de cada tarefa:**
- nada de `npm ci` ou `npm install` com o `ng serve` no ar, e nenhuma dependência nova no front;
- com a API no ar, o Maven só roda numa cópia da pasta, numa pasta temporária (por causa do devtools);
- o backup é feito antes de começar o backend.

## 10. Riscos

| Risco | Mitigação |
|---|---|
| Um *ID token* interceptado é reapresentado à API dentro da validade (cerca de 1 h) | O HTTPS no 2.6 e a validade curta. Um `nonce` exigiria guardar estado no servidor ou num cookie: fica fora do escopo e é registrado no ADR |
| O Google troca as chaves | O `JWKSourceBuilder` rebaixa o JWKS quando aparece um `kid` desconhecido |
| O JWKS do Google fica fora do ar | O login pelo Google falha com 401, e o login por senha continua funcionando. As chaves ficam em cache |
| O público em Teste barra quem não é usuário de teste | É o esperado até o 2.6 (status Em produção). O README avisa |
| O pop-up do Google é bloqueado no navegador de teste | A conferência real vai para o Chrome |
| A V13 é aplicada sem querer pelo devtools | O backup antes, e o Maven só numa cópia da pasta com a API no ar |
