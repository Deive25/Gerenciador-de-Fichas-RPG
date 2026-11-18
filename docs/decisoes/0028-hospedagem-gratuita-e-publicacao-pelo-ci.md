# 0028. Hospedagem gratuita e publicação pelo CI

Status: aceita · Incremento 2.6 · 2026-11-17

## Contexto
O J1 precisa de um link público e gratuito, com dois usos: o grupo do Arthur joga pelo celular, e um recrutador abre o link e vê a mesa ao vivo em menos de 2 minutos. Não pode haver custo mensal nem segredo no repositório. O front e a API ficam em domínios diferentes, o que já pesou nos ADRs [0022](0022-autenticacao-propria-com-jwt-e-refresh-rotacionado.md) e [0023](0023-tokens-no-navegador.md).

## Decisão
- **Três serviços, todos no plano gratuito:**
  - **Front:** Cloudflare Pages (`https://fichas-rpg.pages.dev`), com os arquivos estáticos do Angular.
  - **API:** Render, como Web Service Docker em Virginia (512 MB, 0,1 CPU). Dorme depois de 15 min sem tráfego e acorda em cerca de 1 min.
  - **Banco:** Neon (PostgreSQL, AWS us-east-1), na mesma região da API. A API usa a conexão **direta**, e não a do *pooler*, porque o Flyway precisa de uma sessão normal para rodar as migrações.
- **Só variáveis de ambiente no Render, nada no repositório:** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SEGREDO`, `APP_CORS_ORIGENS`, `SPRING_PROFILES_ACTIVE=demo` e `LOGGING_LEVEL_ORG_HIBERNATE_SQL=info` (para não registrar todo SQL). Opcionalmente, `JAVA_TOOL_OPTIONS`, para ajustar a JVM sem refazer a imagem. O `.env.example` lista os nomes, sem valores.
- **Dockerfile em dois estágios:** o JDK compila o jar (`-DskipTests`, porque o CI já testou), e a imagem final leva só o JRE, com usuário sem privilégio.
- **CDS (*Class Data Sharing*) do Java 21 no `docker build`.** Um *training run* sobe a aplicação até o fim do carregamento (`-Dspring.context.exit=onRefresh`, sem banco) e grava o arquivo de classes já lidas. No arranque de verdade, a JVM reaproveita esse arquivo. Medido no Docker local com `--cpus 0.1` e 512 MB: **128 s sem CDS, 63 s com CDS**, com cerca de 220 MiB de memória. A imagem cresce cerca de 140 MB. A medida no Render ainda não foi feita (fica para o primeiro arranque a frio).
- **A API não é mantida acordada.** Em vez de um ping, o front consulta `/actuator/health/liveness` ao abrir e mostra a faixa "Acordando o servidor gratuito…" enquanto não houver resposta. Esse é o único caminho de saúde público; ele não consulta o banco, então também não acorda o Neon, e o resto do actuator não é exposto.
- **A CSP vai no `_headers` do Pages.** Em resumo: `script-src 'self'` mais o script do Google, **sem `'unsafe-inline'`**; `connect-src` com a API (HTTPS e WSS) e o Google; `worker-src 'self' blob:`; `frame-ancestors 'none'`. O que protege o refresh no `localStorage` contra XSS (ADR 0023) é justamente o `script-src` sem `'unsafe-inline'`. O `style-src` aceita `'unsafe-inline'`, porque o Angular e o Material injetam estilos e um site estático não tem *nonce*. Três ajustes vieram da prática:
  - o Angular 22 injetava um `<script>` em linha para o CSS crítico, que a CSP bloqueia; a saída foi desligar `optimization.styles.inlineCritical`;
  - o worker de física do dado 3D nasce de um `blob:`, daí o `worker-src 'self' blob:`;
  - **Early Hints:** o Pages gera sozinho um header `Link` a partir dos `modulepreload` do `index.html`, com caminhos relativos. Numa rota como `/fichas/5`, o navegador pedia `/fichas/chunk-*.js` e recebia HTML. A linha `! Link` no `_headers` remove esse header, e valem só os links do HTML, com base `/`.
- **Publicação pelo CI, não pelas integrações nativas.** O `ci.yml` ganhou o job `deploy`, que só roda em *push* na `main`, depois de `backend`, `frontend` e `e2e`, no environment `producao`. Ele compila o front, chama o *deploy hook* do Render (o auto-deploy do Render fica desligado) e roda `wrangler@4.156.2 pages deploy`. Os três segredos (`RENDER_DEPLOY_HOOK`, `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID`) existem só nesse environment. O primeiro deploy foi manual (Render apontando para a branch do PR e o front pelo wrangler), para conferir o link antes do merge.

## Alternativas consideradas
- **Front no Workers Static Assets** (o que a Cloudflare recomenda para projetos novos): num site estático, não traz ganho sobre o Pages, que continua ativo e sem aviso de descontinuação.
- **API no Cloud Run ou no Koyeb:** o Cloud Run exige faturamento com cartão. O Render tem o que o projeto usa (Docker, WebSocket, *health check*) e não mudou desde a spec da fase 2.
- **Integrações nativas do Render e do Pages com o GitHub (deploy a cada push):** são mais simples, mas publicariam mesmo com o CI vermelho, porque não esperam os testes. O job `deploy` garante a ordem: testes, depois publicação.
- **Manter a API acordada com um agendamento (cron/ping):** elimina o sono, mas gasta as 750 h mensais do plano, depende de um serviço de fora e foge do objetivo de custo zero sem manutenção. Aceitei o sono e deixei o aviso na tela.

## Consequências
- O Arthur tem um link para mostrar, e cada merge na `main` publica sozinho.
- **Pior:** a API dorme. Depois de 15 min parada, o primeiro acesso espera cerca de 1 min do Render mais o arranque da JVM (a 0,1 CPU, 63 s no teste local). A faixa na tela explica a espera, mas ela existe.
- **Pior:** a imagem Docker ficou cerca de 140 MB maior por causa do CDS, e o `docker build` demora mais (ele sobe a aplicação uma vez).
- **Pior:** há três segredos no GitHub. Ficam num environment, e o token da Cloudflare só edita o Pages, mas quem controla o repositório controla a publicação.
- **Pior:** tudo depende de planos gratuitos que mudam. Os limites da spec têm data (2026-11-17), e qualquer custo novo passa pelo Arthur antes.
- **Pior:** a CSP é mais um lugar a manter. Uma biblioteca nova que use `eval`, script em linha ou outro tipo de worker pode ser bloqueada, e o erro só aparece no console do navegador.
- O broker do WebSocket (ADR 0026) continua em memória, o que serve a uma instância só, e o plano gratuito tem uma.
