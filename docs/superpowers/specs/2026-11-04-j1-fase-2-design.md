# J1 fase 2: Fichas RPG para a mesa (design)

- **Status:** aprovado em conversa, parte por parte, pelo Arthur em 2026-11-04. Aguarda a revisão desta spec escrita.
- **Base:** `main` em `b4a6cb7`. Fim da fase 1: Java 21, Spring Boot 4.1.2, PostgreSQL 17 + Flyway, 74 testes, ADRs 0001–0012.

## 1. Objetivo

Transformar o Gerenciador de Fichas numa ferramenta que o grupo de RPG do Arthur use de verdade durante as sessões, com acabamento de portfólio e um link público para recrutadores.

**Sucesso:**
- o grupo joga uma sessão usando só o celular (fichas e rolagens ao vivo);
- um recrutador abre o link, entra com uma conta de demonstração e vê a mesa funcionando em menos de 2 minutos, contando o tempo de o servidor gratuito acordar.

**Escolhas do Arthur:** os dois objetivos (uso real e vitrine); Angular atual com redesign; visual "investigação sombria"; layout pensado primeiro para o celular; login por e-mail/senha e por Google; papéis Mestre/Jogador; campanhas com link de convite; rolagens em tempo real; deploy só gratuito, como último incremento.

**Fora do escopo desta fase:**
- rolagens secretas do mestre;
- atualização ao vivo da ficha (só as rolagens são ao vivo);
- crítico e desastre (1 e 100 especiais);
- chat;
- ~~upload de imagem~~ (entrou no 2.1c; ver `2026-11-04-j1-redesign-design.md`);
- exportar PDF;
- várias instâncias da API;
- domínio próprio;
- e-mail transacional (convite e recuperação de senha).

## 2. Arquitetura

- **Frontend:** SPA Angular estática, servida pelo Cloudflare Pages (CDN, não dorme).
- **API:** Spring Boot num container Docker, no Render gratuito (dorme após 15 min sem uso e acorda em ~1 min).
- **Banco:** PostgreSQL no Neon gratuito (1 GB, sem expiração).
- **Comunicação:** REST (JSON) para tudo; WebSocket (STOMP) só para receber rolagens ao vivo.
- **Organização da API:** um único deployable, com pacotes por domínio: `auth`, `campanha`, `ficha`, `rolagem`, `comum`.
- **Fontes:** pesquisa de 2026-11-04 com páginas oficiais. Evitados: Postgres gratuito do Render (apagado após 30 dias) e Fly.io (sem plano gratuito para contas novas). Pontos não confirmados (por exemplo, se algum serviço exige cartão) são reavaliados no 2.6.

## 3. Modelo de dados (migrações Flyway, sem perda de dados)

| Tabela | Colunas principais | Incremento |
|---|---|---|
| `usuario` (nova) | `id`, `email` (único), `nome`, `senha_hash` (nulo se só Google), `google_sub` (único, nulo), `criado_em` | 2.2 (Google em 2.5) |
| `refresh_token` (nova) | `id`, `usuario_id`, `hash`, `expira_em`, `revogado_em`, `substituido_por` | 2.2 |
| `ficha` (hoje `sheet_player`) | + `dono_id` (2.2), + `campanha_id` (2.3); tabela e colunas renomeadas para nomes claros (2.3) | 2.2 / 2.3 |
| `ficha_pericia` | sem mudança | — |
| `campanha` (nova) | `id`, `nome`, `descricao`, `codigo_convite` (único, aleatório, ≥ 10 caracteres), `convite_ativo`, `criado_em` | 2.3 |
| `participacao` (nova) | PK (`campanha_id`, `usuario_id`), `papel` (`MESTRE` \| `JOGADOR`), `entrou_em` | 2.3 |
| `rolagem` (nova) | `id`, `campanha_id`, `ficha_id`, `usuario_id`, `pericia`, `valor`, `rolagem`, `resultado`, `criada_em`; índice (`campanha_id`, `criada_em desc`) | 2.4 |

