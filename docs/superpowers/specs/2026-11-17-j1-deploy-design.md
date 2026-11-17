# J1 2.6: deploy gratuito (design)

- **Status:** aprovado pelo Arthur em 2026-11-17 (conversa e spec escrita, com o plano).
- **Base:** `main` em `7174b43` (2.5 mesclado, banco na V13). Parte da spec da fase 2 (`2026-11-04-j1-fase-2-design.md`, §2, §5, §9 e §10).

## 1. Objetivo
Pôr o J1 num link público e gratuito, com dois usos:
- o grupo do Arthur joga pelo celular;
- um recrutador abre o link, entra na demonstração e vê a mesa ao vivo em menos de 2 minutos, contando o tempo de o servidor acordar.

**Anti-objetivos:**
- qualquer custo mensal;
- segredo no repositório;
- um visitante estragar os dados de outro;
- a tela ficar muda enquanto a API acorda.

## 2. Escolhas do Arthur (2026-11-17)
| Tema | Escolha |
|---|---|
| Entrada do recrutador | O botão **"Ver demonstração"** cria uma conta temporária só daquele visitante, já com uma campanha e fichas de exemplo |
| Mostrar o ao vivo | Um **convite para uma segunda aba** (ou para o celular), que cria uma segunda conta temporária, de jogador, na mesma mesa |
| Sono da API | **Aceitar o sono**, com o aviso "acordando o servidor"; nada de manter a API acordada |
| Publicação | Um **job no CI depois dos testes**, só na `main`, com o *deploy hook* do Render e o `wrangler pages deploy` |
| Hospedagem | **Caminho A:** Cloudflare Pages + Render (Docker) + Neon, como na spec da fase 2 |

**Fora do escopo:**
- domínio próprio;
- mais de uma instância da API;
- manter a API acordada;
- e-mail;
- monitoramento pago;
- CDS/AOT da JVM (só entra se o arranque passar de 2 min, ver §8).

## 3. Abordagens consideradas
- **A (escolhida):** Pages + Render + Neon. Foi o que a fase 2 aprovou, e a pesquisa nova confirma os três planos.
- **B:** front no Workers Static Assets, que é o que a Cloudflare recomenda para projetos novos. Num site estático, não traz ganho sobre o Pages, que continua ativo e sem aviso de descontinuação.
- **C:** API no Cloud Run ou no Koyeb. O Cloud Run exige faturamento com cartão, e o Render não mudou desde 2026-11-04.

## 4. Serviços e limites (pesquisa de 2026-11-17, páginas oficiais)
| Serviço | Plano gratuito | O que importa para o J1 |
|---|---|---|
| Render (render.com/docs/free) | 512 MB, 0,1 CPU, 750 h/mês por workspace; dorme depois de 15 min sem tráfego e acorda em ~1 min; Docker, WebSocket e *health check* | O Spring Boot precisa caber em 512 MB e subir com 0,1 CPU. Sem cartão, os serviços são suspensos até o fim do mês se a banda acabar. As portas 25/465/587 ficam bloqueadas (sem e-mail, que está fora do escopo) |
| Neon (neon.com, FAQ de 2026-11-03) | 1 GB por projeto, 100 CU-h/mês, dorme depois de 5 min | Com o retrato no banco (ADR 0014) e as contas demo apagadas em 24 h, 1 GB sobra |
| Cloudflare Pages (developers.cloudflare.com/pages/platform/limits) | 500 builds/mês, 20 mil arquivos, 25 MiB por arquivo, `_headers` com até 100 regras | A CSP vai no `_headers`, e as rotas do Angular caem no `index.html` |

**A conferir na criação das contas (é o Arthur quem cria):** se algum dos três pedir cartão, ele decide antes de seguir.

**Região:**
- Render em Virginia (us-east) e Neon na AWS us-east-1, juntos para a consulta não atravessar o país;
- do Brasil, são ~120 ms até o leste dos EUA.

## 5. API
**5.1 Dockerfile** (na pasta da API):
- *multi-stage*: `eclipse-temurin:21-jdk` roda `./mvnw -B package -DskipTests` (o CI já testou), e `eclipse-temurin:21-jre` roda o jar;
- usuário sem privilégio e `EXPOSE 8080`;
- `JAVA_TOOL_OPTIONS` vem do Render, para ajustar sem rebuild: começa em `-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k`;
- um `.dockerignore` deixa de fora o `target/`, o `bin/` e o `.env`.

**5.2 Configuração** (só variáveis de ambiente no Render, nada no repositório):
- `DB_URL` (o endpoint **direto** do Neon, não o *pooler*, porque o Flyway precisa de sessão, com `sslmode=require`), `DB_USERNAME`, `DB_PASSWORD`;
- `JWT_SEGREDO` (novo, gerado só para a produção);
- `APP_CORS_ORIGENS=https://<projeto>.pages.dev`;
- `SPRING_PROFILES_ACTIVE=demo`;
- `LOGGING_LEVEL_ORG_HIBERNATE_SQL=info`, para não registrar todo SQL;
- `spring.datasource.hikari.maximum-pool-size=5` (o padrão é 10);
- `server.forward-headers-strategy=native`, para o IP do visitante vir do `X-Forwarded-For` do Render;
- o Swagger continua público, porque ajuda o recrutador.

