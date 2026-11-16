# 0023. Onde o navegador guarda os tokens

Status: aceita · Incremento 2.2 · 2026-11-16

## Contexto
O login (2.2, [ADR 0022](0022-autenticacao-propria-com-jwt-e-refresh-rotacionado.md)) devolve um token de acesso de 15 min e um refresh de 30 dias. O front precisa guardá-los de um jeito que sobreviva a uma recarga e a uma aba nova, sem que um script de terceiro os leia com facilidade. No deploy (2.6), o site fica no Cloudflare Pages e a API no Render, em domínios diferentes.

## Decisão
- **O token de acesso fica só na memória** (`Sessao`). Recarregar a página o apaga.
- **O refresh fica no `localStorage`** (`fichas-rpg.refresh`), com toda leitura e escrita protegida por try/catch.
- **Ao recarregar, o guard das rotas renova antes de abrir a tela**, então a pessoa continua logada e na mesma página.
- **Um interceptor** põe o `Authorization: Bearer` nas chamadas à API, nunca nas de `/auth/*`, porque lá o 401 significa "senha errada". Quando recebe 401, ele renova uma vez e repete a chamada, sem laço.
- **A renovação é única:**
  - **na mesma aba,** várias chamadas que recebem 401 juntas esperam a mesma promessa;
  - **entre abas,** `navigator.locks` põe as renovações em fila, e cada uma relê o refresh do `localStorage` dentro da trava.
  Sem isso, duas abas reapresentariam o mesmo refresh, e a detecção de roubo do servidor derrubaria a sessão.
- **Só um 401 do `/auth/refresh` apaga a sessão.** Uma queda de rede ou um 5xx mantém o refresh, para a pessoa não perder o login porque a API está fora do ar ou acordando.
- **O "Sair" faz uma navegação completa para `/entrar`.** Isso zera a lista e os retratos em memória, o que importa quando outra pessoa usa o mesmo aparelho.
- **O `?volta=` só aceita um caminho interno** (começa com `/` e não com `//`), para não virar um redirecionamento aberto.

## Alternativas consideradas
- **Cookie *httpOnly* com o refresh:** um script não leria o refresh. Mas, com a API em outro site, o cookie seria de terceiro e o Safari o bloqueia. Para funcionar, seria preciso um proxy no mesmo site (por exemplo, Pages Functions) ou um domínio próprio pago, mais a proteção contra CSRF. Fica como o caminho se o projeto ganhar um domínio próprio.
- **Tudo só na memória:** seria a opção mais segura, mas cada recarga ou aba nova pediria login de novo, o que é ruim no celular, no meio da mesa.
- **Tudo no `localStorage`, inclusive o token de acesso:** não traz ganho, e um XSS levaria os dois tokens.

## Consequências
- Recarregar, abrir outra aba e voltar no dia seguinte mantêm o login; conferido no navegador, inclusive com duas abas recarregando ao mesmo tempo.
- **Pior:** um XSS consegue ler o refresh. As defesas são o Angular escapando todo texto (nenhum `innerHTML` dinâmico no projeto), a rotação com detecção de reuso e, no deploy (2.6), uma CSP rígida.
- **Pior:** sair numa aba não desloga as outras na hora. Elas caem no login na próxima renovação.