**Dados antigos:**
- a migração do 2.2 cria o usuário de sistema `legado`, que não consegue entrar, e passa a ser dono das fichas existentes;
- a migração do 2.3 cria a campanha "Fichas importadas", com o `legado` como mestre, e a preenche com essas fichas;
- a coluna `jogador` (texto) continua na ficha.
- **Efeito prático:** as fichas antigas ficam preservadas no banco, mas nenhum usuário real as vê, porque o `legado` não consegue entrar. Isso é aceitável porque não há produção: só existem dados de desenvolvimento local. Não haverá uma função para "reivindicar" fichas (YAGNI).

**Demonstração:** os usuários Mestre Demo e Jogadora Demo, com uma campanha pronta, são criados só quando o perfil `demo` está ativo, e nunca por migração.

## 4. Permissões

| Ação | Mestre da campanha | Jogador da campanha | Fora da campanha |
|---|---|---|---|
| Ver a ficha completa | todas | só as próprias | — |
| Ver o resumo (personagem, vida, sanidade) | todas | todas da mesa | — |
| Criar ficha | sim (dono = mestre) | sim (dono = ele) | — |
| Editar ou excluir ficha | todas | só as próprias | — |
| Rolar teste de perícia | com qualquer ficha da mesa | com as próprias | — |
| Ver o histórico e as rolagens ao vivo | sim | sim | — |
| Renomear ou excluir a campanha, gerenciar o convite, remover jogador | sim | não (só sair) | entrar pelo convite |

**Erros:**
- sem token, ou com token inválido → **401**;
- não membro, para qualquer coisa da campanha → **404** (a campanha "não existe" para ele);
- membro sem permissão para a ação → **403**.

Tudo em ProblemDetail, como hoje.

## 5. Autenticação

**E-mail e senha (2.2):**
- senha com no mínimo 8 caracteres, guardada como hash BCrypt;
- o login devolve um **access token** (JWT assinado pela API, 15 min, com `sub` e `email`) e um **refresh token** (opaco e aleatório, 30 dias, guardado só como hash, **rotacionado** a cada `/auth/refresh`);
- reapresentar um refresh já usado revoga a cadeia inteira (detecção de roubo);
- o logout revoga o refresh token;
- o JWT é validado pelo resource server do Spring Security.

**Onde o navegador guarda os tokens:**
- o front (pages.dev) e a API (onrender.com) ficam em sites diferentes, então um cookie *httpOnly* seria cookie de terceiro e o Safari o bloqueia;
- por isso o access token fica só em memória e o refresh token fica em `localStorage`;
- mitigações: CSP rígida no Pages, nenhum `innerHTML` dinâmico e rotação dos tokens;
- registrado num ADR, com a alternativa: domínio próprio (pago) com cookie *httpOnly* e `SameSite`.

**Google (2.5):**
- o front usa o Google Identity Services e envia o *ID token* para `POST /auth/google`;
- a API valida a assinatura (JWKS do Google), o `aud` (client ID) e o `iss`, e devolve os próprios tokens;
- se o `google_sub` já existe, entra nessa conta;
- se o e-mail verificado já tem conta por senha, liga as duas;
- senão, cria uma conta nova.

**WebSocket:** o access token vai no frame CONNECT do STOMP e é validado num `ChannelInterceptor`. O SUBSCRIBE em `/topic/campanhas/{id}/rolagens` só é aceito se o usuário for membro da campanha.

## 6. API

**Sem mudança:** as regras atuais (JSON estrito, PUT como substituição completa, ProblemDetail, códigos HTTP).