**5.3 Saúde:**
- o `spring-boot-starter-actuator` expõe só o `health`, com as *probes*;
- o Render e o front chamam `/actuator/health/liveness`, que não consulta o banco (assim não acorda o Neon à toa);
- esse caminho fica no `permitAll`.

**5.4 Perfil `demo`:**
- `POST /auth/demo` só existe com o perfil ligado; sem ele, responde 404. O corpo é opcional: `{ "codigoConvite": "..." }`.
- **Sem convite**, o endpoint cria:
  - um usuário "Visitante NNNN", com `demo = true`, e-mail interno inválido para login, sem senha e sem Google;
  - a campanha "Mesa de demonstração", com o visitante de mestre;
  - uma ficha de Ordem Paranormal e uma de D&D 2024, as duas preenchidas.

  A resposta são os tokens, como no login, mais o id da campanha.
- **Com convite**, ele só aceita o código de uma campanha cujo mestre seja demo; senão, 404. Ele cria o jogador demo, já membro da mesa, com uma ficha própria.
- **Migração V14:** `usuario.demo boolean not null default false` (e `criado_em`, se ainda não existir).
- O `/me` e o `GET /convites/{codigo}` passam a dizer se a conta ou a campanha é demo.

**5.5 Limpeza** (fecha um risco do ADR 0022):
- a mesma tarefa apaga os usuários demo criados há mais de 24 h, com tudo o que é deles (fichas, retratos, listas, campanhas, membros, rolagens, refresh), e os refresh tokens vencidos;
- **a API dorme**, então um `@Scheduled` diário sozinho pode nunca rodar. A tarefa roda no `ApplicationReadyEvent` (cada vez que acorda) e a cada 6 h enquanto está acordada;
- os refresh já usados, mas ainda válidos, ficam até vencer, porque a detecção de reuso depende deles.

**5.6 Limite de tentativas** (fecha o outro risco do ADR 0022):
- um filtro em memória, com janela fixa por IP e por rota, cobre `POST /auth/login`, `/auth/registro`, `/auth/google` e `/auth/demo`;
- os limites são 10 pedidos por minuto e, no `/auth/demo`, 5 a cada 10 min;
- acima do limite, responde **429** com `Retry-After`, no formato de erro do projeto (ADR 0007);
- o mapa é limpo de tempos em tempos;
- a memória basta porque há uma instância só (registrar a troca por Redis/Bucket4j se escalar);
- `/auth/refresh` fica de fora, porque a rotação já o protege.

## 6. Front
- **`environment.prod.ts`:** `apiUrl` com a URL do Render e `demoAtiva: true`. No dev, `demoAtiva` também é `true`; sem o perfil, a API responde 404 e a tela mostra o erro.
- **Entrar:** um botão "Ver demonstração", abaixo do Google, chama `/auth/demo` e abre a campanha demo.
- **A sessão demo fica no `sessionStorage`, e não no `localStorage`.** Uma sessão por aba é o que permite abrir o mestre e o jogador no mesmo navegador. Hoje o refresh fica no `localStorage`, compartilhado entre as abas: a segunda conta trocaria a primeira na renovação. Fechar a aba encerra a demo.
- **Campanha demo:** quando o usuário é demo, um cartão diz "Abra este convite em outra aba ou no celular para entrar como jogador", com o botão de copiar o link.
- **Convite:** sem sessão e com campanha demo, aparece "Entrar como jogador de demonstração" (`/auth/demo` com o código).
- **Aviso "acordando o servidor":**
  - ao abrir o app, o front chama `/actuator/health/liveness`;
  - sem resposta em 3 s, aparece uma faixa fixa: "Acordando o servidor gratuito… pode levar até 2 minutos (Ns)";
  - o front tenta de novo a cada 5 s, por até 3 min, e a faixa some quando a API responde;
  - enquanto isso, Entrar, Google e Ver demonstração ficam "aguardando o servidor".
- **`public/_headers`** (vai para o `dist` e é aplicado pelo Pages):
  ```
  /*
    Content-Security-Policy: default-src 'self'; script-src 'self' https://accounts.google.com/gsi/client; frame-src https://accounts.google.com/gsi/; connect-src 'self' https://<api>.onrender.com wss://<api>.onrender.com https://accounts.google.com/gsi/; style-src 'self' 'unsafe-inline' https://accounts.google.com/gsi/style; img-src 'self' data: blob:; font-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'
    X-Content-Type-Options: nosniff
    Referrer-Policy: strict-origin-when-cross-origin
    Permissions-Policy: camera=(), microphone=(), geolocation=()
  ```
  - O `script-src` não tem `'unsafe-inline'`: é isso que protege o refresh contra XSS (ADR 0023).
  - O `style-src` aceita `'unsafe-inline'` porque o Angular e o Material injetam estilos, e um site estático não tem *nonce*; estilo injetado é bem menos perigoso que script.
  - Se a inlinização de CSS crítico do Angular usar `onload` em linha (violação de `script-src`), desligar `optimization.styles.inlineCritical`.
  - O `blob:` cobre o retrato (`createObjectURL`), e o `data:`, as texturas do dado 3D.
  - Conferir no console, com `npx wrangler pages dev`, que não há violação.

