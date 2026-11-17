# 0027. Login com Google pelo ID token e ligação de contas

Status: aceita · Incremento 2.5 · 2026-11-17

## Contexto
Desde o 2.2 ([ADR 0022](0022-autenticacao-propria-com-jwt-e-refresh-rotacionado.md)), cada pessoa tem uma conta com e-mail e senha, e a senha segue a NIST (pelo menos 15 caracteres). Para tirar esse atrito, o 2.5 acrescenta "Continuar com o Google". Há três restrições:
- a API é stateless e emite os próprios tokens;
- no deploy (2.6), o front e a API ficam em domínios diferentes ([ADR 0023](0023-tokens-no-navegador.md));
- o cadastro por senha **não confirma o e-mail**.

## Decisão
- **O front usa o botão oficial do Google Identity Services** e recebe um *ID token* (um JWT assinado pelo Google). Ele o envia para `POST /auth/google`.
- **A API confere o token** com um `NimbusJwtDecoder` próprio. O algoritmo é RS256, as chaves vêm do JWKS do Google (com cache e troca pelo `kid`), e os validadores são:
  - `iss` ∈ {`https://accounts.google.com`, `accounts.google.com`};
  - `aud` = o nosso Client ID;
  - `exp`, pelo `Clock` da aplicação.
  Depois a API devolve o **mesmo par de tokens do login por senha**, numa família nova.
- **Esse decoder não é um bean `JwtDecoder`.** Ele fica privado no `VerificadorDoGoogle`, e o *resource server* continua com um só decoder, o HS256 dos nossos tokens.
- **Regras da conta, nesta ordem:**
  1. e-mail não verificado no Google → 401, e nada muda;
  2. o `google_sub` já existe → entra. O `sub` é a identidade, e o e-mail pode mudar;
  3. o e-mail já tem conta → **liga as duas, apaga a senha e revoga todas as famílias de refresh**. Se a conta já estava ligada a outro `sub`, a resposta é 401;
  4. senão → cria uma conta sem senha.
- **Por que apagar a senha na ligação (*pre-account takeover*):** como o cadastro não confirma o e-mail, alguém pode criar uma conta com o e-mail de outra pessoa e uma senha que ele conhece. Se a ligação mantivesse a senha, essa pessoa continuaria entrando depois que a dona do e-mail usasse o Google. Quem provou ao Google que é dona do e-mail vence. A conta ligada passa a entrar só pelo Google (escolha do Arthur: "definir senha" fica fora do escopo).
- **O Client ID fica no repositório.** Ele é público: o navegador o mostra a qualquer um. O client secret não é usado nesse fluxo.
- **Os testes não usam a rede.** Um JWKS em memória (`ImmutableJWKSet`) com uma chave RSA gerada no teste substitui o bean `chavesDoGoogle`, então o caminho do código (seletor de chave, RS256 e validadores) é o mesmo da produção.

## Alternativas consideradas
- **O `oauth2-client` do Spring (fluxo *authorization code* no servidor):** exige client secret, URI de redirecionamento e sessão com cookie, o que não combina com a API stateless em outro domínio.
- **O `GoogleIdTokenVerifier` (biblioteca `google-api-client`):** faz o mesmo que o Nimbus, que já vem com o Spring Security, e traz mais uma dependência.
- **Ligar e manter a senha:** é o mais simples, mas deixa aberto o *pre-account takeover*.
- **Não ligar sozinho (409, "entre com a senha"):** é seguro, mas as contas antigas nunca usariam o Google.
- **Aceitar um e-mail não verificado sem ligar:** cria casos a mais, e um e-mail não confirmado ocuparia o lugar do dono real.
- **Um `nonce` contra a reapresentação do ID token:** exigiria guardar estado no servidor ou num cookie. Fica fora do escopo (veja as consequências).

## Consequências
- Entrar pelo Google leva dois cliques, e a sessão continua a do 2.2 (renovação, "Sair" e duas abas).
- **Pior:** quem tinha senha e liga a conta ao Google perde o login por senha, e não há como definir uma de novo (ainda).
- **Pior:** um ID token interceptado pode ser reapresentado à API enquanto vale (cerca de 1 h). As defesas são o HTTPS no deploy e a validade curta.
- **Pior:** se o JWKS do Google estiver fora do ar e sem cache, o login pelo Google falha com 401. O login por senha continua funcionando.
- **Até o 2.6**, o público do cliente OAuth está em Teste, então só os usuários de teste do Arthur entram pelo Google. O 2.6 acrescenta a origem do Pages, o status Em produção e a CSP liberando `accounts.google.com`.