| Área | Rotas |
|---|---|
| Auth | `POST /auth/registro`, `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/google`; `GET /me` |
| Campanhas | `GET/POST /campanhas`; `GET/PATCH/DELETE /campanhas/{id}`; `GET /campanhas/{id}/membros`; `DELETE /campanhas/{id}/membros/{usuarioId}` (o jogador pode remover a si mesmo) |
| Convite | `POST /campanhas/{id}/convite` (gera ou renova, invalidando o código anterior); `DELETE /campanhas/{id}/convite` (desativa); `GET /convites/{codigo}` (prévia: nome da campanha e do mestre); `POST /convites/{codigo}/aceitar` |
| Fichas | `GET/POST /campanhas/{id}/fichas`: a lista traz sempre o **resumo** (id, personagem, jogador, dono, vida e sanidade atuais e máximas), e o detalhe completo é pedido por `GET /fichas/{id}`, conforme a permissão. `GET/PUT/DELETE /fichas/{id}` usam o JSON do 1.6. No 2.2, antes das campanhas, `GET/POST /fichas` continuam existindo, só com as fichas do próprio usuário (dono = quem está logado). |
| Rolagens | `POST /fichas/{id}/testes-de-pericia` (passa a gravar e publicar); `GET /campanhas/{id}/rolagens?antes=<id>&limite=50` (paginação por cursor) |
| Tempo real | WebSocket `/ws` (STOMP); tópico `/topic/campanhas/{id}/rolagens` |

**Mudanças de contrato conscientes:**
- `GET /fichas` e `POST /fichas` dão lugar a `/campanhas/{id}/fichas` no 2.3;
- todas as rotas, exceto auth, convite (prévia), documentação e health, passam a exigir token no 2.2.

O Angular muda junto em cada incremento.

**Publicação em tempo real:**
- o serviço grava a rolagem e publica um evento de domínio;
- um `@TransactionalEventListener(AFTER_COMMIT)` envia o evento ao tópico com o `SimpMessagingTemplate`, então nada é anunciado se a gravação falhar;
- broker STOMP simples, em memória: há uma instância só;
- várias instâncias exigiriam um broker externo (RabbitMQ/Redis), o que fica registrado no ADR.

## 7. Frontend

**Base:**
- novo projeto em `frontend/`, na versão estável atual do Angular (confirmar no npm ao iniciar o 2.1);
- componentes standalone, *signals*, `@if/@for`, rotas com carregamento sob demanda;
- o frontend Angular 13 sai no 2.1, quando o novo cobrir tudo o que ele faz.

**Visual "investigação sombria":**
- fundo grafite (`#12151c`), cards com borda sutil, títulos com serifa e texto em sans-serif;
- dourado (`#c9a45c`) para ações, vermelho (`#d0533f`) para vida, verde (`#4fb38c`) para sanidade;
- tudo definido como variáveis CSS;
- Angular Material como base acessível (formulários, diálogos, menus), com um tema próprio;
- os textos de ajuda de atributos e perícias da versão antiga viram dicas (tooltips).

**Layout, pensado primeiro para o celular:**

> **Substituído** pela spec do redesign (`2026-11-04-j1-redesign-design.md`, §3): agora há um painel de fichas e uma tela própria para cada ficha, sem colunas fixas nem abas embaixo. As rolagens ao vivo do 2.4 abrem num painel lateral, aberto por um botão. O texto original fica abaixo como histórico.

- no celular, barra no topo e abas embaixo: **Mesa · Minha ficha · Rolagens**;
- no computador, as mesmas telas em 2 colunas: fichas à esquerda e rolagens ao vivo à direita.

**Telas:**
- Entrar e cadastrar;
- Convite (`/convite/:codigo`; quem não está logado vê a prévia e entra depois de fazer login);
- Minhas campanhas;
- Campanha (as abas acima);
- Ficha (ver, editar e criar, com 🎲 em cada perícia e o resultado mostrando as faixas).

**Infraestrutura:**
- interceptor HTTP que põe o token e, quando recebe 401, renova uma única vez e repete a chamada;
- `@stomp/rx-stomp` com reconexão;
- enquanto a API acorda, a tela de login mostra "acordando o servidor…" e tenta de novo, com espera crescente.

## 8. Testes

**Backend** (o padrão atual continua: integração contra PostgreSQL real com Testcontainers):
- testes de migração com dados antigos (V3 em diante);
- um teste por regra da matriz de permissões (401/403/404);
- testes de autenticação: senha errada, token expirado, refresh reutilizado revoga a cadeia, *ID token* do Google inválido é recusado (com chaves de teste);
- teste de integração de WebSocket: um cliente STOMP inscrito recebe a rolagem que outro usuário fez por REST, e um não membro é recusado na inscrição;
- testes unitários das regras novas: convite e papéis.