## 7. Publicação
- **Job `deploy` no `ci.yml`:**
  - `needs: [backend, frontend, e2e]`;
  - roda só em `push` na `main` (depois do merge), com `environment: producao`;
  - `permissions: contents: read`;
  - passos:
    1. `curl -fsS -X POST "$RENDER_DEPLOY_HOOK"`: o Render puxa a `main` e builda o Dockerfile, com o auto-deploy desligado;
    2. `npx ng build`;
    3. `npx wrangler@<versão fixa> pages deploy <dist>/browser --project-name=<projeto> --branch=main`.
- **Segredos só no environment `producao` do GitHub:** `RENDER_DEPLOY_HOOK`, `CLOUDFLARE_API_TOKEN` (só Pages:Edit) e `CLOUDFLARE_ACCOUNT_ID`.
- **Primeiro deploy, antes do merge:** o Arthur aponta o serviço do Render para a branch do PR, e a publicação manual do front sai da branch. Assim, o link é conferido antes de mesclar. Depois do merge, o Render volta para a `main`.

## 8. Tarefas do Arthur (contas e painéis; cada uma passa por ele)
1. Criar as contas: Render, Neon e Cloudflare (parar se pedirem cartão).
2. No Neon: um projeto em us-east-1 e o banco `fichas`, com a *connection string* direta.
3. No Render: um Web Service Docker, gratuito, em Virginia, com as variáveis da §5.2, o *health check* `/actuator/health/liveness`, o auto-deploy desligado e o *deploy hook* copiado.
4. Na Cloudflare: um projeto Pages de *direct upload* e um token de API com Pages:Edit.
5. No GitHub: o environment `producao` com os três segredos.
6. No Google Cloud:
   - a origem `https://<projeto>.pages.dev` em "Origens JavaScript autorizadas";
   - publicar o app (**Em produção**).

   Os escopos são só `openid email profile`, então não há verificação do Google.
7. Medir o arranque frio no primeiro deploy. Se a página do Render + a JVM passarem de 2 min, testar CDS antes de qualquer outra mudança.

## 9. Testes
- **API (Testcontainers):**
  - `/auth/demo` com e sem convite, e o 404 sem o perfil;
  - convite de campanha real recusado;
  - a limpeza apaga a conta demo velha com tudo o que é dela e mantém a nova e as reais;
  - os refresh vencidos são apagados;
  - o 429 com `Retry-After` e o reinício da janela;
  - `/actuator/health/liveness` público.
- **Front (Vitest):**
  - o botão de demo;
  - a sessão demo no `sessionStorage`;
  - o cartão do convite demo;
  - a faixa de acordar (aparece aos 3 s, some quando a API responde e desiste em 3 min).
- **Local:**
  - `docker build` + `docker run` contra o Postgres local com o perfil demo;
  - `wrangler pages dev` com a CSP, o botão do Google e o dado 3D sem violação no console.
- **Produção:** do link público, conferir a demo em duas abas (mestre e jogador), a rolagem ao vivo, o login com Google e a faixa de acordar com a API dormindo.

## 10. Pronto quando
- O link público funciona do login à rolagem ao vivo: demo em duas abas e Google com uma conta fora da lista de teste.
- O arranque frio fica em menos de 2 min.
- Nenhum segredo no repositório (`git grep` e o histórico do PR).
- CI verde com o job `deploy` publicando depois do merge.
- O README tem o link e a seção de deploy.
- O 2.5 aparece como "Concluído (PR #21)".
- ADR 0028 (hospedagem e publicação) e ADR 0029 (conta de demonstração e limite de tentativas).

## 11. Riscos
| Risco | Mitigação |
|---|---|
| A JVM com 0,1 CPU sobe devagar | Ajuste da JVM por variável de ambiente; medir; CDS como passo seguinte |
| 512 MB não bastam (OOM) | `MaxRAMPercentage`, SerialGC, pool de 5; o Render mostra o uso de memória |
| `X-Forwarded-For` falsificado burla o limite | O Tomcat confia só no proxy interno do Render; conferir o IP no log no primeiro deploy |
| Plano gratuito muda ou passa a pedir cartão | Os limites desta spec têm data; qualquer custo passa pelo Arthur |
| Visitante enche o banco | Limite no `/auth/demo` e limpeza em 24 h; o Neon avisa perto de 1 GB |
| A CSP quebra o botão do Google ou o dado | `wrangler pages dev` antes do merge, de olho no console |
