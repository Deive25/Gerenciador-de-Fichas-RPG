# 0022. Autenticação própria com JWT de acesso e refresh rotacionado

Status: aceita · Incremento 2.2 · 2026-11-16

## Contexto
Até o 2.1i, a API não tinha login: qualquer pessoa que a alcançasse lia, editava e apagava qualquer ficha. O 2.2 dá uma conta a cada pessoa (e-mail e senha) e faz cada uma ver, editar e rolar só as próprias fichas. O deploy (2.6) vai pôr o front e a API em domínios diferentes (Cloudflare Pages e Render), e o login com Google (2.5) precisa caber depois sem refazer o que existe.

## Decisão
- **A própria API emite e valida os tokens**, com o *resource server* do Spring Security:
  - **acesso:** JWT HS256 de 15 min, com `iss` `fichas-rpg`, `sub` = id do usuário e `email`. A chave vem de `JWT_SEGREDO`, e a API não sobe sem ela ou com menos de 32 bytes;
  - **refresh:** 256 bits aleatórios, válido por 30 dias a partir da última renovação. O banco guarda só o SHA-256 dele.
- **HS256, e não RS256:** só a própria API emite e confere o token. Uma chave assimétrica só faria sentido se outro serviço validasse.
- **SHA-256 no refresh, e não BCrypt:** o refresh é aleatório e longo, então ninguém o adivinha, e a API precisa achá-lo pelo hash. A senha é fraca por natureza e precisa de um hash lento com sal (BCrypt).
- **Rotação por família:**
  - cada login é uma família;
  - cada renovação troca o refresh e marca o antigo como revogado;
  - **reapresentar um refresh já usado revoga a família inteira** (detecção de roubo), e as outras famílias do mesmo usuário (o celular, o PC) continuam vivas;
  - a renovação trava a linha (`SELECT ... FOR UPDATE`), para duas renovações simultâneas não gerarem dois filhos;
  - **o serviço devolve um resultado (`Renovado` ou `Recusado`) em vez de lançar uma exceção dentro da transação.** A exceção desfaria a revogação da família no rollback. O controller responde 401 depois do commit, e um teste confere que a revogação ficou gravada.
- **Ficha de outro usuário dá 404, e não 403.** Para quem não é o dono, a ficha não existe, e a API não revela quais ids estão em uso. Um componente único (`FichasDoUsuario`) busca por id **e** dono, e todos os serviços passam por ele. O 403 aparece no 2.3, para o membro da mesa sem permissão.
- **Login sem revelar contas:** senha errada e e-mail inexistente dão o mesmo 401. Com o e-mail inexistente, a senha é comparada com um hash fixo, para o tempo de resposta não denunciar quais e-mails têm conta.
- **Cadastro com e-mail repetido dá 409 em `errors.email`.** Isso revela que o e-mail tem conta, e foi aceito pela usabilidade: a pessoa precisa saber que deve entrar em vez de se cadastrar.
- **Stateless e sem CSRF:** o token vai no cabeçalho `Authorization`, que o navegador não envia sozinho. O CORS fica num lugar só (`app.cors.origens`), e não em cada controller.

## Alternativas consideradas
- **Sessão no servidor com cookie:** é a mais simples num domínio só, mas no deploy o front e a API ficam em sites diferentes, e um cookie de terceiro é bloqueado pelo Safari (veja o [ADR 0023](0023-tokens-no-navegador.md)).
- **Provedor externo (Keycloak, Auth0):** resolve muita coisa pronta, mas é mais um serviço para hospedar de graça ou um plano pago, para um app de um grupo de RPG. Eu também perderia o que este incremento quer mostrar: entender o fluxo.
- **Só um JWT longo, sem refresh:** é simples, mas um token roubado valeria por dias, sem como revogar.

## Consequências
- Cada pessoa vê só as próprias fichas. As fichas de antes do login ficam com o usuário de sistema `legado`, que não consegue entrar, e um SQL documentado no README as passa para uma conta.
- **Pior:** depois do logout, o token de acesso ainda vale até vencer (no máximo 15 min), porque o JWT é stateless. O refresh, este sim, para na hora.
- **Pior:** a chave HS256 é um segredo compartilhado. Se vazar, qualquer um forja tokens; ela fica fora do repositório, e o `.env.example` traz o campo vazio de propósito.
- **Pior:** não há limite de tentativas de login. Isso fica registrado como risco para o deploy (2.6).
- Os refresh vencidos nunca são apagados. Com poucos usuários, são alguns milhares de linhas por ano; a limpeza será reavaliada no 2.6.