**Frontend:**
- testes unitários com o executor padrão do Angular atual;
- Playwright no CI, com poucos testes de ponta a ponta: cadastro/login → criar campanha → entrar por convite em outra sessão → rolar → a rolagem aparece na outra sessão.

**Regras do processo** (as mesmas da fase 1):
- nenhum teste existente é removido;
- mudanças de asserção só acontecem quando o contrato muda de propósito, e são listadas no PR;
- `./mvnw -B clean verify` e o build e os testes do front ficam verdes no CI.

## 9. Incrementos

Cada incremento passa por: dev Sonnet → verificador Sonnet separado → documentação (README, ADR, `estado.md`) → PR com o CI verde, mesclado como *merge commit*. A autorização do Arthur vale para a fase 2.

| Inc. | Entrega | Pronto quando |
|---|---|---|
| **2.1** | Frontend novo (Angular atual, visual B, celular primeiro) cobrindo o que existe hoje: lista, ver, criar, editar e excluir fichas, e teste de perícia com 🎲. API sem mudança. CI do front no Node atual; o `frontend/` antigo sai. | O fluxo funciona no navegador em largura de celular e de computador; testes unitários verdes; `ng build` no CI. |
| **2.2** | Login por e-mail/senha: usuários, JWT, refresh rotacionado, logout, Spring Security, dono da ficha (o legado fica com as antigas), telas de entrar e cadastrar, interceptor. Cada usuário vê, edita e rola só as próprias fichas; o compartilhamento chega no 2.3. | 401/403 testados; senha só como hash; migração testada com dados antigos; o front renova o token sozinho. |
| **2.3** | Campanhas, convites e papéis: fichas por campanha, renomeação para `ficha` com nomes claros, campanha "Fichas importadas", perfil `demo`, telas Minhas campanhas, Campanha e Convite. | A matriz de permissões da §4 inteira está testada; renovar o convite invalida o código antigo; migração testada. |
| **2.4** | Rolagens persistidas e ao vivo: tabela `rolagem`, histórico paginado, STOMP autenticado, aba Rolagens e coluna ao vivo no computador. | Teste de WebSocket entre dois clientes; teste de ponta a ponta com duas sessões no navegador. |
| **2.5** | Login com Google (depende do Arthur criar o projeto OAuth gratuito no Google Cloud). | Ligação por e-mail verificado testada; token inválido é recusado; o botão funciona no ambiente local. |
| **2.6** | Deploy gratuito: Dockerfile, Render, Neon, Cloudflare Pages, CORS, CSP, perfil `demo` em produção, publicação automática a cada merge, link no README (depende do Arthur criar as contas). | O link público funciona do login à rolagem ao vivo; nenhum segredo no repositório; aviso de "acordando o servidor". |

**Entre o 2.1 e o 2.2** entram o **2.1b** (novo visual), o **2.1c** (retrato) e o **2.1d** (dado 3D). Eles estão descritos em `2026-11-04-j1-redesign-design.md`.

**Novos ADRs previstos:**
- Angular novo em vez de upgrade;
- armazenamento de tokens;
- campanhas e permissões (404 versus 403);
- WebSocket STOMP e publicação após o commit;
- login com Google (ligação de contas);
- hospedagem gratuita.

## 10. Riscos

| Risco | Mitigação |
|---|---|
| Planos gratuitos mudam ou passam a exigir cartão | Reavaliar no 2.6 com uma nova pesquisa; qualquer custo passa pelo Arthur |
| API dorme (~1 min para acordar) | A tela abre pela CDN e mostra o aviso; as contas de demo ficam prontas |
| Refresh token em `localStorage` (risco de XSS) | CSP rígida, sem HTML dinâmico, rotação e revogação; ADR com a alternativa paga |
| Escopo grande adia o C1 (.NET) | Cada incremento é publicável; o Arthur pode parar depois de qualquer um |
| Versão do Angular muda durante a fase | Fixar a versão no 2.1 e só atualizar com decisão |
